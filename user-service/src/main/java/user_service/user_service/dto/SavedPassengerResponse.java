package user_service.user_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Saved passenger profile")
public class SavedPassengerResponse {

    private Long id;

    @Schema(description = "Friendly nickname", example = "Myself")
    private String label;

    private String firstName;
    private String lastName;
    private int age;
    private String gender;
    private String email;
    private String phone;
    private String passportNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
