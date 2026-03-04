package com.flight.reporting.service;

import com.flight.reporting.dto.*;
import com.flight.reporting.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * UC10 Step 4 — Admin downloads report.
 * Converts each report type into a downloadable CSV string.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final ReportGenerationService reportService;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    public CsvDownloadResponse exportBookingSummary(String adminId) {
        log.info("[UC10-CSV] Exporting BOOKING_SUMMARY for admin={}", adminId);
        BookingSummaryReport r = reportService.generateBookingSummary(adminId, null, null);

        StringBuilder csv = new StringBuilder();
        csv.append("Report,Booking Summary Report\n");
        csv.append("Reference,").append(r.getReportReference()).append("\n");
        csv.append("Generated At,").append(r.getGeneratedAt()).append("\n\n");

        csv.append("HEADLINE METRICS\n");
        csv.append("Metric,Value\n");
        csv.append("Total Bookings,").append(r.getTotalBookings()).append("\n");
        csv.append("Confirmed,").append(r.getConfirmedBookings()).append("\n");
        csv.append("Cancelled,").append(r.getCancelledBookings()).append("\n");
        csv.append("Pending Payment,").append(r.getPendingPaymentBookings()).append("\n");
        csv.append("Payment Failed,").append(r.getPaymentFailedBookings()).append("\n");
        csv.append("Total Revenue (INR),").append(r.getTotalRevenue()).append("\n");
        csv.append("Average Booking Value (INR),").append(r.getAverageBookingValue()).append("\n\n");

        csv.append("TOP ROUTES\n");
        csv.append("Route,Bookings,Revenue (INR)\n");
        if (r.getTopRoutes() != null) {
            r.getTopRoutes().forEach(rt ->
                    csv.append(rt.getRoute()).append(",")
                       .append(rt.getBookingCount()).append(",")
                       .append(rt.getRevenue()).append("\n"));
        }

        csv.append("\nMONTHLY TREND\n");
        csv.append("Month,Bookings,Revenue (INR)\n");
        if (r.getMonthlyTrend() != null) {
            r.getMonthlyTrend().forEach(m ->
                    csv.append(m.getMonth()).append(",")
                       .append(m.getBookings()).append(",")
                       .append(m.getRevenue()).append("\n"));
        }

        return buildResponse("booking_summary", r.getReportReference(), csv, countRows(r));
    }

    public CsvDownloadResponse exportRevenue(String adminId) {
        log.info("[UC10-CSV] Exporting REVENUE_REPORT for admin={}", adminId);
        RevenueReport r = reportService.generateRevenueReport(adminId, null, null);

        StringBuilder csv = new StringBuilder();
        csv.append("Report,Revenue Report\n");
        csv.append("Reference,").append(r.getReportReference()).append("\n");
        csv.append("Generated At,").append(r.getGeneratedAt()).append("\n\n");

        csv.append("HEADLINE\n");
        csv.append("Metric,Value\n");
        csv.append("Total Revenue (INR),").append(r.getTotalRevenue()).append("\n");
        csv.append("Total Refunds (INR),").append(r.getTotalRefunds()).append("\n");
        csv.append("Net Revenue (INR),").append(r.getNetRevenue()).append("\n");
        csv.append("Average Ticket Price (INR),").append(r.getAverageTicketPrice()).append("\n");
        csv.append("Total Transactions,").append(r.getTotalTransactions()).append("\n\n");

        csv.append("MONTHLY BREAKDOWN\n");
        csv.append("Month,Revenue,Refunds,Net,Transactions\n");
        if (r.getMonthlyBreakdown() != null) {
            r.getMonthlyBreakdown().forEach(m ->
                    csv.append(m.getMonth()).append(",").append(m.getRevenue()).append(",")
                       .append(m.getRefunds()).append(",").append(m.getNet()).append(",")
                       .append(m.getTransactions()).append("\n"));
        }

        csv.append("\nREVENUE BY ROUTE\n");
        csv.append("Route,Revenue,Tickets,Avg Price\n");
        if (r.getRevenueByRoute() != null) {
            r.getRevenueByRoute().forEach(rt ->
                    csv.append(rt.getRoute()).append(",").append(rt.getRevenue()).append(",")
                       .append(rt.getTickets()).append(",").append(rt.getAveragePrice()).append("\n"));
        }

        return buildResponse("revenue_report", r.getReportReference(), csv,
                r.getMonthlyBreakdown() != null ? r.getMonthlyBreakdown().size() : 0);
    }

    public CsvDownloadResponse exportFlightUtilization(String adminId) {
        log.info("[UC10-CSV] Exporting FLIGHT_UTILIZATION for admin={}", adminId);
        FlightUtilizationReport r = reportService.generateFlightUtilization(adminId, null, null);

        StringBuilder csv = new StringBuilder();
        csv.append("Report,Flight Utilization Report\n");
        csv.append("Reference,").append(r.getReportReference()).append("\n");
        csv.append("Generated At,").append(r.getGeneratedAt()).append("\n");
        csv.append("Overall Occupancy Rate,").append(r.getOverallOccupancyRate()).append("%\n\n");

        csv.append("FLIGHT BREAKDOWN\n");
        csv.append("Flight,Airline,Route,Total Seats,Booked,Available,Occupancy %,Band\n");
        if (r.getFlights() != null) {
            r.getFlights().forEach(f ->
                    csv.append(f.getFlightNumber()).append(",")
                       .append(f.getAirline()).append(",")
                       .append(f.getDepartureCity()).append(" → ").append(f.getDestinationCity()).append(",")
                       .append(f.getTotalSeats()).append(",")
                       .append(f.getSeatsBooked()).append(",")
                       .append(f.getSeatsAvailable()).append(",")
                       .append(f.getOccupancyRate()).append("%,")
                       .append(f.getUtilizationBand()).append("\n"));
        }

        return buildResponse("flight_utilization", r.getReportReference(), csv,
                r.getFlights() != null ? r.getFlights().size() : 0);
    }

    public CsvDownloadResponse exportCancellations(String adminId) {
        log.info("[UC10-CSV] Exporting CANCELLATION_REPORT for admin={}", adminId);
        CancellationReport r = reportService.generateCancellationReport(adminId, null, null);

        StringBuilder csv = new StringBuilder();
        csv.append("Report,Cancellation Report\n");
        csv.append("Reference,").append(r.getReportReference()).append("\n");
        csv.append("Generated At,").append(r.getGeneratedAt()).append("\n\n");

        csv.append("HEADLINE\n");
        csv.append("Metric,Value\n");
        csv.append("Total Cancellations,").append(r.getTotalCancellations()).append("\n");
        csv.append("Refunded,").append(r.getRefundedCancellations()).append("\n");
        csv.append("No Refund,").append(r.getNoRefundCancellations()).append("\n");
        csv.append("Rejected,").append(r.getRejectedCancellations()).append("\n");
        csv.append("Total Refund Amount (INR),").append(r.getTotalRefundAmount()).append("\n");
        csv.append("Refund Rate,").append(r.getRefundRate()).append("%\n\n");

        csv.append("BY REASON\n");
        csv.append("Reason,Count,Total Refund (INR)\n");
        if (r.getByReason() != null) {
            r.getByReason().forEach(rr ->
                    csv.append(rr.getReason()).append(",")
                       .append(rr.getCount()).append(",")
                       .append(rr.getTotalRefund()).append("\n"));
        }

        csv.append("\nREFUND BY TIME BAND\n");
        csv.append("Band,Count,Refund Amount (INR)\n");
        if (r.getRefundByTimeBand() != null) {
            r.getRefundByTimeBand().forEach(b ->
                    csv.append(b.getBand()).append(",")
                       .append(b.getCount()).append(",")
                       .append(b.getRefundAmount()).append("\n"));
        }

        return buildResponse("cancellation_report", r.getReportReference(), csv,
                (int) r.getTotalCancellations());
    }

    public CsvDownloadResponse exportSupport(String adminId) {
        log.info("[UC10-CSV] Exporting SUPPORT_SUMMARY for admin={}", adminId);
        SupportSummaryReport r = reportService.generateSupportSummary(adminId, null, null);

        StringBuilder csv = new StringBuilder();
        csv.append("Report,Support Summary Report\n");
        csv.append("Reference,").append(r.getReportReference()).append("\n");
        csv.append("Generated At,").append(r.getGeneratedAt()).append("\n\n");

        csv.append("HEADLINE\n");
        csv.append("Metric,Value\n");
        csv.append("Total Tickets,").append(r.getTotalTickets()).append("\n");
        csv.append("Open,").append(r.getOpenTickets()).append("\n");
        csv.append("In Progress,").append(r.getInProgressTickets()).append("\n");
        csv.append("Resolved,").append(r.getResolvedTickets()).append("\n");
        csv.append("Closed,").append(r.getClosedTickets()).append("\n");
        csv.append("Resolution Rate,").append(r.getResolutionRate()).append("%\n");
        csv.append("Avg Resolution Hours,").append(r.getAverageResolutionHours()).append("\n\n");

        csv.append("BY CATEGORY\n");
        csv.append("Category,Total,Resolved,Resolution Rate,Avg Hours\n");
        if (r.getByCategory() != null) {
            r.getByCategory().forEach(c ->
                    csv.append(c.getCategory()).append(",")
                       .append(c.getTotal()).append(",")
                       .append(c.getResolved()).append(",")
                       .append(c.getResolutionRate()).append("%,")
                       .append(c.getAvgResolutionHours()).append("\n"));
        }

        csv.append("\nAGENT PERFORMANCE\n");
        csv.append("Agent,Assigned,Resolved,Resolution Rate\n");
        if (r.getAgentPerformance() != null) {
            r.getAgentPerformance().forEach(a ->
                    csv.append(a.getAgentName()).append(",")
                       .append(a.getAssigned()).append(",")
                       .append(a.getResolved()).append(",")
                       .append(a.getResolutionRate()).append("%\n"));
        }

        return buildResponse("support_summary", r.getReportReference(), csv,
                (int) r.getTotalTickets());
    }

    public CsvDownloadResponse exportPayments(String adminId) {
        log.info("[UC10-CSV] Exporting PAYMENT_REPORT for admin={}", adminId);
        PaymentReport r = reportService.generatePaymentReport(adminId, null, null);

        StringBuilder csv = new StringBuilder();
        csv.append("Report,Payment Report\n");
        csv.append("Reference,").append(r.getReportReference()).append("\n");
        csv.append("Generated At,").append(r.getGeneratedAt()).append("\n\n");

        csv.append("HEADLINE\n");
        csv.append("Metric,Value\n");
        csv.append("Total Payments,").append(r.getTotalPayments()).append("\n");
        csv.append("Successful,").append(r.getSuccessfulPayments()).append("\n");
        csv.append("Failed,").append(r.getFailedPayments()).append("\n");
        csv.append("Success Rate,").append(r.getSuccessRate()).append("%\n");
        csv.append("Total Revenue (INR),").append(r.getTotalRevenue()).append("\n");
        csv.append("Avg Payment (INR),").append(r.getAveragePaymentAmount()).append("\n\n");

        csv.append("BY PAYMENT METHOD\n");
        csv.append("Method,Count,Total Amount (INR),Success Rate\n");
        if (r.getByMethod() != null) {
            r.getByMethod().forEach(m ->
                    csv.append(m.getMethod()).append(",")
                       .append(m.getCount()).append(",")
                       .append(m.getTotalAmount()).append(",")
                       .append(m.getSuccessRate()).append("%\n"));
        }

        csv.append("\nFAILURE REASONS\n");
        csv.append("Reason,Count,Percentage\n");
        if (r.getFailureReasons() != null) {
            r.getFailureReasons().forEach(f ->
                    csv.append(f.getReason()).append(",")
                       .append(f.getCount()).append(",")
                       .append(f.getPercentage()).append("%\n"));
        }

        return buildResponse("payment_report", r.getReportReference(), csv,
                (int) r.getTotalPayments());
    }

    public CsvDownloadResponse exportDashboard(String adminId) {
        log.info("[UC10-CSV] Exporting FULL_DASHBOARD for admin={}", adminId);
        DashboardReport r = reportService.generateDashboard(adminId);

        StringBuilder csv = new StringBuilder();
        csv.append("Report,Full Analytics Dashboard\n");
        csv.append("Reference,").append(r.getReportReference()).append("\n");
        csv.append("Generated At,").append(r.getGeneratedAt()).append("\n\n");

        csv.append("KPI,Value\n");
        csv.append("Total Bookings,").append(r.getTotalBookings()).append("\n");
        csv.append("Confirmed Bookings,").append(r.getConfirmedBookings()).append("\n");
        csv.append("Cancelled Bookings,").append(r.getCancelledBookings()).append("\n");
        csv.append("Total Revenue (INR),").append(r.getTotalRevenue()).append("\n");
        csv.append("Net Revenue (INR),").append(r.getNetRevenue()).append("\n");
        csv.append("Overall Occupancy Rate,").append(r.getOverallOccupancyRate()).append("%\n");
        csv.append("Total Cancellations,").append(r.getTotalCancellations()).append("\n");
        csv.append("Total Refunds (INR),").append(r.getTotalRefundAmount()).append("\n");
        csv.append("Cancellation Rate,").append(r.getCancellationRate()).append("%\n");
        csv.append("Support Tickets,").append(r.getTotalSupportTickets()).append("\n");
        csv.append("Open Tickets,").append(r.getOpenSupportTickets()).append("\n");
        csv.append("Ticket Resolution Rate,").append(r.getTicketResolutionRate()).append("%\n");
        csv.append("Payment Success Rate,").append(r.getPaymentSuccessRate()).append("%\n");
        csv.append("Failed Payments,").append(r.getFailedPayments()).append("\n");

        return buildResponse("full_dashboard", r.getReportReference(), csv, 6);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private CsvDownloadResponse buildResponse(String type, String ref,
                                              StringBuilder csv, int rows) {
        String filename = type + "_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        return CsvDownloadResponse.builder()
                .filename(filename)
                .reportType(type.toUpperCase())
                .generatedAt(LocalDateTime.now().format(FMT))
                .rowCount(rows)
                .csvContent(csv.toString())
                .build();
    }

    private int countRows(BookingSummaryReport r) {
        int rows = 0;
        if (r.getTopRoutes() != null) rows += r.getTopRoutes().size();
        if (r.getMonthlyTrend() != null) rows += r.getMonthlyTrend().size();
        return rows;
    }
}
