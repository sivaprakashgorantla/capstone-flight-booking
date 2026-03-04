package com.flight.reporting.service;

import com.flight.reporting.client.LiveDataClient;
import com.flight.reporting.dto.*;
import com.flight.reporting.exception.ReportNotFoundException;
import com.flight.reporting.model.*;
import com.flight.reporting.repository.ReportRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * UC10 — Core report generation service.
 * Step 1: Admin selects report type.
 * Step 2: System retrieves data (seeded + optional live via RestTemplate).
 * Step 3: Report generated and audit record persisted.
 * Step 4: Admin views or downloads report.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final ReportRecordRepository reportRecordRepository;
    private final LiveDataClient         liveDataClient;

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    // ═══════════════════════════════════════════════════════════════════════
    // UC10 Step 1 + 2 + 3 — generate each report type
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public BookingSummaryReport generateBookingSummary(String adminId,
                                                       String fromDate,
                                                       String toDate) {
        log.info("[UC10] Generating BOOKING_SUMMARY for admin={}", adminId);

        // UC10 Step 2: Try live data first, fall back to seeded stats
        Map<String, Object> live = liveDataClient.fetchBookingStats();
        boolean usedLive = live != null;

        // ── Core metrics (seeded baseline — realistic for demo) ───────────────
        long total         = 150L;
        long confirmed     = 89L;
        long cancelled     = 36L;
        long pendingPay    = 12L;
        long payFailed     = 8L;
        BigDecimal revenue = new BigDecimal("1875000");

        if (usedLive) {
            log.info("[UC10] BOOKING_SUMMARY using live data from booking-service");
            total      = toLong(live.get("total"), total);
            confirmed  = toLong(live.get("confirmed"), confirmed);
            cancelled  = toLong(live.get("cancelled"), cancelled);
            pendingPay = toLong(live.get("pendingPayment"), pendingPay);
            payFailed  = toLong(live.get("paymentFailed"), payFailed);
            revenue    = toBigDecimal(live.get("totalRevenue"), revenue);
        }

        BigDecimal avgValue = total > 0
                ? revenue.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BookingSummaryReport report = BookingSummaryReport.builder()
                .reportReference(saveRecord(ReportType.BOOKING_SUMMARY,
                        "Booking Summary Report", adminId, fromDate, toDate, (int) total))
                .reportType(ReportType.BOOKING_SUMMARY.name())
                .generatedAt(LocalDateTime.now().format(DISPLAY_FMT))
                .totalBookings(total)
                .confirmedBookings(confirmed)
                .cancelledBookings(cancelled)
                .pendingPaymentBookings(pendingPay)
                .paymentFailedBookings(payFailed)
                .totalRevenue(revenue)
                .averageBookingValue(avgValue)
                .bookingsByStatus(Map.of(
                        "CONFIRMED",          confirmed,
                        "CANCELLED",          cancelled,
                        "PENDING_PAYMENT",    pendingPay,
                        "PAYMENT_PROCESSING", 5L,
                        "PAYMENT_FAILED",     payFailed
                ))
                .topRoutes(buildTopRoutes())
                .monthlyTrend(buildBookingMonthlyTrend())
                .build();

        log.info("[UC10] BOOKING_SUMMARY generated: ref={}", report.getReportReference());
        return report;
    }

    @Transactional
    public RevenueReport generateRevenueReport(String adminId, String fromDate, String toDate) {
        log.info("[UC10] Generating REVENUE_REPORT for admin={}", adminId);

        BigDecimal total   = new BigDecimal("1875000");
        BigDecimal refunds = new BigDecimal("312500");
        BigDecimal net     = total.subtract(refunds);
        BigDecimal avg     = new BigDecimal("12500");

        RevenueReport report = RevenueReport.builder()
                .reportReference(saveRecord(ReportType.REVENUE_REPORT,
                        "Revenue Report", adminId, fromDate, toDate, 142))
                .reportType(ReportType.REVENUE_REPORT.name())
                .generatedAt(LocalDateTime.now().format(DISPLAY_FMT))
                .fromDate(fromDate)
                .toDate(toDate)
                .totalRevenue(total)
                .totalRefunds(refunds)
                .netRevenue(net)
                .averageTicketPrice(avg)
                .totalTransactions(142L)
                .monthlyBreakdown(List.of(
                        RevenueReport.MonthlyRevenue.builder().month("Jan 2026")
                                .revenue(new BigDecimal("525000"))
                                .refunds(new BigDecimal("87500"))
                                .net(new BigDecimal("437500")).transactions(42L).build(),
                        RevenueReport.MonthlyRevenue.builder().month("Feb 2026")
                                .revenue(new BigDecimal("680000"))
                                .refunds(new BigDecimal("112500"))
                                .net(new BigDecimal("567500")).transactions(54L).build(),
                        RevenueReport.MonthlyRevenue.builder().month("Mar 2026")
                                .revenue(new BigDecimal("670000"))
                                .refunds(new BigDecimal("112500"))
                                .net(new BigDecimal("557500")).transactions(46L).build()
                ))
                .revenueByRoute(List.of(
                        RevenueReport.RouteRevenue.builder().route("Delhi → Mumbai")
                                .revenue(new BigDecimal("562500")).tickets(45L)
                                .averagePrice(new BigDecimal("12500")).build(),
                        RevenueReport.RouteRevenue.builder().route("Mumbai → Bangalore")
                                .revenue(new BigDecimal("400000")).tickets(32L)
                                .averagePrice(new BigDecimal("12500")).build(),
                        RevenueReport.RouteRevenue.builder().route("Delhi → Bangalore")
                                .revenue(new BigDecimal("378000")).tickets(28L)
                                .averagePrice(new BigDecimal("13500")).build(),
                        RevenueReport.RouteRevenue.builder().route("Hyderabad → Chennai")
                                .revenue(new BigDecimal("300000")).tickets(25L)
                                .averagePrice(new BigDecimal("12000")).build(),
                        RevenueReport.RouteRevenue.builder().route("Bangalore → Kolkata")
                                .revenue(new BigDecimal("234500")).tickets(20L)
                                .averagePrice(new BigDecimal("11725")).build()
                ))
                .revenueByAirline(List.of(
                        RevenueReport.AirlineRevenue.builder().airline("Air India")
                                .revenue(new BigDecimal("843750")).bookings(65L).build(),
                        RevenueReport.AirlineRevenue.builder().airline("IndiGo")
                                .revenue(new BigDecimal("562500")).bookings(45L).build(),
                        RevenueReport.AirlineRevenue.builder().airline("SpiceJet")
                                .revenue(new BigDecimal("468750")).bookings(40L).build()
                ))
                .build();

        log.info("[UC10] REVENUE_REPORT generated: ref={}", report.getReportReference());
        return report;
    }

    @Transactional
    public FlightUtilizationReport generateFlightUtilization(String adminId,
                                                              String fromDate,
                                                              String toDate) {
        log.info("[UC10] Generating FLIGHT_UTILIZATION for admin={}", adminId);

        List<FlightUtilizationReport.FlightStat> flights = List.of(
                buildFlightStat("AI-101", "Air India",  "Delhi",     "Mumbai",    180, 162),
                buildFlightStat("6E-202", "IndiGo",     "Mumbai",    "Bangalore", 160, 128),
                buildFlightStat("UK-303", "Vistara",    "Delhi",     "Bangalore", 200, 140),
                buildFlightStat("SG-404", "SpiceJet",   "Hyderabad", "Chennai",   150, 120),
                buildFlightStat("AI-505", "Air India",  "Bangalore", "Kolkata",   180, 108),
                buildFlightStat("6E-606", "IndiGo",     "Mumbai",    "Hyderabad", 160,  72),
                buildFlightStat("UK-707", "Vistara",    "Delhi",     "Chennai",   200, 180),
                buildFlightStat("SG-808", "SpiceJet",   "Kolkata",   "Delhi",     150,  60)
        );

        long totalSeats  = flights.stream().mapToLong(f -> f.getTotalSeats()).sum();
        long filledSeats = flights.stream().mapToLong(f -> f.getSeatsBooked()).sum();
        double overall   = totalSeats > 0 ? round2((double) filledSeats / totalSeats * 100) : 0;

        long high   = flights.stream().filter(f -> f.getOccupancyRate() >= 80).count();
        long medium = flights.stream().filter(f -> f.getOccupancyRate() >= 50
                && f.getOccupancyRate() < 80).count();
        long low    = flights.stream().filter(f -> f.getOccupancyRate() < 50).count();

        FlightUtilizationReport report = FlightUtilizationReport.builder()
                .reportReference(saveRecord(ReportType.FLIGHT_UTILIZATION,
                        "Flight Utilization Report", adminId, fromDate, toDate, flights.size()))
                .reportType(ReportType.FLIGHT_UTILIZATION.name())
                .generatedAt(LocalDateTime.now().format(DISPLAY_FMT))
                .totalFlights((long) flights.size())
                .totalSeatsAvailable(totalSeats)
                .totalSeatsFilled(filledSeats)
                .overallOccupancyRate(overall)
                .flights(flights)
                .highUtilizationFlights(high)
                .mediumUtilizationFlights(medium)
                .lowUtilizationFlights(low)
                .build();

        log.info("[UC10] FLIGHT_UTILIZATION generated: ref={} occupancy={}%",
                report.getReportReference(), overall);
        return report;
    }

    @Transactional
    public CancellationReport generateCancellationReport(String adminId,
                                                          String fromDate,
                                                          String toDate) {
        log.info("[UC10] Generating CANCELLATION_REPORT for admin={}", adminId);

        Map<String, Object> live = liveDataClient.fetchCancellationStats();

        long total    = 36L;
        long refunded = 22L;
        long noRefund = 10L;
        long rejected = 4L;
        BigDecimal totalRefundAmt = new BigDecimal("312500");
        BigDecimal totalOriginal  = new BigDecimal("450000");

        if (live != null) {
            total    = toLong(live.get("total"), total);
            refunded = toLong(live.get("refunded"), refunded);
            noRefund = toLong(live.get("noRefund"), noRefund);
            rejected = toLong(live.get("rejected"), rejected);
        }

        double refundRate = total > 0 ? round2((double) refunded / total * 100) : 0;
        double avgRefundPct = 62.5;

        CancellationReport report = CancellationReport.builder()
                .reportReference(saveRecord(ReportType.CANCELLATION_REPORT,
                        "Cancellation Report", adminId, fromDate, toDate, (int) total))
                .reportType(ReportType.CANCELLATION_REPORT.name())
                .generatedAt(LocalDateTime.now().format(DISPLAY_FMT))
                .totalCancellations(total)
                .refundedCancellations(refunded)
                .noRefundCancellations(noRefund)
                .rejectedCancellations(rejected)
                .totalRefundAmount(totalRefundAmt)
                .totalOriginalAmount(totalOriginal)
                .refundRate(refundRate)
                .averageRefundPercentage(avgRefundPct)
                .byStatus(Map.of(
                        "REFUNDED", refunded,
                        "NO_REFUND", noRefund,
                        "REJECTED", rejected,
                        "PENDING", 0L
                ))
                .byReason(List.of(
                        CancellationReport.CancellationReason.builder().reason("Personal reasons")
                                .count(12).totalRefund(new BigDecimal("112500")).build(),
                        CancellationReport.CancellationReason.builder().reason("Business change")
                                .count(10).totalRefund(new BigDecimal("87500")).build(),
                        CancellationReport.CancellationReason.builder().reason("Weather concerns")
                                .count(8).totalRefund(new BigDecimal("75000")).build(),
                        CancellationReport.CancellationReason.builder().reason("Other / Not specified")
                                .count(6).totalRefund(new BigDecimal("37500")).build()
                ))
                .refundByTimeBand(List.of(
                        CancellationReport.RefundBand.builder().band(">48h (90% refund)")
                                .count(10).refundAmount(new BigDecimal("135000")).build(),
                        CancellationReport.RefundBand.builder().band("24–48h (75% refund)")
                                .count(8).refundAmount(new BigDecimal("90000")).build(),
                        CancellationReport.RefundBand.builder().band("12–24h (50% refund)")
                                .count(6).refundAmount(new BigDecimal("60000")).build(),
                        CancellationReport.RefundBand.builder().band("6–12h (25% refund)")
                                .count(4).refundAmount(new BigDecimal("27500")).build(),
                        CancellationReport.RefundBand.builder().band("<6h (0% refund)")
                                .count(8).refundAmount(BigDecimal.ZERO).build()
                ))
                .monthlyTrend(List.of(
                        CancellationReport.MonthlyCancellation.builder().month("Jan 2026")
                                .cancellations(10).refunds(new BigDecimal("87500")).build(),
                        CancellationReport.MonthlyCancellation.builder().month("Feb 2026")
                                .cancellations(14).refunds(new BigDecimal("125000")).build(),
                        CancellationReport.MonthlyCancellation.builder().month("Mar 2026")
                                .cancellations(12).refunds(new BigDecimal("100000")).build()
                ))
                .build();

        log.info("[UC10] CANCELLATION_REPORT generated: ref={}", report.getReportReference());
        return report;
    }

    @Transactional
    public SupportSummaryReport generateSupportSummary(String adminId,
                                                        String fromDate,
                                                        String toDate) {
        log.info("[UC10] Generating SUPPORT_SUMMARY for admin={}", adminId);

        Map<String, Object> live = liveDataClient.fetchSupportStats();

        long total      = 45L;
        long open       = 8L;
        long inProgress = 12L;
        long awaiting   = 5L;
        long resolved   = 15L;
        long closed     = 5L;

        if (live != null) {
            total      = toLong(live.get("total"),        total);
            open       = toLong(live.get("open"),         open);
            inProgress = toLong(live.get("inProgress"),   inProgress);
            awaiting   = toLong(live.get("awaitingUser"), awaiting);
            resolved   = toLong(live.get("resolved"),     resolved);
            closed     = toLong(live.get("closed"),       closed);
        }

        double resolutionRate = total > 0
                ? round2((double)(resolved + closed) / total * 100) : 0;

        SupportSummaryReport report = SupportSummaryReport.builder()
                .reportReference(saveRecord(ReportType.SUPPORT_SUMMARY,
                        "Support Summary Report", adminId, fromDate, toDate, (int) total))
                .reportType(ReportType.SUPPORT_SUMMARY.name())
                .generatedAt(LocalDateTime.now().format(DISPLAY_FMT))
                .totalTickets(total)
                .openTickets(open)
                .inProgressTickets(inProgress)
                .awaitingUserTickets(awaiting)
                .resolvedTickets(resolved)
                .closedTickets(closed)
                .resolutionRate(resolutionRate)
                .averageResolutionHours(18.4)
                .byStatus(Map.of(
                        "OPEN", open, "IN_PROGRESS", inProgress,
                        "AWAITING_USER", awaiting, "RESOLVED", resolved, "CLOSED", closed
                ))
                .byCategory(List.of(
                        buildCategoryStat("BOOKING_ISSUE",      12, 8,  18.0),
                        buildCategoryStat("PAYMENT_ISSUE",      10, 9,  12.0),
                        buildCategoryStat("REFUND_ISSUE",       8,  7,  24.0),
                        buildCategoryStat("FLIGHT_DELAY",       7,  5,  6.0),
                        buildCategoryStat("BAGGAGE",            4,  3,  30.0),
                        buildCategoryStat("GENERAL_ENQUIRY",    4,  4,  4.0)
                ))
                .byPriority(Map.of(
                        "URGENT", 5L, "HIGH", 12L, "MEDIUM", 18L, "LOW", 10L
                ))
                .agentPerformance(List.of(
                        SupportSummaryReport.AgentStat.builder()
                                .agentName("agent-booking").assigned(12).resolved(8)
                                .resolutionRate(66.7).build(),
                        SupportSummaryReport.AgentStat.builder()
                                .agentName("agent-payments").assigned(10).resolved(9)
                                .resolutionRate(90.0).build(),
                        SupportSummaryReport.AgentStat.builder()
                                .agentName("agent-refunds").assigned(8).resolved(7)
                                .resolutionRate(87.5).build(),
                        SupportSummaryReport.AgentStat.builder()
                                .agentName("agent-operations").assigned(7).resolved(5)
                                .resolutionRate(71.4).build(),
                        SupportSummaryReport.AgentStat.builder()
                                .agentName("agent-general").assigned(4).resolved(4)
                                .resolutionRate(100.0).build()
                ))
                .build();

        log.info("[UC10] SUPPORT_SUMMARY generated: ref={} resolutionRate={}%",
                report.getReportReference(), resolutionRate);
        return report;
    }

    @Transactional
    public PaymentReport generatePaymentReport(String adminId, String fromDate, String toDate) {
        log.info("[UC10] Generating PAYMENT_REPORT for admin={}", adminId);

        Map<String, Object> live = liveDataClient.fetchPaymentStats();

        long total      = 142L;
        long successful = 126L;
        long failed     = 16L;
        BigDecimal revenue = new BigDecimal("1875000");

        if (live != null) {
            total      = toLong(live.get("total"),      total);
            successful = toLong(live.get("successful"), successful);
            failed     = toLong(live.get("failed"),     failed);
            revenue    = toBigDecimal(live.get("totalRevenue"), revenue);
        }

        double successRate = total > 0 ? round2((double) successful / total * 100) : 0;
        double failureRate = total > 0 ? round2((double) failed / total * 100) : 0;
        BigDecimal avg = successful > 0
                ? revenue.divide(BigDecimal.valueOf(successful), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        PaymentReport report = PaymentReport.builder()
                .reportReference(saveRecord(ReportType.PAYMENT_REPORT,
                        "Payment Report", adminId, fromDate, toDate, (int) total))
                .reportType(ReportType.PAYMENT_REPORT.name())
                .generatedAt(LocalDateTime.now().format(DISPLAY_FMT))
                .totalPayments(total)
                .successfulPayments(successful)
                .failedPayments(failed)
                .pendingPayments(0L)
                .successRate(successRate)
                .failureRate(failureRate)
                .totalRevenue(revenue)
                .averagePaymentAmount(avg)
                .largestPayment(new BigDecimal("45000"))
                .smallestPayment(new BigDecimal("3500"))
                .byStatus(Map.of(
                        "COMPLETED", successful,
                        "FAILED",    failed,
                        "PENDING",   0L,
                        "REFUNDED",  22L
                ))
                .byMethod(List.of(
                        PaymentReport.PaymentMethodStat.builder().method("Credit Card")
                                .count(72).totalAmount(new BigDecimal("937500")).successRate(91.7).build(),
                        PaymentReport.PaymentMethodStat.builder().method("Debit Card")
                                .count(38).totalAmount(new BigDecimal("475000")).successRate(84.2).build(),
                        PaymentReport.PaymentMethodStat.builder().method("UPI")
                                .count(24).totalAmount(new BigDecimal("300000")).successRate(95.8).build(),
                        PaymentReport.PaymentMethodStat.builder().method("Net Banking")
                                .count(8).totalAmount(new BigDecimal("162500")).successRate(75.0).build()
                ))
                .monthlyTrend(List.of(
                        PaymentReport.MonthlyPayment.builder().month("Jan 2026")
                                .successful(40).failed(5).revenue(new BigDecimal("525000")).build(),
                        PaymentReport.MonthlyPayment.builder().month("Feb 2026")
                                .successful(51).failed(7).revenue(new BigDecimal("680000")).build(),
                        PaymentReport.MonthlyPayment.builder().month("Mar 2026")
                                .successful(35).failed(4).revenue(new BigDecimal("670000")).build()
                ))
                .failureReasons(List.of(
                        PaymentReport.FailureReason.builder().reason("Insufficient funds")
                                .count(7).percentage(43.8).build(),
                        PaymentReport.FailureReason.builder().reason("Card declined by bank")
                                .count(5).percentage(31.2).build(),
                        PaymentReport.FailureReason.builder().reason("Network timeout")
                                .count(3).percentage(18.8).build(),
                        PaymentReport.FailureReason.builder().reason("Invalid card details")
                                .count(1).percentage(6.2).build()
                ))
                .build();

        log.info("[UC10] PAYMENT_REPORT generated: ref={} successRate={}%",
                report.getReportReference(), successRate);
        return report;
    }

    @Transactional
    public DashboardReport generateDashboard(String adminId) {
        log.info("[UC10] Generating FULL_DASHBOARD for admin={}", adminId);

        // Attempt live pulls; fall back silently
        liveDataClient.fetchBookingStats();
        liveDataClient.fetchPaymentStats();

        DashboardReport report = DashboardReport.builder()
                .reportReference(saveRecord(ReportType.FULL_DASHBOARD,
                        "Full Analytics Dashboard", adminId, null, null, 6))
                .reportType(ReportType.FULL_DASHBOARD.name())
                .generatedAt(LocalDateTime.now().format(DISPLAY_FMT))
                // Booking
                .totalBookings(150L)
                .confirmedBookings(89L)
                .cancelledBookings(36L)
                .totalBookingRevenue(new BigDecimal("1875000"))
                // Revenue
                .totalRevenue(new BigDecimal("1875000"))
                .totalRefunds(new BigDecimal("312500"))
                .netRevenue(new BigDecimal("1562500"))
                .averageTicketPrice(new BigDecimal("12500"))
                // Flight
                .totalFlights(8L)
                .overallOccupancyRate(74.3)
                .highUtilizationFlights(4L)
                .lowUtilizationFlights(1L)
                // Cancellation
                .totalCancellations(36L)
                .totalRefundAmount(new BigDecimal("312500"))
                .cancellationRate(24.0)
                // Support
                .totalSupportTickets(45L)
                .openSupportTickets(8L)
                .ticketResolutionRate(44.4)
                .averageResolutionHours(18.4)
                // Payment
                .totalPayments(142L)
                .paymentSuccessRate(88.7)
                .failedPayments(16L)
                // Drill-down links
                .bookingDetailEndpoint("GET /reports/booking-summary")
                .revenueDetailEndpoint("GET /reports/revenue")
                .flightDetailEndpoint("GET /reports/flight-utilization")
                .cancellationDetailEndpoint("GET /reports/cancellations")
                .supportDetailEndpoint("GET /reports/support")
                .paymentDetailEndpoint("GET /reports/payments")
                .build();

        log.info("[UC10] FULL_DASHBOARD generated: ref={}", report.getReportReference());
        return report;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Report history
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ReportRecordResponse> getHistory() {
        return reportRecordRepository.findAllByOrderByGeneratedAtDesc()
                .stream().map(this::toRecordResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReportRecordResponse> getHistoryByType(String typeStr) {
        ReportType type = parseType(typeStr);
        return reportRecordRepository.findByReportTypeOrderByGeneratedAtDesc(type)
                .stream().map(this::toRecordResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReportRecordResponse getRecordById(Long id) {
        return reportRecordRepository.findById(id)
                .map(this::toRecordResponse)
                .orElseThrow(() -> new ReportNotFoundException(id));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    /** Persists audit record and returns the generated reference. */
    private String saveRecord(ReportType type, String title, String generatedBy,
                              String fromDate, String toDate, int recordCount) {
        String ref = generateReference();
        ReportRecord record = ReportRecord.builder()
                .reportReference(ref)
                .reportType(type)
                .title(title)
                .generatedBy(generatedBy)
                .fromDate(fromDate)
                .toDate(toDate)
                .recordCount(recordCount)
                .status(ReportStatus.GENERATED)
                .build();
        reportRecordRepository.save(record);
        return ref;
    }

    private String generateReference() {
        String ref = "RPT-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 8).toUpperCase();
        return reportRecordRepository.existsByReportReference(ref) ? generateReference() : ref;
    }

    private FlightUtilizationReport.FlightStat buildFlightStat(String flightNo, String airline,
            String dep, String dest, int total, int booked) {
        double occ  = round2((double) booked / total * 100);
        String band = occ >= 80 ? "HIGH" : occ >= 50 ? "MEDIUM" : "LOW";
        return FlightUtilizationReport.FlightStat.builder()
                .flightNumber(flightNo).airline(airline)
                .departureCity(dep).destinationCity(dest)
                .totalSeats(total).seatsBooked(booked).seatsAvailable(total - booked)
                .occupancyRate(occ).utilizationBand(band)
                .build();
    }

    private SupportSummaryReport.CategoryStat buildCategoryStat(String cat, long total,
            long resolved, double avgHours) {
        double rate = total > 0 ? round2((double) resolved / total * 100) : 0;
        return SupportSummaryReport.CategoryStat.builder()
                .category(cat).total(total).resolved(resolved)
                .resolutionRate(rate).avgResolutionHours(avgHours).build();
    }

    private List<BookingSummaryReport.RouteStats> buildTopRoutes() {
        return List.of(
                BookingSummaryReport.RouteStats.builder().route("Delhi → Mumbai")
                        .departureCity("Delhi").destinationCity("Mumbai")
                        .bookingCount(45).revenue(new BigDecimal("562500")).build(),
                BookingSummaryReport.RouteStats.builder().route("Mumbai → Bangalore")
                        .departureCity("Mumbai").destinationCity("Bangalore")
                        .bookingCount(32).revenue(new BigDecimal("400000")).build(),
                BookingSummaryReport.RouteStats.builder().route("Delhi → Bangalore")
                        .departureCity("Delhi").destinationCity("Bangalore")
                        .bookingCount(28).revenue(new BigDecimal("378000")).build(),
                BookingSummaryReport.RouteStats.builder().route("Hyderabad → Chennai")
                        .departureCity("Hyderabad").destinationCity("Chennai")
                        .bookingCount(25).revenue(new BigDecimal("300000")).build(),
                BookingSummaryReport.RouteStats.builder().route("Bangalore → Kolkata")
                        .departureCity("Bangalore").destinationCity("Kolkata")
                        .bookingCount(20).revenue(new BigDecimal("234500")).build()
        );
    }

    private List<BookingSummaryReport.MonthlyBooking> buildBookingMonthlyTrend() {
        return List.of(
                BookingSummaryReport.MonthlyBooking.builder().month("Jan 2026")
                        .bookings(45).revenue(new BigDecimal("525000")).build(),
                BookingSummaryReport.MonthlyBooking.builder().month("Feb 2026")
                        .bookings(58).revenue(new BigDecimal("680000")).build(),
                BookingSummaryReport.MonthlyBooking.builder().month("Mar 2026")
                        .bookings(47).revenue(new BigDecimal("670000")).build()
        );
    }

    private ReportType parseType(String value) {
        try {
            return ReportType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.flight.reporting.exception.BadRequestException(
                    "Invalid report type: " + value);
        }
    }

    private ReportRecordResponse toRecordResponse(ReportRecord r) {
        return ReportRecordResponse.builder()
                .id(r.getId())
                .reportReference(r.getReportReference())
                .reportType(r.getReportType() != null ? r.getReportType().name() : null)
                .title(r.getTitle())
                .generatedBy(r.getGeneratedBy())
                .fromDate(r.getFromDate())
                .toDate(r.getToDate())
                .recordCount(r.getRecordCount())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .generatedAt(r.getGeneratedAt())
                .build();
    }

    private long toLong(Object val, long fallback) {
        if (val == null) return fallback;
        return val instanceof Number ? ((Number) val).longValue() : fallback;
    }

    private BigDecimal toBigDecimal(Object val, BigDecimal fallback) {
        if (val == null) return fallback;
        try { return new BigDecimal(val.toString()); }
        catch (Exception e) { return fallback; }
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
