package br.com.teamtacles.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Schema(name = "ErrorResponse", description = "Standard format for API error responses")
public class ErrorResponse {

    @Schema(description = "Timestamp when the error occurred.")
    private LocalDateTime dateTime;

    @Schema(description = "HTTP status code.")
    private int status;

    @Schema(description = "A short, human-readable summary of the problem type.")
    private String errorTitle;

    @Schema(description = "A detailed error message.")
    private String errorMessage;

    @Schema(description = "A machine-readable error code for specific error handling.")
    private String errorCode;

    public ErrorResponse(int status, String errorTitle, String errorMessage, String errorCode) {
        this.dateTime = LocalDateTime.now();
        this.status = status;
        this.errorTitle = errorTitle;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public ErrorResponse(int status, String errorTitle, String errorMessage) {
        this(status, errorTitle, errorMessage, null);
    }
}