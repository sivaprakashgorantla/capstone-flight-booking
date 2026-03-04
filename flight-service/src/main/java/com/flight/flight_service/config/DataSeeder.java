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
 * Coverage : 30 route-pairs (60 one-way routes) across 12 major Indian cities
 * Airlines : Air India · IndiGo · SpiceJet · Vistara · AirAsia India · Akasa Air
 * Dates    : today → today + 29  (always current, no hardcoded dates)
 * Flights  : 3 per route per day (morning / afternoon / evening) = ~5 400 total
 * Pricing  : base ± weekend premium ± slot premium ± mild demand curve
 * Status   : SCHEDULED (default) · DELAYED (occasional evening) · CANCELLED (rare)
 *
 * Idempotent — skips seeding when table is non-empty (H2 create-drop wipes on restart).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final FlightRepository flightRepository;

    // ═════════════════════════════════════════════════════════════════════════
    // Domain records
    // ═════════════════════════════════════════════════════════════════════════

    private record Airport(String city, String code) {}

    private record Airline(String code, String name, int seats) {}

    /**
     * @param airlines  up to 3 airlines operating this route (slot 0 = morning, 1 = afternoon, 2 = evening)
     */
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
    private static final Airline _6E = new Airline("6E",  "IndiGo",        160);
    private static final Airline SG  = new Airline("SG",  "SpiceJet",      140);
    private static final Airline UK  = new Airline("UK",  "Vistara",       150);
    private static final Airline I5  = new Airline("I5",  "AirAsia India", 180);
    private static final Airline QP  = new Airline("QP",  "Akasa Air",     150);

    // ─── Departure times per slot (5 variants rotated by route+day for spread) ─
    //     Each inner array = {hour, minute}
    private static final int[][] MORNING   = {{6,0},{6,30},{7,0},{7,30},{8,0}};
    private static final int[][] AFTERNOON = {{11,0},{11,30},{12,0},{12,30},{13,0}};
    private static final int[][] EVENING   = {{16,0},{16,30},{17,0},{17,30},{18,0}};

    // ═════════════════════════════════════════════════════════════════════════
    // Entry point
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void run(String... args) {
        if (flightRepository.count() > 0) {
            log.info("DataSeeder: flights already present — skipping.");
            return;
        }

        LocalDate today     = LocalDate.now();
        List<Route> routes  = buildRoutes();
        List<Flight> batch  = new ArrayList<>(6000);
        int serial          = 1000;   // unique serial per restart (H2 drops table, so always starts fresh)

        for (int day = 0; day < 30; day++) {
            LocalDate date    = today.plusDays(day);
            boolean isWeekend = date.getDayOfWeek() == DayOfWeek.FRIDAY
                             || date.getDayOfWeek() == DayOfWeek.SATURDAY
                             || date.getDayOfWeek() == DayOfWeek.SUNDAY;

            for (int ri = 0; ri < routes.size(); ri++) {
                Route route      = routes.get(ri);
                int   variant    = (ri + day) % 5;   // spread departure times across the day

                int[] morningSlot   = MORNING[variant];
                int[] afternoonSlot = AFTERNOON[variant];
                int[] eveningSlot   = EVENING[variant];
                int[][] slots = {morningSlot, afternoonSlot, eveningSlot};

                for (int si = 0; si < route.airlines().length; si++) {
                    Airline       airline = route.airlines()[si];
                    int[]         slot    = slots[si];
                    LocalDateTime dep     = date.atTime(slot[0], slot[1]);
                    LocalDateTime arr     = dep.plusMinutes(route.durationMins());

                    // ── Pricing logic ─────────────────────────────────────────
                    double price = route.basePrice()
                            * (1.0 + (si == 0 ? 0.08 : si == 2 ? 0.05 : 0.0))  // slot premium
                            * (isWeekend ? 1.15 : 1.0)                           // weekend premium
                            * (1.0 + (day % 7) * 0.01);                          // mild demand curve
                    int roundedPrice = (int) Math.round(price / 50.0) * 50;      // round to ₹50

                    // ── Status: mostly SCHEDULED, occasional DELAYED/CANCELLED ─
                    FlightStatus status = FlightStatus.SCHEDULED;
                    if (si == 2 && (ri + day) % 9 == 0) {
                        status = FlightStatus.DELAYED;    // ~1 in 9 evening flights
                    } else if (si == 2 && (ri + day) % 40 == 0) {
                        status = FlightStatus.CANCELLED;  // very rare
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
                            .price(BigDecimal.valueOf(roundedPrice))
                            .totalSeats(airline.seats())
                            .availableSeats(airline.seats())
                            .status(status)
                            .build());
                }
            }
        }

        // Save in chunks of 500 for efficiency
        int chunkSize = 500;
        for (int i = 0; i < batch.size(); i += chunkSize) {
            flightRepository.saveAll(batch.subList(i, Math.min(i + chunkSize, batch.size())));
        }

        log.info("DataSeeder: ✅ seeded {} flights | {} routes | {} days ({} → {})",
                batch.size(), routes.size(), 30, today, today.plusDays(29));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Route definitions  —  30 pairs (60 one-way routes)
    // ═════════════════════════════════════════════════════════════════════════

    private List<Route> buildRoutes() {
        List<Route> r = new ArrayList<>(60);

        // ── DEL (Delhi) hub ───────────────────────────────────────────────────
        r.add(route(DEL, BOM, 135, 4500,  AI, _6E, UK ));   // Delhi → Mumbai
        r.add(route(BOM, DEL, 135, 4500,  AI, _6E, SG ));   // Mumbai → Delhi
        r.add(route(DEL, BLR, 165, 5500,  AI, _6E, UK ));   // Delhi → Bengaluru
        r.add(route(BLR, DEL, 165, 5500,  AI, _6E, QP ));   // Bengaluru → Delhi
        r.add(route(DEL, MAA, 155, 5200,  AI, _6E, I5 ));   // Delhi → Chennai
        r.add(route(MAA, DEL, 155, 5200,  AI, _6E, SG ));   // Chennai → Delhi
        r.add(route(DEL, HYD, 130, 4800,  AI, _6E, UK ));   // Delhi → Hyderabad
        r.add(route(HYD, DEL, 130, 4800,  AI, _6E, SG ));   // Hyderabad → Delhi
        r.add(route(DEL, CCU, 145, 5000,  AI, _6E, QP ));   // Delhi → Kolkata
        r.add(route(CCU, DEL, 145, 5000,  AI, _6E, I5 ));   // Kolkata → Delhi
        r.add(route(DEL, GOI, 155, 5200,  AI, _6E, SG ));   // Delhi → Goa
        r.add(route(GOI, DEL, 155, 5200,  AI, _6E, UK ));   // Goa → Delhi
        r.add(route(DEL, COK, 215, 6800,  AI, _6E, UK ));   // Delhi → Kochi
        r.add(route(COK, DEL, 215, 6800,  AI, _6E, SG ));   // Kochi → Delhi
        r.add(route(DEL, AMD,  90, 3200,  AI, _6E, QP ));   // Delhi → Ahmedabad
        r.add(route(AMD, DEL,  90, 3200,  AI, _6E, SG ));   // Ahmedabad → Delhi
        r.add(route(DEL, JAI,  55, 2500,  AI, _6E, SG ));   // Delhi → Jaipur
        r.add(route(JAI, DEL,  55, 2500,  AI, _6E, QP ));   // Jaipur → Delhi
        r.add(route(DEL, LKO,  70, 2800,  AI, _6E, QP ));   // Delhi → Lucknow
        r.add(route(LKO, DEL,  70, 2800,  AI, _6E, SG ));   // Lucknow → Delhi
        r.add(route(DEL, PNQ, 145, 4800,  AI, _6E, SG ));   // Delhi → Pune
        r.add(route(PNQ, DEL, 145, 4800,  AI, _6E, QP ));   // Pune → Delhi

        // ── BOM (Mumbai) hub ──────────────────────────────────────────────────
        r.add(route(BOM, BLR, 110, 4000,  AI, _6E, SG ));   // Mumbai → Bengaluru
        r.add(route(BLR, BOM, 110, 4000,  AI, _6E, UK ));   // Bengaluru → Mumbai
        r.add(route(BOM, MAA, 110, 4200,  AI, _6E, I5 ));   // Mumbai → Chennai
        r.add(route(MAA, BOM, 110, 4200,  AI, _6E, SG ));   // Chennai → Mumbai
        r.add(route(BOM, HYD,  80, 3200,  AI, _6E, QP ));   // Mumbai → Hyderabad
        r.add(route(HYD, BOM,  80, 3200,  AI, _6E, SG ));   // Hyderabad → Mumbai
        r.add(route(BOM, CCU, 155, 5000,  AI, _6E, UK ));   // Mumbai → Kolkata
        r.add(route(CCU, BOM, 155, 5000,  AI, _6E, SG ));   // Kolkata → Mumbai
        r.add(route(BOM, GOI,  70, 2800,  AI, _6E, SG ));   // Mumbai → Goa
        r.add(route(GOI, BOM,  70, 2800,  AI, _6E, QP ));   // Goa → Mumbai
        r.add(route(BOM, COK, 120, 4500,  AI, _6E, SG ));   // Mumbai → Kochi
        r.add(route(COK, BOM, 120, 4500,  AI, _6E, UK ));   // Kochi → Mumbai
        r.add(route(BOM, AMD,  65, 2500,  AI, _6E, QP ));   // Mumbai → Ahmedabad
        r.add(route(AMD, BOM,  65, 2500,  AI, _6E, SG ));   // Ahmedabad → Mumbai
        r.add(route(BOM, PNQ,  40, 1800,  AI, _6E, SG ));   // Mumbai → Pune
        r.add(route(PNQ, BOM,  40, 1800,  AI, _6E, QP ));   // Pune → Mumbai
        r.add(route(BOM, JAI, 135, 4200,  AI, _6E, SG ));   // Mumbai → Jaipur
        r.add(route(JAI, BOM, 135, 4200,  AI, _6E, QP ));   // Jaipur → Mumbai

        // ── South India routes ────────────────────────────────────────────────
        r.add(route(BLR, MAA,  60, 2500,  AI, _6E, UK ));   // Bengaluru → Chennai
        r.add(route(MAA, BLR,  60, 2500,  AI, _6E, SG ));   // Chennai → Bengaluru
        r.add(route(BLR, HYD,  70, 2800,  AI, _6E, SG ));   // Bengaluru → Hyderabad
        r.add(route(HYD, BLR,  70, 2800,  AI, _6E, QP ));   // Hyderabad → Bengaluru
        r.add(route(BLR, COK,  85, 3000,  AI, _6E, SG ));   // Bengaluru → Kochi
        r.add(route(COK, BLR,  85, 3000,  AI, _6E, QP ));   // Kochi → Bengaluru
        r.add(route(BLR, CCU, 155, 5200,  AI, _6E, UK ));   // Bengaluru → Kolkata
        r.add(route(CCU, BLR, 155, 5200,  AI, _6E, SG ));   // Kolkata → Bengaluru
        r.add(route(MAA, HYD,  75, 2800,  AI, _6E, SG ));   // Chennai → Hyderabad
        r.add(route(HYD, MAA,  75, 2800,  AI, _6E, QP ));   // Hyderabad → Chennai
        r.add(route(MAA, COK,  60, 2500,  AI, _6E, UK ));   // Chennai → Kochi
        r.add(route(COK, MAA,  60, 2500,  AI, _6E, SG ));   // Kochi → Chennai
        r.add(route(MAA, CCU, 155, 5000,  AI, _6E, SG ));   // Chennai → Kolkata
        r.add(route(CCU, MAA, 155, 5000,  AI, _6E, QP ));   // Kolkata → Chennai
        r.add(route(HYD, CCU, 120, 4800,  AI, _6E, SG ));   // Hyderabad → Kolkata
        r.add(route(CCU, HYD, 120, 4800,  AI, _6E, QP ));   // Kolkata → Hyderabad
        r.add(route(HYD, COK, 100, 3800,  AI, _6E, SG ));   // Hyderabad → Kochi
        r.add(route(COK, HYD, 100, 3800,  AI, _6E, QP ));   // Kochi → Hyderabad

        // ── Additional popular routes ─────────────────────────────────────────
        r.add(route(AMD, BLR, 140, 4500,  AI, _6E, SG ));   // Ahmedabad → Bengaluru
        r.add(route(BLR, AMD, 140, 4500,  AI, _6E, QP ));   // Bengaluru → Ahmedabad
        r.add(route(BLR, GOI,  90, 3200,  AI, _6E, SG ));   // Bengaluru → Goa
        r.add(route(GOI, BLR,  90, 3200,  AI, _6E, QP ));   // Goa → Bengaluru

        return r;
    }

    // ── Builder helper — keeps route definitions concise ─────────────────────
    private Route route(Airport from, Airport to, int durationMins, int basePrice,
                        Airline a1, Airline a2, Airline a3) {
        return new Route(from, to, durationMins, basePrice, new Airline[]{a1, a2, a3});
    }
}
