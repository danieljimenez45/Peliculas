package es.danieljr.peliculas.rest.peliculas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción de película no encontrada
 * Status 404
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PeliculaNotFoundException extends PeliculaException {
  public PeliculaNotFoundException(Long id) {
    super("Película con id " + id + " no encontrada");
  }
}









