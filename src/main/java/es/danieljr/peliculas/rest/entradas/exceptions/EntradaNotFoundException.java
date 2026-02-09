package es.danieljr.peliculas.rest.entradas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción de entrada no encontrada
 * Status 404
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntradaNotFoundException extends EntradaException {
    public EntradaNotFoundException(Long id) {
        super("Entrada con id " + id + " no encontrada");
    }
}









