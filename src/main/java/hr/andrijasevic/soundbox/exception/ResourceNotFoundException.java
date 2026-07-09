package hr.andrijasevic.soundbox.exception;

import org.springframework.http.HttpStatus;

/** 404 — a requested user, album, review or list does not exist. */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
