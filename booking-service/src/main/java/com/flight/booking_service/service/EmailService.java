package com.flight.booking_service.service;

import com.flight.booking_service.model.Booking;
import com.flight.booking_service.model.Passenger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Mock email service -- logs confirmation emails.
 * In production, replace with JavaMailSender or an email gateway (SendGrid, SES, etc.)
 */
@Slf4j
@Service
public class EmailService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public void sendBookingConfirmation(Booking booking, List<Passenger> passengers) {
        String subject = "Booking Confirmed! " + booking.getBookingReference()
                + " | " + booking.getDepartureCity() + " -> " + booking.getDestinationCity();

        String body = buildEmailBody(booking, passengers);

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║            BOOKING CONFIRMATION EMAIL SENT               ║");
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║ To      : {}", booking.getUserEmail());
        log.info("║ Subject : {}", subject);
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info(body);
        log.info("╚══════════════════════════════════════════════════════════╝");
    }

    public void sendPaymentFailureNotification(Booking booking, String reason) {
        log.warn("╔══════════════════════════════════════════════════════════╗");
        log.warn("║            PAYMENT FAILURE EMAIL SENT                    ║");
        log.warn("╠══════════════════════════════════════════════════════════╣");
        log.warn("║ To      : {}", booking.getUserEmail());
        log.warn("║ Booking : {}", booking.getBookingReference());
        log.warn("║ Reason  : {}", reason);
        log.warn("║ Action  : Please retry payment or contact support.       ║");
        log.warn("╚══════════════════════════════════════════════════════════╝");
    }

    private String buildEmailBody(Booking booking, List<Passenger> passengers) {
        StringBuilder sb = new StringBuilder();
        sb.append("║\n");
        sb.append("║  Dear ").append(booking.getUserId()).append(",\n");
        sb.append("║\n");
        sb.append("║  Your booking is CONFIRMED!\n");
        sb.append("║\n");
        sb.append("║  FLIGHT DETAILS\n");
        sb.append("║  Booking Ref  : ").append(booking.getBookingReference()).append("\n");
        sb.append("║  Flight       : ").append(booking.getFlightNumber()).append(" (").append(booking.getAirline()).append(")\n");
        sb.append("║  Route        : ").append(booking.getDepartureCity()).append(" (").append(booking.getDepartureAirport()).append(")")
          .append(" -> ").append(booking.getDestinationCity()).append(" (").append(booking.getDestinationAirport()).append(")\n");
        sb.append("║  Departure    : ").append(booking.getDepartureTime().format(FMT)).append("\n");
        sb.append("║  Arrival      : ").append(booking.getArrivalTime().format(FMT)).append("\n");
        sb.append("║  Payment Ref  : ").append(booking.getPaymentReference()).append("\n");
        sb.append("║  Total Paid   : Rs.").append(booking.getTotalAmount()).append("\n");
        sb.append("║\n");
        sb.append("║  PASSENGERS\n");
        for (int i = 0; i < passengers.size(); i++) {
            Passenger p = passengers.get(i);
            sb.append("║  [").append(i + 1).append("] ").append(p.getFirstName()).append(" ").append(p.getLastName())
              .append(" | Seat: ").append(p.getSeatNumber())
              .append(" | Age: ").append(p.getAge()).append("\n");
        }
        sb.append("║\n");
        sb.append("║  Have a safe flight!\n");
        return sb.toString();
    }
}
