package br.com.teamtacles.common.exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.authentication.DisabledException;
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("Validation error processing request: {}", errorMessage, ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation error", errorMessage);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String typeName = Optional.ofNullable(ex.getRequiredType()).map(Class::getSimpleName).orElse("a valid type");
        String message = String.format("The parameter '%s' must be of type '%s'.", ex.getName(), typeName);
        log.error("Invalid parameter type for parameter '{}'. Expected: {}.", ex.getName(), typeName, ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid parameter type", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String genericErrorMessage = "Invalid JSON format. Please check your request body.";
        log.error("Invalid JSON format received. {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid JSON Format", genericErrorMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Invalid argument provided: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Parameter Value", ex.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex) {
        log.error("Attempt to create an already existing username: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Username already exists",
                ex.getMessage(),
                "USERNAME_ALREADY_EXISTS"
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        String genericErrorMessage = "Invalid email or password. Please try again.";
        log.error("Authentication failed due to bad credentials.", ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Authentication Failed",
                genericErrorMessage,
                "INVALID_CREDENTIALS"
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        log.error("Attempt to create an already existing email: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Email already exists",
                ex.getMessage(),
                "EMAIL_ALREADY_EXISTS"
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatchException(PasswordMismatchException ex) {
        log.error("Password Mismatch: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Password mismatch",
                ex.getMessage(),
                "PASSWORD_MISMATCH"
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("Resource Not Found: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found",
                ex.getMessage(),
                "RESOURCE_NOT_FOUND"
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {
        log.error("Resource Already Exists: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.CONFLICT, "Resource already exists", ex.getMessage());
    }

    @ExceptionHandler(InvalidTaskStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTaskStateException(InvalidTaskStateException ex) {
        log.error("Invalid Task State modification attempt: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.CONFLICT, "Resource cannot be modified", ex.getMessage());
    }

    @ExceptionHandler(SameAsCurrentPasswordException.class)
    public ResponseEntity<ErrorResponse> handleSameAsCurrentPasswordException(SameAsCurrentPasswordException ex) {
        String genericErrorMessage = "The new password cannot be the same as your current password.";
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Password",
                genericErrorMessage,
                "SAME_AS_CURRENT_PASSWORD"
        );
        log.error("Attempt to set the same password as current.", ex);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException ex) {
        String genericErrorMessage = "Invalid or missing authentication credentials. Please log in.";
        log.error("Authentication credentials not found for the request.", ex);
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized - User Not Authenticated", genericErrorMessage);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        String genericErrorMessage = "You do not have permission to access this resource.";
        String errorMessage = (ex.getMessage() != null && !ex.getMessage().isEmpty())
                                    ? ex.getMessage()
                                    : genericErrorMessage;

        log.error("Access denied for the request.", ex);
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access Forbidden", errorMessage);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        String genericErrorMessage = "An unexpected error occurred. Please try again later.";
        log.error("Internal Server Error: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", genericErrorMessage);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(DisabledException ex) {
        String friendlyErrorMessage = "Your account is not verified. Please check your email for the verification link.";
        log.error("Login attempt with disabled account: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Account Not Verified",
                friendlyErrorMessage,
                "ACCOUNT_NOT_VERIFIED"
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String errorTitle, String errorMessage) {
        ErrorResponse errorResponse = new ErrorResponse(status.value(), errorTitle, errorMessage);
        return new ResponseEntity<>(errorResponse, status);
    }
}