package com.flight.reporting.model;

public enum ReportStatus {
    GENERATED,   // Freshly computed
    CACHED       // Served from stored snapshot
}
