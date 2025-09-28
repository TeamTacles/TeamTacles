package br.com.teamtacles.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ErrorResponse", description = "Standard format for API error responses")
public class ErrorResponse {

    @Schema(description = "Timestamp when the error occurred.", example = "2023-10-27T10:15:30")
    private LocalDateTime dateTime;

    @Schema(description = "HTTP status code.", example = "400")
    private int status;

    @Schema(description = "A short, human-readable summary of the problem type.", example = "Bad Request")
    private String errorTitle;

    @Schema(description = "A detailed error message.", example = "Passwords do not match.")
    private String errorMessage;

    public ErrorResponse(int status, String errorTitle, String errorMessage) {
        this.dateTime = LocalDateTime.now();
        this.status = status;
        this.errorTitle = errorTitle;
        this.errorMessage = errorMessage;
    }
}
