package com.flight.support_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Admin request to resolve a support ticket")
public class ResolveTicketRequest {

    @NotBlank(message = "Resolution message is required")
    @Size(min = 10, max = 2000, message = "Resolution must be between 10 and 2000 characters")
    @Schema(
        description = "Resolution details explaining how the issue was resolved",
        example = "Your booking issue has been resolved. The refund of $150 has been processed to your original payment method and should reflect within 5-7 business days."
    )
    private String resolution;
}
