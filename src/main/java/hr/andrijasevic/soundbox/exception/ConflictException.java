package hr.andrijasevic.soundbox.exception;

import org.springframework.http.HttpStatus;

/** 409 — the request conflicts with existing state (e.g. email/username already taken). */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
