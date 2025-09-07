package br.com.teamtacles.common.exception;



public class SameAsCurrentPasswordException extends RuntimeException {

    public SameAsCurrentPasswordException(String message) {
        super(message);
    }

    public SameAsCurrentPasswordException(String message, Throwable cause) {

        super(message, cause);
    }
}