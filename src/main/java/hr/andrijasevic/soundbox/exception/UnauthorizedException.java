package hr.andrijasevic.soundbox.exception;

import org.springframework.http.HttpStatus;

/** 401 — bad credentials at login. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
