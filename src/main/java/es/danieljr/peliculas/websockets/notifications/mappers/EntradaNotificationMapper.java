package es.danieljr.peliculas.websockets.notifications.mappers;

import es.danieljr.peliculas.rest.entradas.models.Entrada;
import es.danieljr.peliculas.websockets.notifications.dto.EntradaNotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class EntradaNotificationMapper {
  public EntradaNotificationResponse toEntradaNotificationDto(Entrada entrada) {
    return new EntradaNotificationResponse(
        entrada.getIdEntrada(),
        entrada.getFecha().toString(),
        entrada.getPrecio(),
        entrada.getMetodoPago(),
        entrada.getPelicula().getIdPelicula(),
        entrada.getCreatedAt() != null ? entrada.getCreatedAt().toString() : null,
        entrada.getUpdatedAt() != null ? entrada.getUpdatedAt().toString() : null
    );
  }
}








