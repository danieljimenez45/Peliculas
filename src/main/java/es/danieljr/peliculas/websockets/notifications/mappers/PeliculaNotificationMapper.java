package es.danieljr.peliculas.websockets.notifications.mappers;

import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import es.danieljr.peliculas.websockets.notifications.dto.PeliculaNotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class PeliculaNotificationMapper {
    public PeliculaNotificationResponse toPeliculaNotificationDto(Pelicula pelicula) {
        return new PeliculaNotificationResponse(
                pelicula.getIdPelicula(),
                pelicula.getTitulo(),
                pelicula.getGenero(),
                pelicula.getDuracion(),
                pelicula.getSinopsis(),
                pelicula.getActoresPrincipales(),
                pelicula.getActoresSecundarios(),
                pelicula.getDirector(),
                pelicula.getCreatedAt() != null ? pelicula.getCreatedAt().toString() : null,
                pelicula.getUpdatedAt() != null ? pelicula.getUpdatedAt().toString() : null
        );
    }
}









