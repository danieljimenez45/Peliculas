package es.danieljr.peliculas.rest.entradas.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EntradaBadRequestException extends EntradaException {
  public EntradaBadRequestException(String message) {
    super(message);
  }
}








