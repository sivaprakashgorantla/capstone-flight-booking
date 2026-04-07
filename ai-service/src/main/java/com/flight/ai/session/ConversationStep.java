package com.flight.ai.session;

public enum ConversationStep {
    IDLE,
    AWAITING_DATE,
    AWAITING_FLIGHT_SELECTION,
    AWAITING_SAVED_PASSENGER_CHOICE,  // pick from saved list or enter manually
    AWAITING_PASSENGER_DETAILS,
    AWAITING_PAYMENT_METHOD,
    AWAITING_UPI_ID,
    AWAITING_CARD_LAST_FOUR,
    DONE
}
