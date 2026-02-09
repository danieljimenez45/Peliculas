package es.danieljr.peliculas.rest.peliculas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PeliculaBadRequestException extends PeliculaException {
  public PeliculaBadRequestException(String message) {
    super(message);
  }
}








