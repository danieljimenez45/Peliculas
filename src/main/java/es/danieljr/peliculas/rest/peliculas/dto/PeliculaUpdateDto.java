package es.danieljr.peliculas.rest.peliculas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "Película a actualizar")
public class PeliculaUpdateDto {
    @Size(min = 1, max = 200, message = "El título debe tener entre 1 y 200 caracteres")
    @Schema(description = "Título de la película", example = "El señor de los anillos")
    private final String titulo;

    @Size(max = 50, message = "El género no puede exceder los 50 caracteres")
    @Schema(description = "Género", example = "Fantasía")
    private final String genero;

    @Positive(message = "La duración debe ser positiva")
    @Schema(description = "Duración en minutos", example = "178")
    private final Integer duracion;

    @Schema(description = "Sinopsis")
    private final String sinopsis;

    @Schema(description = "Actores principales")
    private final String actoresPrincipales;

    @Schema(description = "Actores secundarios")
    private final String actoresSecundarios;

    @Size(max = 100, message = "El director no puede exceder los 100 caracteres")
    @Schema(description = "Director", example = "Peter Jackson")
    private final String director;
}
