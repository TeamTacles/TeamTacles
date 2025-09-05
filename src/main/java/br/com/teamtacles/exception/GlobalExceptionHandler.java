package br.com.teamtacles.exception;
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        logger.warn("Validation error: {}", errorMessage);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation error", errorMessage);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String typeName = Optional.ofNullable(ex.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("a valid type");
        String message = String.format("The parameter '%s' must be of type '%s'.", ex.getName(), typeName);

        logger.warn("Invalid parameter type: {}", message, ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid parameter type", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String genericErrorMessage = "Invalid JSON format. Please check your request body.";
        logger.warn("Invalid JSON format received.", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid JSON Format", genericErrorMessage);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.warn("Invalid argument provided.", ex);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Parameter Value", ex.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex) {
        logger.warn("Attempt to create an already existing username: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Username already exists", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        String genericErrorMessage = "Invalid email or password. Please try again.";
        logger.warn("Authentication failed due to bad credentials: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Authentication Failed", genericErrorMessage);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        logger.warn("Attempt to create an already existing email: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Email already exists", ex.getMessage());
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatchException(PasswordMismatchException ex) {
        logger.warn("Password Mismatch: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Password mismatch", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource Not Found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {
        logger.warn("Resource Already Exists: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Resource already exists", ex.getMessage());
    }

    @ExceptionHandler(InvalidTaskStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTaskStateException(InvalidTaskStateException ex) {
        logger.warn("Invalid Task State modification attempt: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Resource cannot be modified", ex.getMessage());
    }

    @ExceptionHandler(SameAsCurrentPasswordException.class)
    public ResponseEntity<ErrorResponse> handleSameAsCurrentPasswordException(SameAsCurrentPasswordException ex) {
        String genericErrorMessage = "The new password cannot be the same as your current password.";
        logger.warn("Attempt to set the same password as current: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid Password", genericErrorMessage);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException ex) {
        String genericErrorMessage = "Invalid or missing authentication credentials. Please log in.";
        logger.warn("Authentication credentials not found for the request.", ex);
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Unauthorized - User Not Authenticated", genericErrorMessage);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        String genericErrorMessage = "You do not have permission to access this resource.";
        logger.warn("Access denied for the request.", ex);
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access Forbidden", genericErrorMessage);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        String genericErrorMessage = "An unexpected error occurred. Please try again later.";
        logger.error("Internal Server Error: ", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", genericErrorMessage);
    }

    //Método Auxiliar para construir a resposta de erro
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String errorTitle, String errorMessage) {
        ErrorResponse errorResponse = new ErrorResponse(status.value(), errorTitle, errorMessage);
        return new ResponseEntity<>(errorResponse, status);
    }
}