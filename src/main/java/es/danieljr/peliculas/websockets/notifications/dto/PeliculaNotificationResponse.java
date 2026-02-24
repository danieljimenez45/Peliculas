package es.danieljr.peliculas.websockets.notifications.dto;

public record
PeliculaNotificationResponse(
        Long idPelicula,
        String titulo,
        String genero,
        Integer duracion,
        String sinopsis,
        String actoresPrincipales,
        String actoresSecundarios,
        String director,
        String createdAt,
        String updatedAt
) {
}









