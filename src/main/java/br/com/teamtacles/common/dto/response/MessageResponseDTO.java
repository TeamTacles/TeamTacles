package br.com.teamtacles.common.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MessageResponseDTO", description = "DTO for authentication response containing the JWT token.")
public class MessageResponseDTO {

    @Schema(description = "Response message", example = "Operation completed successfully.")
    private String message;
}
