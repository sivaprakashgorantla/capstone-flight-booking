package com.flight.flight_service.config;

import com.flight.flight_service.model.Flight;
import com.flight.flight_service.model.FlightStatus;
import com.flight.flight_service.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds realistic flight data on startup.
 *
 * Coverage : 53 route-pairs (106 one-way routes) across 12 major Indian cities
 * Airlines : Air India · IndiGo · SpiceJet · Vistara · AirAsia India · Akasa Air
 * Dates    : today → today + 29 (always current, no hardcoded dates)
 * Flights  : 4–6 per route per day (all airlines on trunk routes) ≈ 15 000 total
 * Pricing  : base ± weekend premium ± slot premium ± demand curve
 * Status   : SCHEDULED (default) · DELAYED (occasional) · CANCELLED (rare)
 *
 * Idempotent — skips seeding when table is non-empty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final FlightRepository flightRepository;

    // ─── Domain records ────────────────────────────────────────────────────────

    private record Airport(String city, String code) {}
    private record Airline(String code, String name, int seats) {}

    /** Variable number of airlines — one flight per airline per day */
    private record Route(Airport from, Airport to, int durationMins, int basePrice, Airline[] airlines) {}

    // ─── Airports ─────────────────────────────────────────────────────────────
    private static final Airport DEL = new Airport("Delhi",     "DEL");
    private static final Airport BOM = new Airport("Mumbai",    "BOM");
    private static final Airport BLR = new Airport("Bengaluru", "BLR");
    private static final Airport MAA = new Airport("Chennai",   "MAA");
    private static final Airport HYD = new Airport("Hyderabad", "HYD");
    private static final Airport CCU = new Airport("Kolkata",   "CCU");
    private static final Airport GOI = new Airport("Goa",       "GOI");
    private static final Airport COK = new Airport("Kochi",     "COK");
    private static final Airport AMD = new Airport("Ahmedabad", "AMD");
    private static final Airport JAI = new Airport("Jaipur",    "JAI");
    private static final Airport PNQ = new Airport("Pune",      "PNQ");
    private static final Airport LKO = new Airport("Lucknow",   "LKO");

    // ─── Airlines ─────────────────────────────────────────────────────────────
    private static final Airline AI  = new Airline("AI",  "Air India",     180);
    private static final Airline _6E = new Airline("6E",  "IndiGo",        186);
    private static final Airline SG  = new Airline("SG",  "SpiceJet",      145);
    private static final Airline UK  = new Airline("UK",  "Vistara",       158);
    private static final Airline I5  = new Airline("I5",  "AirAsia India", 180);
    private static final Airline QP  = new Airline("QP",  "Akasa Air",     174);

    /**
     * 8 evenly-spread departure slots (hour, minute).
     * Routes with N airlines are assigned the first N slots.
     * Variant offset (route_index + day) % 3 staggers the window slightly.
     */
    private static final int[][] SLOTS = {
            {5, 45}, {7, 30}, {9, 20}, {11, 15},
            {13, 30}, {15, 45}, {17, 30}, {19, 55}
    };

    // Slot premium multipliers indexed by slot (0-7)
    private static final double[] SLOT_PREMIUM = {
            1.06, 1.10, 1.04, 1.00, 1.02, 1.08, 1.12, 1.05
    };

    // ─── Entry point ──────────────────────────────────────────────────────────

    @Override
    public void run(String... args) {
        if (flightRepository.count() > 0) {
            log.info("DataSeeder: flights already present — skipping.");
            return;
        }

        LocalDate     today  = LocalDate.now();
        List<Route>   routes = buildRoutes();
        List<Flight>  batch  = new ArrayList<>(16_000);
        int serial = 1000;

        for (int day = 0; day < 30; day++) {
            LocalDate date      = today.plusDays(day);
            boolean   isWeekend = date.getDayOfWeek() == DayOfWeek.FRIDAY
                                || date.getDayOfWeek() == DayOfWeek.SATURDAY
                                || date.getDayOfWeek() == DayOfWeek.SUNDAY;

            for (int ri = 0; ri < routes.size(); ri++) {
                Route route   = routes.get(ri);
                int   offset  = (ri + day) % 3;  // slight stagger per route+day

                for (int si = 0; si < route.airlines().length; si++) {
                    Airline airline  = route.airlines()[si];
                    int     slotIdx  = (si + offset) % SLOTS.length;
                    int[]   slot     = SLOTS[slotIdx];

                    LocalDateTime dep = date.atTime(slot[0], slot[1]);
                    LocalDateTime arr = dep.plusMinutes(route.durationMins());

                    // Pricing: base × slot premium × weekend × demand curve
                    double price = route.basePrice()
                            * SLOT_PREMIUM[slotIdx]
                            * (isWeekend ? 1.15 : 1.0)
                            * (1.0 + (day % 7) * 0.012);
                    int rounded = (int) Math.round(price / 50.0) * 50;

                    // Status distribution
                    FlightStatus status = FlightStatus.SCHEDULED;
                    int hash = ri * 31 + day * 17 + si;
                    if (slotIdx >= 5 && hash % 9 == 0) {
                        status = FlightStatus.DELAYED;
                    } else if (hash % 55 == 0) {
                        status = FlightStatus.CANCELLED;
                    }

                    batch.add(Flight.builder()
                            .flightNumber(airline.code() + serial++)
                            .airline(airline.name())
                            .departureCity(route.from().city())
                            .departureAirport(route.from().code())
                            .destinationCity(route.to().city())
                            .destinationAirport(route.to().code())
                            .departureTime(dep)
                            .arrivalTime(arr)
                            .price(BigDecimal.valueOf(rounded))
                            .totalSeats(airline.seats())
                            .availableSeats(airline.seats())
                            .status(status)
                            .build());
                }
            }
        }

        // Save in chunks of 500 for performance
        int chunk = 500;
        for (int i = 0; i < batch.size(); i += chunk) {
            flightRepository.saveAll(batch.subList(i, Math.min(i + chunk, batch.size())));
        }

        log.info("DataSeeder: ✅ seeded {} flights across {} one-way routes for {} days ({} → {})",
                batch.size(), routes.size(), 30, today, today.plusDays(29));
    }

    // ─── Route definitions ─────────────────────────────────────────────────────
    // Trunk routes  (DEL/BOM hubs) → all 6 airlines
    // Regional routes               → 4 airlines
    // Short-haul / thin routes      → 3 airlines

    private List<Route> buildRoutes() {
        List<Route> r = new ArrayList<>(110);

        // ══ DEL hub — trunk (all 6 airlines) ════════════════════════════════
        r.add(route(DEL, BOM, 135, 4500,  AI, _6E, UK, SG, I5, QP));
        r.add(route(BOM, DEL, 135, 4500,  AI, _6E, SG, UK, I5, QP));

        r.add(route(DEL, BLR, 165, 5500,  AI, _6E, UK, SG, I5, QP));
        r.add(route(BLR, DEL, 165, 5500,  AI, _6E, QP, SG, UK, I5));

        r.add(route(DEL, MAA, 155, 5200,  AI, _6E, I5, UK, SG, QP));
        r.add(route(MAA, DEL, 155, 5200,  AI, _6E, SG, I5, UK, QP));

        r.add(route(DEL, HYD, 130, 4800,  AI, _6E, UK, SG, I5, QP));
        r.add(route(HYD, DEL, 130, 4800,  AI, _6E, SG, QP, UK, I5));

        r.add(route(DEL, CCU, 145, 5000,  AI, _6E, QP, I5, SG, UK));
        r.add(route(CCU, DEL, 145, 5000,  AI, _6E, I5, SG, QP, UK));

        r.add(route(DEL, GOI, 155, 5200,  AI, _6E, SG, UK, I5, QP));
        r.add(route(GOI, DEL, 155, 5200,  AI, _6E, UK, SG, QP, I5));

        // DEL — 4-airline routes
        r.add(route(DEL, COK, 215, 6800,  AI, _6E, UK, I5));
        r.add(route(COK, DEL, 215, 6800,  AI, _6E, SG, QP));

        r.add(route(DEL, AMD,  90, 3200,  AI, _6E, QP, SG));
        r.add(route(AMD, DEL,  90, 3200,  AI, _6E, SG, QP));

        r.add(route(DEL, JAI,  55, 2500,  AI, _6E, SG, QP));
        r.add(route(JAI, DEL,  55, 2500,  AI, _6E, QP, SG));

        r.add(route(DEL, LKO,  70, 2800,  AI, _6E, QP, I5));
        r.add(route(LKO, DEL,  70, 2800,  AI, _6E, SG, QP));

        r.add(route(DEL, PNQ, 145, 4800,  AI, _6E, SG, I5));
        r.add(route(PNQ, DEL, 145, 4800,  AI, _6E, QP, UK));

        // ══ BOM hub — trunk (all 6 airlines) ════════════════════════════════
        r.add(route(BOM, BLR, 110, 4000,  AI, _6E, SG, UK, I5, QP));
        r.add(route(BLR, BOM, 110, 4000,  AI, _6E, UK, SG, QP, I5));

        r.add(route(BOM, MAA, 110, 4200,  AI, _6E, I5, SG, UK, QP));
        r.add(route(MAA, BOM, 110, 4200,  AI, _6E, SG, I5, QP, UK));

        r.add(route(BOM, HYD,  80, 3200,  AI, _6E, QP, SG, UK, I5));
        r.add(route(HYD, BOM,  80, 3200,  AI, _6E, SG, QP, I5, UK));

        r.add(route(BOM, CCU, 155, 5000,  AI, _6E, UK, SG, I5, QP));
        r.add(route(CCU, BOM, 155, 5000,  AI, _6E, SG, UK, QP, I5));

        // BOM — 4-airline routes
        r.add(route(BOM, GOI,  70, 2800,  AI, _6E, SG, QP));
        r.add(route(GOI, BOM,  70, 2800,  AI, _6E, QP, SG));

        r.add(route(BOM, COK, 120, 4500,  AI, _6E, SG, UK));
        r.add(route(COK, BOM, 120, 4500,  AI, _6E, UK, I5));

        r.add(route(BOM, AMD,  65, 2500,  AI, _6E, QP, SG));
        r.add(route(AMD, BOM,  65, 2500,  AI, _6E, SG, I5));

        r.add(route(BOM, JAI, 135, 4200,  AI, _6E, SG, QP));
        r.add(route(JAI, BOM, 135, 4200,  AI, _6E, QP, SG));

        r.add(route(BOM, PNQ,  40, 1800,  AI, _6E, SG, QP));
        r.add(route(PNQ, BOM,  40, 1800,  AI, _6E, QP, SG));

        // ══ South India trunk (all 6) ════════════════════════════════════════
        r.add(route(BLR, HYD,  70, 2800,  AI, _6E, SG, UK, I5, QP));
        r.add(route(HYD, BLR,  70, 2800,  AI, _6E, QP, SG, UK, I5));

        r.add(route(BLR, MAA,  60, 2500,  AI, _6E, UK, SG, I5, QP));
        r.add(route(MAA, BLR,  60, 2500,  AI, _6E, SG, UK, QP, I5));

        // South — 4-airline routes
        r.add(route(BLR, COK,  85, 3000,  AI, _6E, SG, QP));
        r.add(route(COK, BLR,  85, 3000,  AI, _6E, QP, I5));

        r.add(route(BLR, CCU, 155, 5200,  AI, _6E, UK, SG));
        r.add(route(CCU, BLR, 155, 5200,  AI, _6E, SG, QP));

        r.add(route(MAA, HYD,  75, 2800,  AI, _6E, SG, I5));
        r.add(route(HYD, MAA,  75, 2800,  AI, _6E, QP, SG));

        r.add(route(MAA, COK,  60, 2500,  AI, _6E, UK, SG));
        r.add(route(COK, MAA,  60, 2500,  AI, _6E, SG, I5));

        r.add(route(MAA, CCU, 155, 5000,  AI, _6E, SG, QP));
        r.add(route(CCU, MAA, 155, 5000,  AI, _6E, QP, I5));

        r.add(route(HYD, CCU, 120, 4800,  AI, _6E, SG, I5));
        r.add(route(CCU, HYD, 120, 4800,  AI, _6E, QP, SG));

        r.add(route(HYD, COK, 100, 3800,  AI, _6E, SG, UK));
        r.add(route(COK, HYD, 100, 3800,  AI, _6E, QP, I5));

        // ══ NEW: HYD spoke routes (3 airlines) ══════════════════════════════
        r.add(route(HYD, JAI, 175, 5600,  AI, _6E, SG));
        r.add(route(JAI, HYD, 175, 5600,  AI, _6E, QP));

        r.add(route(HYD, AMD, 110, 3800,  AI, _6E, I5));
        r.add(route(AMD, HYD, 110, 3800,  AI, _6E, SG));

        r.add(route(HYD, GOI, 110, 3800,  AI, _6E, QP));
        r.add(route(GOI, HYD, 110, 3800,  AI, _6E, SG));

        r.add(route(HYD, LKO, 155, 5200,  AI, _6E, I5));
        r.add(route(LKO, HYD, 155, 5200,  AI, _6E, QP));

        r.add(route(HYD, PNQ,  90, 3200,  AI, _6E, SG));
        r.add(route(PNQ, HYD,  90, 3200,  AI, _6E, I5));

        // ══ NEW: AMD spoke routes ════════════════════════════════════════════
        r.add(route(AMD, BLR, 140, 4500,  AI, _6E, SG));
        r.add(route(BLR, AMD, 140, 4500,  AI, _6E, QP));

        r.add(route(AMD, MAA, 155, 5000,  AI, _6E, I5));
        r.add(route(MAA, AMD, 155, 5000,  AI, _6E, SG));

        r.add(route(AMD, JAI,  70, 2800,  AI, _6E, SG));
        r.add(route(JAI, AMD,  70, 2800,  AI, _6E, QP));

        r.add(route(AMD, GOI, 110, 3800,  AI, _6E, SG));
        r.add(route(GOI, AMD, 110, 3800,  AI, _6E, QP));

        // ══ NEW: GOI spoke routes ════════════════════════════════════════════
        r.add(route(GOI, BLR,  90, 3200,  AI, _6E, SG));
        r.add(route(BLR, GOI,  90, 3200,  AI, _6E, QP));

        r.add(route(GOI, MAA, 120, 4200,  AI, _6E, I5));
        r.add(route(MAA, GOI, 120, 4200,  AI, _6E, SG));

        r.add(route(GOI, COK, 100, 3800,  AI, _6E, SG));
        r.add(route(COK, GOI, 100, 3800,  AI, _6E, QP));

        // ══ NEW: LKO spoke routes ════════════════════════════════════════════
        r.add(route(LKO, BLR, 180, 5800,  AI, _6E, SG));
        r.add(route(BLR, LKO, 180, 5800,  AI, _6E, QP));

        r.add(route(LKO, MAA, 175, 5500,  AI, _6E, I5));
        r.add(route(MAA, LKO, 175, 5500,  AI, _6E, SG));

        r.add(route(LKO, GOI, 175, 5600,  AI, _6E, QP));
        r.add(route(GOI, LKO, 175, 5600,  AI, _6E, SG));

        r.add(route(LKO, COK, 215, 6800,  AI, _6E, I5));
        r.add(route(COK, LKO, 215, 6800,  AI, _6E, SG));

        // ══ NEW: JAI spoke routes ════════════════════════════════════════════
        r.add(route(JAI, BLR, 175, 5600,  AI, _6E, SG));
        r.add(route(BLR, JAI, 175, 5600,  AI, _6E, QP));

        r.add(route(JAI, MAA, 185, 5800,  AI, _6E, I5));
        r.add(route(MAA, JAI, 185, 5800,  AI, _6E, SG));

        r.add(route(JAI, GOI, 165, 5200,  AI, _6E, QP));
        r.add(route(GOI, JAI, 165, 5200,  AI, _6E, SG));

        r.add(route(JAI, CCU, 195, 6200,  AI, _6E, I5));
        r.add(route(CCU, JAI, 195, 6200,  AI, _6E, SG));

        // ══ NEW: PNQ spoke routes ════════════════════════════════════════════
        r.add(route(PNQ, BLR, 110, 3800,  AI, _6E, SG));
        r.add(route(BLR, PNQ, 110, 3800,  AI, _6E, QP));

        r.add(route(PNQ, MAA, 115, 4000,  AI, _6E, I5));
        r.add(route(MAA, PNQ, 115, 4000,  AI, _6E, SG));

        r.add(route(PNQ, GOI,  70, 2800,  AI, _6E, SG));
        r.add(route(GOI, PNQ,  70, 2800,  AI, _6E, QP));

        r.add(route(PNQ, CCU, 165, 5200,  AI, _6E, I5));
        r.add(route(CCU, PNQ, 165, 5200,  AI, _6E, SG));

        // ══ NEW: COK–CCU direct ══════════════════════════════════════════════
        r.add(route(COK, CCU, 180, 5800,  AI, _6E, SG));
        r.add(route(CCU, COK, 180, 5800,  AI, _6E, QP));

        return r;
    }

    /** Varargs factory — keeps route definitions concise. */
    private Route route(Airport from, Airport to, int durationMins, int basePrice, Airline... airlines) {
        return new Route(from, to, durationMins, basePrice, airlines);
    }
}
