package com.flight.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flight.ai.client.BookingServiceClient;
import com.flight.ai.client.FlightServiceClient;
import com.flight.ai.client.PaymentServiceClient;
import com.flight.ai.client.UserServiceClient;
import com.flight.ai.dto.*;
import com.flight.ai.session.ConversationSession;
import com.flight.ai.session.ConversationStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatbotService {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT    = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ChatClient           chatClient;
    private final BookingServiceClient bookingClient;
    private final FlightServiceClient  flightClient;
    private final PaymentServiceClient paymentClient;
    private final UserServiceClient    userClient;
    private final ObjectMapper         objectMapper;

    private final ConcurrentHashMap<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    public ChatbotService(ChatClient.Builder builder,
                          BookingServiceClient bookingClient,
                          FlightServiceClient flightClient,
                          PaymentServiceClient paymentClient,
                          UserServiceClient userClient,
                          ObjectMapper objectMapper) {
        this.chatClient    = builder.build();
        this.bookingClient = bookingClient;
        this.flightClient  = flightClient;
        this.paymentClient = paymentClient;
        this.userClient    = userClient;
        this.objectMapper  = objectMapper;
    }

    // =====================================================================
    // Entry Point
    // =====================================================================

    public String chat(String userMessage, String authToken, String sessionId) {
        String sid = (sessionId != null && !sessionId.isBlank())
                ? sessionId : UUID.randomUUID().toString();

        ConversationSession session = sessions.computeIfAbsent(sid, k -> new ConversationSession());
        session.setLastActivity(LocalDateTime.now());
        if (session.getAuthToken() == null) session.setAuthToken(authToken);

        log.info("chat step={} msg={}", session.getStep(), userMessage);
        return route(userMessage.trim(), session, authToken);
    }

    // =====================================================================
    // State Router
    // =====================================================================

    private String route(String msg, ConversationSession session, String authToken) {
        return switch (session.getStep()) {
            case IDLE                            -> handleIdle(msg, session, authToken);
            case AWAITING_DATE                   -> handleDate(msg, session, authToken);
            case AWAITING_FLIGHT_SELECTION       -> handleFlightSelection(msg, session, authToken);
            case AWAITING_SAVED_PASSENGER_CHOICE -> handleSavedPassengerChoice(msg, session, authToken);
            case AWAITING_PASSENGER_DETAILS      -> handlePassengerDetails(msg, session, authToken);
            case AWAITING_PAYMENT_METHOD         -> handlePaymentMethod(msg, session, authToken);
            case AWAITING_UPI_ID                 -> handleUpiId(msg, session, authToken);
            case AWAITING_CARD_LAST_FOUR         -> handleCardLastFour(msg, session, authToken);
            case DONE                            -> handleDone(msg, session, authToken);
        };
    }

    // =====================================================================
    // IDLE
    // =====================================================================

    private String handleIdle(String msg, ConversationSession session, String authToken) {
        String lower = msg.toLowerCase();

        if (lower.contains("upcoming") || lower.contains("next flight"))
            return buildBookingsList("Your Upcoming Flights", fetchUpcoming(authToken));
        if (lower.contains("completed") || lower.contains("past flight"))
            return buildBookingsList("Your Completed Flights", fetchCompleted(authToken));
        if (lower.contains("my booking") || lower.contains("all booking") || lower.contains("all my"))
            return buildBookingsList("All My Bookings", fetchAll(authToken));

        if (isBookingIntent(lower))
            return startBookingFlow(msg, session, authToken);

        return askAi("You are a helpful flight booking assistant. Answer briefly: " + msg);
    }

    private boolean isBookingIntent(String msg) {
        if (msg.contains("book") || msg.contains("ticket") || msg.contains("seat") ||
            msg.contains("reserve") || msg.contains("purchase")) return true;
        if (msg.contains("fly from") || msg.contains("flight from") || msg.contains("flight to") ||
            msg.contains("flights from") || msg.contains("flights to") ||
            msg.contains("travel from") || msg.contains("cheap flight") || msg.contains("cheapest") ||
            msg.contains("find flight") || msg.contains("search flight") ||
            msg.contains("show flight") || msg.contains("available flight") ||
            msg.contains("any flight")) return true;
        if (msg.contains("price") || msg.contains("prices") || msg.contains("fare") ||
            msg.contains("fares") || msg.contains("cost") || msg.contains("how much") ||
            msg.contains("rate") || msg.contains("rates")) return true;
        if (msg.contains("i need a") || msg.contains("i need to fly") ||
            msg.contains("i want to go") || msg.contains("i want to fly") ||
            msg.contains("help me fly") || msg.contains("plan a trip")) return true;
        if (msg.matches(".*\\bflight[s]?\\b.*")) return true;
        if (msg.matches(".*\\bfly\\b.*"))         return true;
        return false;
    }

    // =====================================================================
    // STEP 1 — Extract booking parameters via AI
    // =====================================================================

    private String startBookingFlow(String msg, ConversationSession session, String authToken) {
        String prompt = "Extract flight booking parameters from this message. Return ONLY valid JSON, no markdown.\n"
                + "Message: \"" + msg + "\"\n"
                + "Return exactly: {\"fromCity\":\"city or null\",\"toCity\":\"city or null\",\"passengers\":1,\"travelDate\":\"YYYY-MM-DD or null\"}\n"
                + "Use proper city names: Hyderabad, Goa, Mumbai, Delhi, Bangalore, Chennai etc.";

        try {
            String json = askAi(prompt).replaceAll("```json|```", "").trim();
            JsonNode node = objectMapper.readTree(json);

            String fromCity   = jsonStr(node, "fromCity");
            String toCity     = jsonStr(node, "toCity");
            int    passengers = (node.has("passengers") && !node.get("passengers").isNull())
                                    ? node.get("passengers").asInt(1) : 1;
            String dateStr    = jsonStr(node, "travelDate");

            session.setFromCity(fromCity);
            session.setToCity(toCity);
            session.setPassengers(Math.max(1, passengers));
            if (dateStr != null) session.setTravelDate(parseDate(dateStr));

            if (fromCity == null || toCity == null) {
                session.setStep(ConversationStep.AWAITING_DATE);
                if (session.getTravelDate() != null) {
                    return "Great! I have your travel date as **" + session.getTravelDate() + "**.\n\n"
                            + "Which cities are you flying between?\n"
                            + "Example: Hyderabad to Goa";
                }
                return "I would love to help you book a flight!\n\n"
                        + "Please tell me:\n- From which city?\n- To which city?\n- Travel date (YYYY-MM-DD)\n\n"
                        + "Example: Hyderabad to Goa on 2026-05-10";
            }
            if (session.getTravelDate() == null) {
                session.setStep(ConversationStep.AWAITING_DATE);
                return "Searching flights from **" + fromCity + "** to **" + toCity
                        + "** for " + passengers + " seat(s).\n\n"
                        + "What is your travel date? (YYYY-MM-DD, e.g. 2026-05-10)";
            }
            return searchAndShowFlights(session, authToken);

        } catch (Exception e) {
            log.warn("Param extraction failed: {}", e.getMessage());
            session.setStep(ConversationStep.AWAITING_DATE);
            return "I would love to help!\nPlease tell me:\n- From city -> To city\n- Travel date (YYYY-MM-DD)\n\n"
                    + "Example: Hyderabad to Goa on 2026-05-10";
        }
    }

    // =====================================================================
    // STEP 2 — Collect travel date
    // =====================================================================

    private String handleDate(String msg, ConversationSession session, String authToken) {
        if (session.getFromCity() == null || session.getToCity() == null) {
            return startBookingFlow(msg, session, authToken);
        }
        if (session.getTravelDate() != null) {
            return searchAndShowFlights(session, authToken);
        }

        LocalDate date = parseDate(msg);
        if (date == null) {
            String extracted = askAi(
                    "Extract only the travel date as YYYY-MM-DD from: \""
                    + msg + "\". Return only the date string, nothing else. Return null if not found."
            ).trim();
            date = parseDate(extracted);
        }
        if (date == null)                   return "Couldn't read the date. Please use YYYY-MM-DD (e.g. 2026-05-10):";
        if (date.isBefore(LocalDate.now())) return "Date must be today or in the future. Please enter a valid date (YYYY-MM-DD):";

        session.setTravelDate(date);
        return searchAndShowFlights(session, authToken);
    }

    // =====================================================================
    // STEP 3 — Search flights & display
    // =====================================================================

    private String searchAndShowFlights(ConversationSession session, String authToken) {
        try {
            FlightSearchApiResponse resp = flightClient.searchFlights(
                    session.getFromCity(), session.getToCity(),
                    session.getTravelDate().format(DATE_FMT), session.getPassengers());

            if (resp == null || resp.getData() == null
                    || resp.getData().getFlights() == null
                    || resp.getData().getFlights().isEmpty()) {
                session.setStep(ConversationStep.IDLE);
                return "No flights found from " + session.getFromCity() + " to " + session.getToCity()
                        + " on " + session.getTravelDate() + ". Try a different date.";
            }

            List<FlightInfo> flights = resp.getData().getFlights();
            flights.sort(Comparator.comparing(FlightInfo::getPrice));
            session.setSearchResults(flights);
            session.setStep(ConversationStep.AWAITING_FLIGHT_SELECTION);
            return buildFlightListMessage(flights, session);

        } catch (Exception e) {
            log.error("Flight search error: {}", e.getMessage());
            session.setStep(ConversationStep.IDLE);
            return "Unable to search flights right now. Please try again.";
        }
    }

    private String buildFlightListMessage(List<FlightInfo> flights, ConversationSession session) {
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(flights.size()).append(" flight(s) | ")
          .append(session.getFromCity()).append(" -> ").append(session.getToCity())
          .append(" | ").append(session.getTravelDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
          .append(" | ").append(session.getPassengers()).append(" seat(s)\n\n");

        for (int i = 0; i < flights.size(); i++) {
            FlightInfo f     = flights.get(i);
            long hrs         = f.getDurationMinutes() / 60;
            long mins        = f.getDurationMinutes() % 60;
            BigDecimal total = f.getPrice().multiply(BigDecimal.valueOf(session.getPassengers()));

            sb.append(i + 1).append(". ").append(f.getAirline()).append(" ").append(f.getFlightNumber());
            if (i == 0) sb.append("  [CHEAPEST]");
            sb.append("\n");
            sb.append("   Departs : ").append(f.getDepartureTime() != null ? f.getDepartureTime().format(DISPLAY_FMT) : "N/A").append("\n");
            sb.append("   Arrives : ").append(f.getArrivalTime()   != null ? f.getArrivalTime().format(DISPLAY_FMT)   : "N/A").append("\n");
            sb.append("   Duration: ").append(hrs).append("h ").append(mins).append("m")
              .append("  |  Seats left: ").append(f.getAvailableSeats()).append("\n");
            sb.append("   Price   : Rs.").append(String.format("%.0f", f.getPrice()))
              .append("/seat  =>  Total: Rs.").append(String.format("%.0f", total)).append("\n\n");
        }
        sb.append("Reply with flight number (1, 2 ...) or say 'cheapest' to select:");
        return sb.toString();
    }

    // =====================================================================
    // STEP 4 — Select flight
    // =====================================================================

    private String handleFlightSelection(String msg, ConversationSession session, String authToken) {
        List<FlightInfo> flights = session.getSearchResults();
        if (flights == null || flights.isEmpty()) {
            session.setStep(ConversationStep.IDLE);
            return "Session expired. Please start again.";
        }

        String lower = msg.toLowerCase().trim();
        FlightInfo selected = null;

        if (lower.contains("cheap") || lower.equals("1") || lower.contains("first")) {
            selected = flights.get(0);
        } else {
            try {
                int idx = Integer.parseInt(lower.replaceAll("[^0-9]", "").trim()) - 1;
                if (idx >= 0 && idx < flights.size()) selected = flights.get(idx);
            } catch (Exception ignored) {}

            if (selected == null) {
                for (FlightInfo f : flights) {
                    if (lower.contains(f.getFlightNumber().toLowerCase())
                            || lower.contains(f.getAirline().toLowerCase())) {
                        selected = f;
                        break;
                    }
                }
            }
        }

        if (selected == null) {
            return "Please reply with a number between 1 and " + flights.size() + " or say 'cheapest':";
        }

        session.setSelectedFlight(selected);
        session.setCollectedPassengers(new ArrayList<>());

        BigDecimal total = selected.getPrice().multiply(BigDecimal.valueOf(session.getPassengers()));
        long hrs  = selected.getDurationMinutes() / 60;
        long mins = selected.getDurationMinutes() % 60;

        String flightSummary = "Selected: " + selected.getAirline() + " " + selected.getFlightNumber() + "\n"
                + "Route   : " + selected.getDepartureCity() + " -> " + selected.getDestinationCity() + "\n"
                + "Departs : " + (selected.getDepartureTime() != null ? selected.getDepartureTime().format(DISPLAY_FMT) : "N/A") + "\n"
                + "Duration: " + hrs + "h " + mins + "m\n"
                + "Total   : Rs." + String.format("%.0f", total) + " (" + session.getPassengers() + " seat(s))\n\n";

        return flightSummary + promptForNextPassenger(session, authToken);
    }

    // =====================================================================
    // STEP 5a — Show saved passengers (or go straight to manual entry)
    // =====================================================================

    /**
     * Called whenever we need details for the next passenger.
     * If the user has saved passengers and hasn't been offered them yet
     * (savedPassengers == null means we haven't fetched), fetch & display.
     */
    private String promptForNextPassenger(ConversationSession session, String authToken) {
        int nextIndex = session.getCollectedPassengers().size() + 1;

        // Fetch saved passengers once per session
        if (session.getSavedPassengers() == null) {
            session.setSavedPassengers(fetchSavedPassengers(authToken));
        }

        List<SavedPassengerDto> saved = session.getSavedPassengers();

        if (saved != null && !saved.isEmpty()) {
            session.setStep(ConversationStep.AWAITING_SAVED_PASSENGER_CHOICE);
            return buildSavedPassengerMenu(saved, nextIndex, session.getPassengers());
        }

        // No saved passengers — go straight to manual entry
        session.setStep(ConversationStep.AWAITING_PASSENGER_DETAILS);
        return "Please provide Passenger " + nextIndex + " details:\n"
                + "First Name: \nLast Name: \nAge: \nGender: MALE/FEMALE\nEmail: \nPhone: \nPassport: (optional)";
    }

    private String buildSavedPassengerMenu(List<SavedPassengerDto> saved, int passengerIndex, int totalPassengers) {
        StringBuilder sb = new StringBuilder();
        sb.append("Passenger ").append(passengerIndex).append(" of ").append(totalPassengers).append("\n\n");
        sb.append("Your saved profiles:\n");
        for (int i = 0; i < saved.size(); i++) {
            SavedPassengerDto sp = saved.get(i);
            sb.append(i + 1).append(". ").append(sp.getLabel())
              .append(" — ").append(sp.getFirstName()).append(" ").append(sp.getLastName())
              .append(", ").append(sp.getAge()).append(" yrs, ").append(sp.getGender())
              .append("\n");
        }
        sb.append("\nReply with:\n");
        sb.append("  • A number (1-").append(saved.size()).append(") to use a saved profile\n");
        sb.append("  • 'new' to enter details manually");
        return sb.toString();
    }

    // =====================================================================
    // STEP 5b — Handle saved passenger choice
    // =====================================================================

    private String handleSavedPassengerChoice(String msg, ConversationSession session, String authToken) {
        List<SavedPassengerDto> saved = session.getSavedPassengers();
        String lower = msg.toLowerCase().trim();

        if (lower.equals("new") || lower.equals("manual") || lower.contains("enter") || lower.contains("myself")) {
            session.setStep(ConversationStep.AWAITING_PASSENGER_DETAILS);
            int nextIndex = session.getCollectedPassengers().size() + 1;
            return "Please provide Passenger " + nextIndex + " details:\n"
                    + "First Name: \nLast Name: \nAge: \nGender: MALE/FEMALE\nEmail: \nPhone: \nPassport: (optional)";
        }

        // Try to parse a number
        try {
            int idx = Integer.parseInt(lower.replaceAll("[^0-9]", "").trim()) - 1;
            if (saved != null && idx >= 0 && idx < saved.size()) {
                SavedPassengerDto sp = saved.get(idx);
                PassengerRequest p = PassengerRequest.builder()
                        .firstName(sp.getFirstName())
                        .lastName(sp.getLastName() != null ? sp.getLastName() : "")
                        .age(sp.getAge())
                        .gender(sp.getGender() != null ? sp.getGender() : "MALE")
                        .email(sp.getEmail())
                        .phone(sp.getPhone())
                        .passportNumber(sp.getPassportNumber())
                        .build();

                session.getCollectedPassengers().add(p);
                int collected = session.getCollectedPassengers().size();
                log.info("Saved passenger '{}' selected for passenger slot {}", sp.getLabel(), collected);

                if (collected < session.getPassengers()) {
                    return "Passenger " + collected + " set to **" + sp.getLabel() + "** ("
                            + sp.getFirstName() + " " + sp.getLastName() + ")!\n\n"
                            + promptForNextPassenger(session, authToken);
                }
                return createBooking(session, authToken);
            }
        } catch (Exception ignored) {}

        // Not a valid number — re-show menu
        if (saved != null && !saved.isEmpty()) {
            return "Please reply with a number between 1 and " + saved.size() + ", or type 'new':\n\n"
                    + buildSavedPassengerMenu(saved, session.getCollectedPassengers().size() + 1, session.getPassengers());
        }
        session.setStep(ConversationStep.AWAITING_PASSENGER_DETAILS);
        int nextIndex = session.getCollectedPassengers().size() + 1;
        return "Please provide Passenger " + nextIndex + " details:\n"
                + "First Name: \nLast Name: \nAge: \nGender: MALE/FEMALE\nEmail: \nPhone: \nPassport: (optional)";
    }

    // =====================================================================
    // STEP 5c — Manual passenger details (AI-assisted)
    // =====================================================================

    private String handlePassengerDetails(String msg, ConversationSession session, String authToken) {
        String prompt = "Extract passenger details from this text. Return ONLY valid JSON, no markdown.\n"
                + "Text: \"" + msg.replace("\"", "'") + "\"\n"
                + "Return: {\"firstName\":\"...\",\"lastName\":\"...\",\"age\":0,"
                + "\"gender\":\"MALE\",\"email\":\"...\",\"phone\":\"...\",\"passportNumber\":null}\n"
                + "gender must be MALE or FEMALE. Use null for missing strings, 0 for missing age.";

        try {
            String json = askAi(prompt).replaceAll("```json|```", "").trim();
            JsonNode node = objectMapper.readTree(json);

            String firstName = jsonStr(node, "firstName");
            String email     = jsonStr(node, "email");
            String phone     = jsonStr(node, "phone");

            if (firstName == null || email == null || phone == null) {
                return "Some details are missing. Please provide:\n"
                        + "First Name: John\nLast Name: Doe\nAge: 30\n"
                        + "Gender: MALE\nEmail: john@example.com\nPhone: 9876543210";
            }

            PassengerRequest p = PassengerRequest.builder()
                    .firstName(firstName)
                    .lastName(jsonStr(node, "lastName") != null ? jsonStr(node, "lastName") : "")
                    .age(node.has("age") && !node.get("age").isNull() ? node.get("age").asInt(0) : 0)
                    .gender(jsonStr(node, "gender") != null ? jsonStr(node, "gender") : "MALE")
                    .email(email)
                    .phone(phone)
                    .passportNumber(jsonStr(node, "passportNumber"))
                    .build();

            session.getCollectedPassengers().add(p);
            int collected = session.getCollectedPassengers().size();

            if (collected < session.getPassengers()) {
                return "Passenger " + collected + " saved!\n\n"
                        + promptForNextPassenger(session, authToken);
            }

            return createBooking(session, authToken);

        } catch (Exception e) {
            log.warn("Passenger parse failed: {}", e.getMessage());
            return "Couldn't parse details. Please use:\n"
                    + "First Name: John\nLast Name: Doe\nAge: 30\n"
                    + "Gender: MALE\nEmail: john@example.com\nPhone: 9876543210";
        }
    }

    // =====================================================================
    // STEP 6 — Create booking
    // =====================================================================

    private String createBooking(ConversationSession session, String authToken) {
        try {
            CreateBookingRequest req = new CreateBookingRequest(
                    session.getSelectedFlight().getId(),
                    session.getCollectedPassengers());

            SingleBookingApiResponse resp = bookingClient.createBooking(authToken, req);

            if (resp == null || !resp.isSuccess() || resp.getData() == null) {
                session.setStep(ConversationStep.IDLE);
                return "Booking creation failed. " + (resp != null ? resp.getMessage() : "Please try again.");
            }

            BookingInfo booking = resp.getData();
            session.setBookingId(booking.getId());
            session.setBookingReference(booking.getBookingReference());
            session.setTotalAmount(booking.getTotalAmount());
            session.setStep(ConversationStep.AWAITING_PAYMENT_METHOD);

            return "Booking Created Successfully!\n\n"
                    + "Reference : " + booking.getBookingReference() + "\n"
                    + "Flight    : " + session.getSelectedFlight().getAirline() + " " + session.getSelectedFlight().getFlightNumber() + "\n"
                    + "Route     : " + session.getFromCity() + " -> " + session.getToCity() + "\n"
                    + "Passengers: " + session.getPassengers() + "\n"
                    + "Total     : Rs." + String.format("%.0f", booking.getTotalAmount()) + "\n\n"
                    + "Choose Payment Method:\n"
                    + "1. UPI\n2. Credit Card\n3. Debit Card\n4. Net Banking\n\nReply with your choice:";

        } catch (Exception e) {
            log.error("createBooking error: {}", e.getMessage());
            session.setStep(ConversationStep.IDLE);
            return "Booking failed: " + e.getMessage();
        }
    }

    // =====================================================================
    // STEP 7 — Payment method
    // =====================================================================

    private String handlePaymentMethod(String msg, ConversationSession session, String authToken) {
        String lower = msg.toLowerCase().trim();

        if (lower.contains("upi") || lower.equals("1")) {
            session.setPaymentMethod("UPI");
            session.setStep(ConversationStep.AWAITING_UPI_ID);
            return "Enter your UPI ID (e.g. name@upi or 9876543210@paytm):";
        }
        if (lower.contains("credit") || lower.equals("2")) {
            session.setPaymentMethod("CREDIT_CARD");
            session.setStep(ConversationStep.AWAITING_CARD_LAST_FOUR);
            return "Enter the last 4 digits of your Credit Card:";
        }
        if (lower.contains("debit") || lower.equals("3")) {
            session.setPaymentMethod("DEBIT_CARD");
            session.setStep(ConversationStep.AWAITING_CARD_LAST_FOUR);
            return "Enter the last 4 digits of your Debit Card:";
        }
        if (lower.contains("net") || lower.contains("bank") || lower.equals("4")) {
            session.setPaymentMethod("NET_BANKING");
            return processPayment(session, authToken, null, null);
        }

        return "Please choose:\n1. UPI\n2. Credit Card\n3. Debit Card\n4. Net Banking";
    }

    // =====================================================================
    // STEP 8a — UPI ID
    // =====================================================================

    private String handleUpiId(String msg, ConversationSession session, String authToken) {
        if (!msg.trim().contains("@")) {
            return "Invalid UPI ID. Must contain '@'. Example: name@upi";
        }
        return processPayment(session, authToken, null, msg.trim());
    }

    // =====================================================================
    // STEP 8b — Card last 4
    // =====================================================================

    private String handleCardLastFour(String msg, ConversationSession session, String authToken) {
        String digits = msg.trim().replaceAll("[^0-9]", "");
        if (digits.length() != 4) return "Please enter exactly 4 digits of your card:";
        return processPayment(session, authToken, digits, null);
    }

    // =====================================================================
    // STEP 9 — Process payment
    // =====================================================================

    private String processPayment(ConversationSession session, String authToken,
                                   String cardLastFour, String upiId) {
        try {
            PaymentRequest req = new PaymentRequest();
            req.setBookingId(session.getBookingId());
            req.setBookingReference(session.getBookingReference());
            req.setTotalAmount(session.getTotalAmount());
            req.setPaymentMethod(session.getPaymentMethod());
            req.setCardLastFour(cardLastFour);
            req.setUpiId(upiId);
            req.setSimulateFailure(false);

            PaymentApiResponse resp = paymentClient.initiatePayment(authToken, req);
            session.setStep(ConversationStep.DONE);

            if (resp != null && resp.getData() != null && "SUCCESS".equals(resp.getData().getStatus())) {
                PaymentInfo pay = resp.getData();
                return "Payment Successful! Your flight is CONFIRMED!\n\n"
                        + "Booking Ref : " + session.getBookingReference() + "\n"
                        + "Payment Ref : " + pay.getPaymentReference() + "\n"
                        + "Amount Paid : Rs." + String.format("%.0f", pay.getAmount()) + "\n"
                        + "Flight      : " + session.getSelectedFlight().getAirline()
                                           + " " + session.getSelectedFlight().getFlightNumber() + "\n"
                        + "Route       : " + session.getFromCity() + " -> " + session.getToCity() + "\n"
                        + "Departs     : " + (session.getSelectedFlight().getDepartureTime() != null
                                ? session.getSelectedFlight().getDepartureTime().format(DISPLAY_FMT) : "N/A") + "\n\n"
                        + "Confirmation sent to your email. Have a great trip!";
            } else {
                return "Payment Failed.\nBooking " + session.getBookingReference()
                        + " is saved.\nType 'pay' to retry.";
            }

        } catch (Exception e) {
            log.error("Payment error: {}", e.getMessage());
            session.setStep(ConversationStep.AWAITING_PAYMENT_METHOD);
            return "Payment failed. Try a different method:\n1. UPI\n2. Credit Card\n3. Debit Card\n4. Net Banking";
        }
    }

    // =====================================================================
    // DONE — reset for next query
    // =====================================================================

    private String handleDone(String msg, ConversationSession session, String authToken) {
        session.reset();
        return handleIdle(msg, session, authToken);
    }

    // =====================================================================
    // Booking history helpers
    // =====================================================================

    private List<BookingInfo> fetchAll(String t) {
        try { BookingApiResponse r = bookingClient.getAllMyBookings(t); return (r != null && r.getData() != null) ? r.getData() : Collections.emptyList(); }
        catch (Exception e) { return Collections.emptyList(); }
    }

    private List<BookingInfo> fetchUpcoming(String t) {
        try { BookingApiResponse r = bookingClient.getUpcomingBookings(t); return (r != null && r.getData() != null) ? r.getData() : Collections.emptyList(); }
        catch (Exception e) { return Collections.emptyList(); }
    }

    private List<BookingInfo> fetchCompleted(String t) {
        try { BookingApiResponse r = bookingClient.getCompletedBookings(t); return (r != null && r.getData() != null) ? r.getData() : Collections.emptyList(); }
        catch (Exception e) { return Collections.emptyList(); }
    }

    private List<SavedPassengerDto> fetchSavedPassengers(String authToken) {
        try {
            SavedPassengerApiResponse resp = userClient.getSavedPassengers(authToken);
            return (resp != null && resp.getData() != null) ? resp.getData() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Could not fetch saved passengers: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildBookingsList(String label, List<BookingInfo> bookings) {
        if (bookings.isEmpty()) return label + "\n\nNo bookings found. Type 'book a flight' to get started!";
        StringBuilder sb = new StringBuilder(label).append(" (").append(bookings.size()).append(")\n\n");
        for (BookingInfo b : bookings) {
            sb.append("Ref: ").append(b.getBookingReference())
              .append(" | ").append(b.getAirline()).append(" ").append(b.getFlightNumber()).append("\n")
              .append(b.getDepartureCity()).append(" -> ").append(b.getDestinationCity())
              .append(" | ").append(b.getDepartureTime() != null ? b.getDepartureTime().format(DISPLAY_FMT) : "N/A")
              .append(" | Rs.").append(String.format("%.0f", b.getTotalAmount()))
              .append(" | ").append(b.getStatus()).append("\n\n");
        }
        return sb.toString();
    }

    // =====================================================================
    // Utilities
    // =====================================================================

    private String askAi(String prompt) {
        return chatClient.prompt().user(prompt).call().content();
    }

    private String jsonStr(JsonNode node, String field) {
        return (node.has(field) && !node.get(field).isNull()) ? node.get(field).asText() : null;
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        for (String p : new String[]{"yyyy-MM-dd", "dd-MM-yyyy", "dd/MM/yyyy"}) {
            try { return LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern(p)); }
            catch (Exception ignored) {}
        }
        return null;
    }
}
