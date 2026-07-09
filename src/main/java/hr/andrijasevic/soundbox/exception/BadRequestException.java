package hr.andrijasevic.soundbox.exception;

import org.springframework.http.HttpStatus;

/** 400 — a well-formed request that violates a business rule (e.g. following yourself). */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
