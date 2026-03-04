package com.flight.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportRecordResponse {
    private Long id;
    private String reportReference;
    private String reportType;
    private String title;
    private String generatedBy;
    private String fromDate;
    private String toDate;
    private int recordCount;
    private String status;
    private LocalDateTime generatedAt;
}
