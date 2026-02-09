package es.danieljr.peliculas.rest.entradas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EntradaConflictException extends EntradaException {
    public EntradaConflictException(String message) {
        super(message);
    }
}








