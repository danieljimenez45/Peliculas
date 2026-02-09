package es.danieljr.peliculas.rest.peliculas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Película a devolver como respuesta")
public class PeliculaResponseDto {
    @Schema(description = "Identificador de la película", example = "1")
    private Long idPelicula;
    @Schema(description = "Título de la película", example = "El señor de los anillos")
    private String titulo;
    @Schema(description = "Género", example = "Fantasía")
    private String genero;
    @Schema(description = "Duración en minutos", example = "178")
    private Integer duracion;
    @Schema(description = "Sinopsis")
    private String sinopsis;
    @Schema(description = "Actores principales")
    private String actoresPrincipales;
    @Schema(description = "Actores secundarios")
    private String actoresSecundarios;
    @Schema(description = "Director", example = "Peter Jackson")
    private String director;
    @Schema(description = "Ids de entradas asociadas")
    private List<Long> entradaIds;
    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;
    @Schema(description = "Fecha de actualización")
    private LocalDateTime updatedAt;
}
