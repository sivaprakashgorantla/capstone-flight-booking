package com.flight.flight_service.config;

import com.flight.flight_service.model.Flight;
import com.flight.flight_service.model.FlightStatus;
import com.flight.flight_service.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Seeds 58 sample flights on startup for POC demonstration.
 * Routes  : DEL-BOM, BOM-DEL, DEL-BLR, BLR-DEL, BOM-BLR, BLR-BOM,
 *           BLR-HYD, HYD-BLR, DEL-MAA, MAA-DEL, MAA-BOM, BOM-MAA,
 *           DEL-CCU, CCU-DEL, BOM-GOI, GOI-BOM, HYD-BOM, MAA-BLR, BLR-MAA
 * Airlines : Air India (AI), IndiGo (6E), SpiceJet (SG),
 *            Vistara (UK), AirAsia India (I5), GoFirst (G8)
 * Dates    : 2026-03-10 (47 flights) + 2026-03-11 (11 flights)
 * Statuses : SCHEDULED (55), DELAYED (2), CANCELLED (1)
 * All seeds are idempotent — guarded by existsByFlightNumber.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final FlightRepository flightRepository;

    @Override
    public void run(String... args) {
        log.info("DataSeeder: seeding flight data...");

        // ════════════════════════════════════════════════════════════════
        // 2026-03-10  —  47 flights
        // ════════════════════════════════════════════════════════════════

        // ── DEL → BOM  (Delhi → Mumbai) — 5 flights ──────────────────
        seed("AI-101", "Air India",      "Delhi",     "DEL", "Mumbai",    "BOM", "2026-03-10T06:30", "2026-03-10T08:45", "4500.00", 180);
        seed("6E-201", "IndiGo",         "Delhi",     "DEL", "Mumbai",    "BOM", "2026-03-10T09:15", "2026-03-10T11:20", "3800.00", 160);
        seed("SG-301", "SpiceJet",       "Delhi",     "DEL", "Mumbai",    "BOM", "2026-03-10T14:00", "2026-03-10T16:05", "3500.00", 140);
        seed("UK-101", "Vistara",        "Delhi",     "DEL", "Mumbai",    "BOM", "2026-03-10T17:30", "2026-03-10T19:40", "5200.00", 150);
        seed("I5-101", "AirAsia India",  "Delhi",     "DEL", "Mumbai",    "BOM", "2026-03-10T20:15", "2026-03-10T22:25", "2900.00", 180);

        // ── BOM → DEL  (Mumbai → Delhi) — 4 flights ──────────────────
        seed("AI-102", "Air India",      "Mumbai",    "BOM", "Delhi",     "DEL", "2026-03-10T07:30", "2026-03-10T09:45", "4600.00", 180);
        seed("6E-202", "IndiGo",         "Mumbai",    "BOM", "Delhi",     "DEL", "2026-03-10T12:00", "2026-03-10T14:10", "3900.00", 160);
        seed("UK-102", "Vistara",        "Mumbai",    "BOM", "Delhi",     "DEL", "2026-03-10T16:30", "2026-03-10T18:45", "5400.00", 150);
        seed("SG-302", "SpiceJet",       "Mumbai",    "BOM", "Delhi",     "DEL", "2026-03-10T20:00", "2026-03-10T22:15", "3600.00", 140);

        // ── DEL → BLR  (Delhi → Bengaluru) — 3 flights ───────────────
        seed("AI-601", "Air India",      "Delhi",     "DEL", "Bengaluru", "BLR", "2026-03-10T07:00", "2026-03-10T09:45", "6200.00", 180);
        seed("6E-701", "IndiGo",         "Delhi",     "DEL", "Bengaluru", "BLR", "2026-03-10T11:00", "2026-03-10T13:50", "5500.00", 160);
        seed("UK-601", "Vistara",        "Delhi",     "DEL", "Bengaluru", "BLR", "2026-03-10T18:30", "2026-03-10T21:15", "7200.00", 150);

        // ── BLR → DEL  (Bengaluru → Delhi) — 3 flights ───────────────
        seed("AI-602", "Air India",      "Bengaluru", "BLR", "Delhi",     "DEL", "2026-03-10T08:30", "2026-03-10T11:15", "6300.00", 180);
        seed("6E-702", "IndiGo",         "Bengaluru", "BLR", "Delhi",     "DEL", "2026-03-10T13:30", "2026-03-10T16:15", "5600.00", 160);
        seed("SG-601", "SpiceJet",       "Bengaluru", "BLR", "Delhi",     "DEL", "2026-03-10T19:30", "2026-03-10T22:15", "5100.00", 140);

        // ── BOM → BLR  (Mumbai → Bengaluru) — 3 flights ──────────────
        seed("AI-801", "Air India",      "Mumbai",    "BOM", "Bengaluru", "BLR", "2026-03-10T07:00", "2026-03-10T09:10", "4800.00", 180);
        seed("6E-811", "IndiGo",         "Mumbai",    "BOM", "Bengaluru", "BLR", "2026-03-10T11:30", "2026-03-10T13:40", "4300.00", 160);
        seed("UK-801", "Vistara",        "Mumbai",    "BOM", "Bengaluru", "BLR", "2026-03-10T17:00", "2026-03-10T19:10", "5600.00", 150);

        // ── BLR → BOM  (Bengaluru → Mumbai) — 3 flights ──────────────
        seed("6E-812", "IndiGo",         "Bengaluru", "BLR", "Mumbai",    "BOM", "2026-03-10T07:30", "2026-03-10T09:40", "4400.00", 160);
        seed("AI-802", "Air India",      "Bengaluru", "BLR", "Mumbai",    "BOM", "2026-03-10T13:00", "2026-03-10T15:10", "4900.00", 180);
        seed("SG-801", "SpiceJet",       "Bengaluru", "BLR", "Mumbai",    "BOM", "2026-03-10T19:30", "2026-03-10T21:40", "3900.00", 140);

        // ── BLR → HYD  (Bengaluru → Hyderabad) — 3 flights ──────────
        seed("AI-401", "Air India",      "Bengaluru", "BLR", "Hyderabad", "HYD", "2026-03-10T08:00", "2026-03-10T09:10", "2800.00", 120);
        seed("6E-501", "IndiGo",         "Bengaluru", "BLR", "Hyderabad", "HYD", "2026-03-10T15:30", "2026-03-10T16:40", "2600.00", 140);
        seed("UK-501", "Vistara",        "Bengaluru", "BLR", "Hyderabad", "HYD", "2026-03-10T18:30", "2026-03-10T19:40", "3200.00", 120);

        // ── HYD → BLR  (Hyderabad → Bengaluru) — 2 flights ──────────
        seed("AI-402", "Air India",      "Hyderabad", "HYD", "Bengaluru", "BLR", "2026-03-10T10:00", "2026-03-10T11:05", "2900.00", 120);
        seed("6E-502", "IndiGo",         "Hyderabad", "HYD", "Bengaluru", "BLR", "2026-03-10T16:30", "2026-03-10T17:35", "2700.00", 140);

        // ── DEL → MAA  (Delhi → Chennai) — 2 flights ─────────────────
        seed("AI-902", "Air India",      "Delhi",     "DEL", "Chennai",   "MAA", "2026-03-10T07:00", "2026-03-10T09:35", "5900.00", 180);
        seed("6E-902", "IndiGo",         "Delhi",     "DEL", "Chennai",   "MAA", "2026-03-10T13:00", "2026-03-10T15:35", "5300.00", 160);

        // ── MAA → DEL  (Chennai → Delhi) — 2 flights ─────────────────
        seed("AI-903", "Air India",      "Chennai",   "MAA", "Delhi",     "DEL", "2026-03-10T10:00", "2026-03-10T12:35", "6000.00", 180);
        seed("6E-903", "IndiGo",         "Chennai",   "MAA", "Delhi",     "DEL", "2026-03-10T16:00", "2026-03-10T18:35", "5400.00", 160);

        // ── MAA → BOM  (Chennai → Mumbai) — 2 flights ────────────────
        seed("6E-801", "IndiGo",         "Chennai",   "MAA", "Mumbai",    "BOM", "2026-03-10T06:45", "2026-03-10T08:55", "4200.00", 150);
        seed("AI-1101","Air India",       "Chennai",   "MAA", "Mumbai",    "BOM", "2026-03-10T14:00", "2026-03-10T16:10", "4700.00", 180);

        // ── BOM → MAA  (Mumbai → Chennai) — 2 flights ────────────────
        seed("AI-1102","Air India",       "Mumbai",    "BOM", "Chennai",   "MAA", "2026-03-10T09:00", "2026-03-10T11:10", "4600.00", 180);
        seed("6E-1102","IndiGo",          "Mumbai",    "BOM", "Chennai",   "MAA", "2026-03-10T17:00", "2026-03-10T19:10", "4100.00", 160);

        // ── DEL → CCU  (Delhi → Kolkata) — 2 flights ─────────────────
        seed("AI-1201","Air India",       "Delhi",     "DEL", "Kolkata",   "CCU", "2026-03-10T07:30", "2026-03-10T10:00", "5600.00", 180);
        seed("6E-1201","IndiGo",          "Delhi",     "DEL", "Kolkata",   "CCU", "2026-03-10T15:00", "2026-03-10T17:30", "5100.00", 160);

        // ── CCU → DEL  (Kolkata → Delhi) — 2 flights ─────────────────
        seed("AI-1202","Air India",       "Kolkata",   "CCU", "Delhi",     "DEL", "2026-03-10T06:00", "2026-03-10T08:30", "5500.00", 180);
        seed("6E-1202","IndiGo",          "Kolkata",   "CCU", "Delhi",     "DEL", "2026-03-10T11:00", "2026-03-10T13:30", "4900.00", 160);

        // ── BOM → GOI  (Mumbai → Goa) — 2 flights ────────────────────
        seed("G8-101", "GoFirst",        "Mumbai",    "BOM", "Goa",       "GOI", "2026-03-10T07:00", "2026-03-10T08:10", "3500.00", 180);
        seed("6E-1301","IndiGo",          "Mumbai",    "BOM", "Goa",       "GOI", "2026-03-10T14:00", "2026-03-10T15:10", "3200.00", 160);

        // ── GOI → BOM  (Goa → Mumbai) — 2 flights ────────────────────
        seed("G8-102", "GoFirst",        "Goa",       "GOI", "Mumbai",    "BOM", "2026-03-10T09:30", "2026-03-10T10:40", "3600.00", 180);
        seed("AI-1302","Air India",       "Goa",       "GOI", "Mumbai",    "BOM", "2026-03-10T16:30", "2026-03-10T17:40", "4200.00", 180);

        // ── HYD → BOM  (Hyderabad → Mumbai) — 2 flights ──────────────
        seed("AI-1401","Air India",       "Hyderabad", "HYD", "Mumbai",    "BOM", "2026-03-10T08:30", "2026-03-10T10:00", "3800.00", 180);
        seed("6E-1401","IndiGo",          "Hyderabad", "HYD", "Mumbai",    "BOM", "2026-03-10T15:00", "2026-03-10T16:30", "3400.00", 160);

        // ── MAA → BLR  (Chennai → Bengaluru) — 2 flights ─────────────
        seed("AI-1501","Air India",       "Chennai",   "MAA", "Bengaluru", "BLR", "2026-03-10T07:30", "2026-03-10T08:30", "2700.00", 180);
        seed("6E-1501","IndiGo",          "Chennai",   "MAA", "Bengaluru", "BLR", "2026-03-10T15:00", "2026-03-10T16:00", "2400.00", 160);

        // ── BLR → MAA  (Bengaluru → Chennai) — 1 flight ──────────────
        seed("AI-1502","Air India",       "Bengaluru", "BLR", "Chennai",   "MAA", "2026-03-10T10:00", "2026-03-10T11:00", "2800.00", 180);

        // ── DELAYED (2026-03-10) ──────────────────────────────────────
        seedWithStatus("AI-901", "Air India",   "Delhi",     "DEL", "Chennai", "MAA",
                       "2026-03-10T08:00", "2026-03-10T10:30", "5100.00", 180, FlightStatus.DELAYED);
        seedWithStatus("SG-901", "SpiceJet",    "Mumbai",    "BOM", "Delhi",   "DEL",
                       "2026-03-10T21:00", "2026-03-10T23:15", "3300.00", 140, FlightStatus.DELAYED);

        // ── CANCELLED (2026-03-10) ────────────────────────────────────
        seedWithStatus("SG-1601","SpiceJet",    "Delhi",     "DEL", "Mumbai",  "BOM",
                       "2026-03-10T22:00", "2026-03-11T00:05", "3100.00", 140, FlightStatus.CANCELLED);

        // ════════════════════════════════════════════════════════════════
        // 2026-03-11  —  11 flights
        // ════════════════════════════════════════════════════════════════

        // ── DEL → BOM — 4 flights ─────────────────────────────────────
        seed("AI-103", "Air India",      "Delhi",     "DEL", "Mumbai",    "BOM", "2026-03-11T06:30", "2026-03-11T08:45", "4700.00", 180);
        seed("6E-203", "IndiGo",         "Delhi",     "DEL", "Mumbai",    "BOM", "2026-03-11T10:00", "2026-03-11T12:10", "4100.00", 160);
        seed("SG-303", "SpiceJet",       "Delhi",     "DEL", "Mumbai",    "BOM", "2026-03-11T14:30", "2026-03-11T16:40", "3700.00", 140);
        seed("UK-103", "Vistara",        "Delhi",     "DEL", "Mumbai",    "BOM", "2026-03-11T18:00", "2026-03-11T20:10", "5400.00", 150);

        // ── BOM → DEL — 3 flights ─────────────────────────────────────
        seed("AI-104", "Air India",      "Mumbai",    "BOM", "Delhi",     "DEL", "2026-03-11T08:00", "2026-03-11T10:15", "4800.00", 180);
        seed("6E-204", "IndiGo",         "Mumbai",    "BOM", "Delhi",     "DEL", "2026-03-11T13:00", "2026-03-11T15:15", "4200.00", 160);
        seed("UK-104", "Vistara",        "Mumbai",    "BOM", "Delhi",     "DEL", "2026-03-11T18:00", "2026-03-11T20:15", "5600.00", 150);

        // ── DEL → BLR — 2 flights ─────────────────────────────────────
        seed("AI-603", "Air India",      "Delhi",     "DEL", "Bengaluru", "BLR", "2026-03-11T08:00", "2026-03-11T10:50", "6500.00", 180);
        seed("6E-703", "IndiGo",         "Delhi",     "DEL", "Bengaluru", "BLR", "2026-03-11T14:00", "2026-03-11T16:50", "5800.00", 160);

        // ── BLR → HYD — 2 flights ─────────────────────────────────────
        seed("AI-403", "Air India",      "Bengaluru", "BLR", "Hyderabad", "HYD", "2026-03-11T09:00", "2026-03-11T10:10", "2900.00", 120);
        seed("6E-503", "IndiGo",         "Bengaluru", "BLR", "Hyderabad", "HYD", "2026-03-11T16:00", "2026-03-11T17:10", "2700.00", 140);

        log.info("DataSeeder: complete — {} flights in DB", flightRepository.count());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void seed(String num, String airline,
                      String depCity, String depAirport,
                      String destCity, String destAirport,
                      String dep, String arr,
                      String price, int seats) {
        seedWithStatus(num, airline, depCity, depAirport, destCity, destAirport,
                       dep, arr, price, seats, FlightStatus.SCHEDULED);
    }

    private void seedWithStatus(String num, String airline,
                                String depCity, String depAirport,
                                String destCity, String destAirport,
                                String dep, String arr,
                                String price, int seats, FlightStatus status) {
        if (flightRepository.existsByFlightNumber(num)) {
            log.debug("Skip (exists): {}", num);
            return;
        }
        flightRepository.save(Flight.builder()
                .flightNumber(num)
                .airline(airline)
                .departureCity(depCity)
                .departureAirport(depAirport)
                .destinationCity(destCity)
                .destinationAirport(destAirport)
                .departureTime(LocalDateTime.parse(dep))
                .arrivalTime(LocalDateTime.parse(arr))
                .price(new BigDecimal(price))
                .totalSeats(seats)
                .availableSeats(seats)
                .status(status)
                .build());
        log.info("Seeded [{}] {} {} → {} ({})", status, num, depCity, destCity, airline);
    }
}
