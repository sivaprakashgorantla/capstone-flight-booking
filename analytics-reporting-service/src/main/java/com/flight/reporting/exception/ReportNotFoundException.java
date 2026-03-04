package com.flight.reporting.exception;

public class ReportNotFoundException extends RuntimeException {
    public ReportNotFoundException(String msg) { super(msg); }
    public ReportNotFoundException(Long id)    { super("Report record not found with ID: " + id); }
}
