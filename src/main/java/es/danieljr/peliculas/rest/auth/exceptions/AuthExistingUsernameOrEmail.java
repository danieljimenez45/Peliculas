package es.danieljr.peliculas.rest.auth.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AuthExistingUsernameOrEmail extends RuntimeException {
    public AuthExistingUsernameOrEmail(String message) {
        super(message);
    }
}
