package com.flight.reporting.config;

import com.flight.reporting.model.*;
import com.flight.reporting.repository.ReportRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds sample report-history records on startup (idempotent).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ReportRecordRepository reportRecordRepository;

    private static final List<String> SEED_REFS = List.of(
            "RPT-SEED0001", "RPT-SEED0002", "RPT-SEED0003",
            "RPT-SEED0004", "RPT-SEED0005", "RPT-SEED0006"
    );

    @Override
    public void run(String... args) {
        long existing = SEED_REFS.stream()
                .filter(reportRecordRepository::existsByReportReference)
                .count();

        if (existing == SEED_REFS.size()) {
            log.info("[DataSeeder] Report seed data already present — skipping.");
            return;
        }

        log.info("[DataSeeder] Seeding {} sample report history records...", SEED_REFS.size());

        saveIfAbsent(ReportRecord.builder()
                .reportReference("RPT-SEED0001")
                .reportType(ReportType.FULL_DASHBOARD)
                .title("Full Analytics Dashboard")
                .generatedBy("admin@flightapp.com")
                .recordCount(6)
                .status(ReportStatus.GENERATED)
                .generatedAt(LocalDateTime.now().minusDays(1))
                .build());

        saveIfAbsent(ReportRecord.builder()
                .reportReference("RPT-SEED0002")
                .reportType(ReportType.BOOKING_SUMMARY)
                .title("Booking Summary Report")
                .generatedBy("admin@flightapp.com")
                .fromDate("2026-01-01")
                .toDate("2026-03-03")
                .recordCount(150)
                .status(ReportStatus.GENERATED)
                .generatedAt(LocalDateTime.now().minusDays(2))
                .build());

        saveIfAbsent(ReportRecord.builder()
                .reportReference("RPT-SEED0003")
                .reportType(ReportType.REVENUE_REPORT)
                .title("Revenue Report — Q1 2026")
                .generatedBy("admin@flightapp.com")
                .fromDate("2026-01-01")
                .toDate("2026-03-31")
                .recordCount(142)
                .status(ReportStatus.GENERATED)
                .generatedAt(LocalDateTime.now().minusDays(3))
                .build());

        saveIfAbsent(ReportRecord.builder()
                .reportReference("RPT-SEED0004")
                .reportType(ReportType.FLIGHT_UTILIZATION)
                .title("Flight Utilization Report")
                .generatedBy("admin@flightapp.com")
                .recordCount(8)
                .status(ReportStatus.GENERATED)
                .generatedAt(LocalDateTime.now().minusDays(4))
                .build());

        saveIfAbsent(ReportRecord.builder()
                .reportReference("RPT-SEED0005")
                .reportType(ReportType.CANCELLATION_REPORT)
                .title("Cancellation Report")
                .generatedBy("admin@flightapp.com")
                .recordCount(36)
                .status(ReportStatus.GENERATED)
                .generatedAt(LocalDateTime.now().minusDays(5))
                .build());

        saveIfAbsent(ReportRecord.builder()
                .reportReference("RPT-SEED0006")
                .reportType(ReportType.PAYMENT_REPORT)
                .title("Payment Report")
                .generatedBy("admin@flightapp.com")
                .recordCount(142)
                .status(ReportStatus.GENERATED)
                .generatedAt(LocalDateTime.now().minusDays(6))
                .build());

        log.info("[DataSeeder] ✓ Report history records seeded successfully.");
    }

    private void saveIfAbsent(ReportRecord r) {
        if (!reportRecordRepository.existsByReportReference(r.getReportReference())) {
            reportRecordRepository.save(r);
        }
    }
}
