package es.danieljr.peliculas.rest.peliculas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "Película a crear")
public class PeliculaCreateDto {
    @NotBlank(message = "El título es obligatorio")
    @Size(min = 1, max = 200, message = "El título debe tener entre 1 y 200 caracteres")
    @Schema(description = "Título de la película", example = "El señor de los anillos")
    private final String titulo;

    @NotBlank(message = "El género es obligatorio")
    @Size(max = 50, message = "El género no puede exceder los 50 caracteres")
    @Schema(description = "Género de la película", example = "Fantasía")
    private final String genero;

    @NotNull(message = "La duración es obligatoria")
    @Positive(message = "La duración debe ser positiva")
    @Schema(description = "Duración en minutos", example = "178")
    private final Integer duracion;

    @Schema(description = "Sinopsis de la película")
    private final String sinopsis;

    @Schema(description = "Actores principales")
    private final String actoresPrincipales;

    @Schema(description = "Actores secundarios")
    private final String actoresSecundarios;

    @NotBlank(message = "El director es obligatorio")
    @Size(max = 100, message = "El director no puede exceder los 100 caracteres")
    @Schema(description = "Director de la película", example = "Peter Jackson")
    private final String director;
}
