package com.flight.reporting.controller;

import com.flight.reporting.dto.*;
import com.flight.reporting.service.CsvExportService;
import com.flight.reporting.service.JwtService;
import com.flight.reporting.service.ReportGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(
    name        = "Analytics & Reports",
    description = "UC10 — All report endpoints. Admin JWT (ROLE_ADMIN) required for every operation."
)
public class ReportController {

    private final ReportGenerationService reportService;
    private final CsvExportService        csvExportService;
    private final JwtService              jwtService;

    // ─────────────────────────────────────────────────────────────────────────
    // Health  (public)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "No auth required")
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<String>> health() {
        return ResponseEntity.ok(
                com.flight.reporting.dto.ApiResponse.success("Analytics-Reporting Service is UP", "UP"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC10 Step 1 + 2 + 3 — Generate & View Reports
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Full Dashboard — all KPIs (UC10 Step 1 → FULL_DASHBOARD)",
        description = "UC10 Step 4: View all key metrics on a single page. " +
                      "Includes booking, revenue, flight, cancellation, support and payment KPIs " +
                      "with drill-down links to each individual report.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dashboard generated"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<DashboardReport>> getDashboard(
            HttpServletRequest req) {

        String adminId = extractAdminId(req);
        log.info("[UC10] GET /reports/dashboard  admin={}", adminId);
        DashboardReport report = reportService.generateDashboard(adminId);
        return ResponseEntity.ok(com.flight.reporting.dto.ApiResponse.success(
                "Full dashboard generated — ref: " + report.getReportReference(), report));
    }

    @GetMapping("/booking-summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Booking Summary Report (UC10 → BOOKING_SUMMARY)",
        description = "UC10 Step 2: Retrieves booking data from booking-service (live) or " +
                      "seeded baseline. Returns total bookings, status breakdown, top routes and monthly trend.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report generated"),
        @ApiResponse(responseCode = "403", description = "Admin role required")
    })
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<BookingSummaryReport>> getBookingSummary(
            @Parameter(description = "Filter from date (yyyy-MM-dd)", example = "2026-01-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "Filter to date (yyyy-MM-dd)",   example = "2026-03-31")
            @RequestParam(required = false) String to,
            HttpServletRequest req) {

        String adminId = extractAdminId(req);
        log.info("[UC10] GET /reports/booking-summary  admin={} from={} to={}", adminId, from, to);
        BookingSummaryReport report = reportService.generateBookingSummary(adminId, from, to);
        return ResponseEntity.ok(com.flight.reporting.dto.ApiResponse.success(
                "Booking summary generated — ref: " + report.getReportReference(), report));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Revenue Report (UC10 → REVENUE_REPORT)",
        description = "Revenue breakdown by month, route and airline. Net revenue after refunds. " +
                      "Supports optional date-range filtering.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<RevenueReport>> getRevenueReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletRequest req) {

        String adminId = extractAdminId(req);
        log.info("[UC10] GET /reports/revenue  admin={} from={} to={}", adminId, from, to);
        RevenueReport report = reportService.generateRevenueReport(adminId, from, to);
        return ResponseEntity.ok(com.flight.reporting.dto.ApiResponse.success(
                "Revenue report generated — ref: " + report.getReportReference(), report));
    }

    @GetMapping("/flight-utilization")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Flight Utilization Report (UC10 → FLIGHT_UTILIZATION)",
        description = "Per-flight occupancy rates, seat fill percentages and utilization bands " +
                      "(HIGH ≥80% / MEDIUM 50–79% / LOW <50%). Includes fleet-wide summary.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<FlightUtilizationReport>> getFlightUtilization(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletRequest req) {

        String adminId = extractAdminId(req);
        log.info("[UC10] GET /reports/flight-utilization  admin={}", adminId);
        FlightUtilizationReport report = reportService.generateFlightUtilization(adminId, from, to);
        return ResponseEntity.ok(com.flight.reporting.dto.ApiResponse.success(
                "Flight utilization report generated — ref: " + report.getReportReference(), report));
    }

    @GetMapping("/cancellations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Cancellation Report (UC10 → CANCELLATION_REPORT)",
        description = "Cancellation counts and refund totals broken down by status, reason and " +
                      "time-before-departure band (>48h=90%, 24–48h=75%, 12–24h=50%, 6–12h=25%, <6h=0%). " +
                      "Also shows monthly cancellation trend. Attempts live data from cancellation-service.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<CancellationReport>> getCancellationReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletRequest req) {

        String adminId = extractAdminId(req);
        log.info("[UC10] GET /reports/cancellations  admin={}", adminId);
        CancellationReport report = reportService.generateCancellationReport(adminId, from, to);
        return ResponseEntity.ok(com.flight.reporting.dto.ApiResponse.success(
                "Cancellation report generated — ref: " + report.getReportReference(), report));
    }

    @GetMapping("/support")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Support Summary Report (UC10 → SUPPORT_SUMMARY)",
        description = "Support ticket metrics: counts by status and category, resolution rate, " +
                      "average resolution time and per-agent performance. " +
                      "Attempts live data from support-service.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<SupportSummaryReport>> getSupportSummary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletRequest req) {

        String adminId = extractAdminId(req);
        log.info("[UC10] GET /reports/support  admin={}", adminId);
        SupportSummaryReport report = reportService.generateSupportSummary(adminId, from, to);
        return ResponseEntity.ok(com.flight.reporting.dto.ApiResponse.success(
                "Support summary generated — ref: " + report.getReportReference(), report));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Payment Report (UC10 → PAYMENT_REPORT)",
        description = "Payment success/failure rates, revenue totals, breakdown by payment method, " +
                      "monthly payment trend and failure-reason analysis. " +
                      "Attempts live data from payment-service.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<PaymentReport>> getPaymentReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletRequest req) {

        String adminId = extractAdminId(req);
        log.info("[UC10] GET /reports/payments  admin={}", adminId);
        PaymentReport report = reportService.generatePaymentReport(adminId, from, to);
        return ResponseEntity.ok(com.flight.reporting.dto.ApiResponse.success(
                "Payment report generated — ref: " + report.getReportReference(), report));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Report History
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Report generation history",
        description = "Returns an audit log of every report generated, newest first. " +
                      "Each record includes the report reference, type, who ran it and when.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<List<ReportRecordResponse>>> getHistory(
            @Parameter(description = "Filter by report type (optional)",
                       example     = "BOOKING_SUMMARY")
            @RequestParam(required = false) String type) {

        log.info("[UC10] GET /reports/history  type={}", type);
        List<ReportRecordResponse> history = type != null
                ? reportService.getHistoryByType(type)
                : reportService.getHistory();
        return ResponseEntity.ok(com.flight.reporting.dto.ApiResponse.success(
                history.size() + " report record(s) found", history));
    }

    @GetMapping("/history/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary  = "Get report record by ID",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<ReportRecordResponse>> getHistoryById(
            @PathVariable Long id) {

        log.info("[UC10] GET /reports/history/{}", id);
        return ResponseEntity.ok(com.flight.reporting.dto.ApiResponse.success(
                "Report record found", reportService.getRecordById(id)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UC10 Step 4 — Download as CSV
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/download/booking-summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Download Booking Summary as CSV (UC10 Step 4)",
        description = "Generates a booking summary report and returns it as a downloadable CSV file. " +
                      "The response body contains the raw CSV; save with the suggested filename.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> downloadBookingSummary(HttpServletRequest req) {
        CsvDownloadResponse csv = csvExportService.exportBookingSummary(extractAdminId(req));
        log.info("[UC10-CSV] Booking summary download — file={} rows={}", csv.getFilename(), csv.getRowCount());
        return csvResponse(csv);
    }

    @GetMapping("/download/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary  = "Download Revenue Report as CSV (UC10 Step 4)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> downloadRevenue(HttpServletRequest req) {
        CsvDownloadResponse csv = csvExportService.exportRevenue(extractAdminId(req));
        log.info("[UC10-CSV] Revenue report download — file={}", csv.getFilename());
        return csvResponse(csv);
    }

    @GetMapping("/download/flight-utilization")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary  = "Download Flight Utilization Report as CSV (UC10 Step 4)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> downloadFlightUtilization(HttpServletRequest req) {
        CsvDownloadResponse csv = csvExportService.exportFlightUtilization(extractAdminId(req));
        log.info("[UC10-CSV] Flight utilization download — file={}", csv.getFilename());
        return csvResponse(csv);
    }

    @GetMapping("/download/cancellations")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary  = "Download Cancellation Report as CSV (UC10 Step 4)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> downloadCancellations(HttpServletRequest req) {
        CsvDownloadResponse csv = csvExportService.exportCancellations(extractAdminId(req));
        log.info("[UC10-CSV] Cancellation report download — file={}", csv.getFilename());
        return csvResponse(csv);
    }

    @GetMapping("/download/support")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary  = "Download Support Summary as CSV (UC10 Step 4)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> downloadSupport(HttpServletRequest req) {
        CsvDownloadResponse csv = csvExportService.exportSupport(extractAdminId(req));
        log.info("[UC10-CSV] Support summary download — file={}", csv.getFilename());
        return csvResponse(csv);
    }

    @GetMapping("/download/payments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary  = "Download Payment Report as CSV (UC10 Step 4)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> downloadPayments(HttpServletRequest req) {
        CsvDownloadResponse csv = csvExportService.exportPayments(extractAdminId(req));
        log.info("[UC10-CSV] Payment report download — file={}", csv.getFilename());
        return csvResponse(csv);
    }

    @GetMapping("/download/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Download Full Dashboard as CSV (UC10 Step 4)",
        description = "Exports all dashboard KPIs as a single CSV for offline analysis or sharing.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> downloadDashboard(HttpServletRequest req) {
        CsvDownloadResponse csv = csvExportService.exportDashboard(extractAdminId(req));
        log.info("[UC10-CSV] Dashboard download — file={}", csv.getFilename());
        return csvResponse(csv);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Admin: quick stats endpoint (summary counts)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary     = "Report generation statistics",
        description = "Returns count of reports generated, grouped by type.",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<com.flight.reporting.dto.ApiResponse<Map<String, Object>>> getStats(
            HttpServletRequest req) {

        log.info("[UC10] GET /reports/stats  admin={}", extractAdminId(req));
        List<ReportRecordResponse> all = reportService.getHistory();

        Map<String, Long> byType = new java.util.LinkedHashMap<>();
        for (ReportRecordResponse r : all) {
            byType.merge(r.getReportType(), 1L, Long::sum);
        }

        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("totalReportsGenerated", (long) all.size());
        stats.put("byType", byType);
        stats.put("availableReports", List.of(
                "BOOKING_SUMMARY", "REVENUE_REPORT", "FLIGHT_UTILIZATION",
                "CANCELLATION_REPORT", "SUPPORT_SUMMARY", "PAYMENT_REPORT", "FULL_DASHBOARD"
        ));

        return ResponseEntity.ok(com.flight.reporting.dto.ApiResponse.success("Report statistics", stats));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Returns CSV as plain-text attachment so browsers prompt a download. */
    private ResponseEntity<String> csvResponse(CsvDownloadResponse csv) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + csv.getFilename() + "\"")
                .body(csv.getCsvContent());
    }

    private String extractAdminId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                return jwtService.extractUserId(header.substring(7));
            } catch (Exception ignored) {}
        }
        return "admin";
    }
}
