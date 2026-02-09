package es.danieljr.peliculas.websockets.notifications.dto;

public record EntradaNotificationResponse(
    Long idEntrada,
    String fecha,
    Double precio,
    String metodoPago,
    Long peliculaId,
    String createdAt,
    String updatedAt
) {
}








