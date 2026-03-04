package user_service.user_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Whether the operation succeeded")
    private boolean success;

    @Schema(description = "Human-readable message")
    private String message;

    @Schema(description = "Response payload (null on errors)")
    private T data;

    @Builder.Default
    @Schema(description = "Timestamp of the response")
    private LocalDateTime timestamp = LocalDateTime.now();

    // ─── Factory helpers ───────────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
