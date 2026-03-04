package com.flight.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Wraps CSV content for the download endpoint (UC10 Step 4 — Admin downloads report).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CsvDownloadResponse {
    private String filename;
    private String reportType;
    private String generatedAt;
    private int rowCount;
    private String csvContent;   // full CSV as a string (client saves to file)
}
