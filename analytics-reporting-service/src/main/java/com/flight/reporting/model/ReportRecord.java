package com.flight.reporting.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Audit log of every report generated.
 * Tracks who ran which report, when, and over what date range.
 */
@Entity
@Table(name = "report_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique reference e.g. RPT-A1B2C3D4 */
    @Column(name = "report_reference", unique = true, nullable = false)
    private String reportReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Column(nullable = false)
    private String title;

    /** Admin userId who triggered the report */
    @Column(name = "generated_by")
    private String generatedBy;

    /** Optional filter — from date (ISO string stored for simplicity) */
    @Column(name = "from_date")
    private String fromDate;

    /** Optional filter — to date */
    @Column(name = "to_date")
    private String toDate;

    /** Number of primary records in the report result */
    @Column(name = "record_count")
    private int recordCount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.GENERATED;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    void prePersist() {
        if (generatedAt == null) generatedAt = LocalDateTime.now();
    }
}
