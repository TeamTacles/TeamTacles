package br.com.teamtacles.common.exception;

public class SameAsCurrentPasswordException extends RuntimeException {

    public SameAsCurrentPasswordException(String message) {
        super(message);
    }
}