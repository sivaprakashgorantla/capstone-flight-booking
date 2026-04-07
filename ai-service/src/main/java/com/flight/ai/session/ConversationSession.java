package com.flight.ai.session;

import com.flight.ai.dto.FlightInfo;
import com.flight.ai.dto.PassengerRequest;
import com.flight.ai.dto.SavedPassengerDto;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ConversationSession {

    private ConversationStep step = ConversationStep.IDLE;

    // Booking intent
    private String    fromCity;
    private String    toCity;
    private int       passengers = 1;
    private LocalDate travelDate;

    // Flight search results
    private List<FlightInfo> searchResults = new ArrayList<>();
    private FlightInfo       selectedFlight;

    // Saved passengers fetched from user-service (null = not yet fetched)
    private List<SavedPassengerDto> savedPassengers = null;

    // Passengers collected for this booking
    private List<PassengerRequest> collectedPassengers = new ArrayList<>();

    // After booking created
    private Long       bookingId;
    private String     bookingReference;
    private BigDecimal totalAmount;

    // Payment
    private String paymentMethod;   // UPI / CREDIT_CARD / DEBIT_CARD / NET_BANKING

    // Auth
    private String authToken;

    // Session metadata
    private LocalDateTime lastActivity = LocalDateTime.now();

    public void reset() {
        step                = ConversationStep.IDLE;
        fromCity            = null;
        toCity              = null;
        passengers          = 1;
        travelDate          = null;
        searchResults       = new ArrayList<>();
        selectedFlight      = null;
        savedPassengers     = null;
        collectedPassengers = new ArrayList<>();
        bookingId           = null;
        bookingReference    = null;
        totalAmount         = null;
        paymentMethod       = null;
    }
}
