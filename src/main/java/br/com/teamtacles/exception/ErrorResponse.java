package br.com.teamtacles.exception;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private LocalDateTime dateTime;
    private int status;
    private String errorTitle;
    private String errorMessage;

    public ErrorResponse(int status, String errorTitle, String errorMessage) {
        this.dateTime = LocalDateTime.now();
        this.status = status;
        this.errorTitle = errorTitle;
        this.errorMessage = errorMessage;
    }
}
