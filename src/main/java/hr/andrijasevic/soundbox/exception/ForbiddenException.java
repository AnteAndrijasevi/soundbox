package hr.andrijasevic.soundbox.exception;

import org.springframework.http.HttpStatus;

/** 403 — authenticated, but not allowed to act on this resource (e.g. not the owner). */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
