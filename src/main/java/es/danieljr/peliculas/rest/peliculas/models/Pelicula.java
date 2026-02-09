package es.danieljr.peliculas.rest.peliculas.models;

import es.danieljr.peliculas.rest.entradas.models.Entrada;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "peliculas")
@Builder
@ToString(exclude = { "entradas" })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "Peliculas")
public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pelicula")
    @Schema(description = "Identificador de la película", example = "1")
    private Long idPelicula;

    @Column(nullable = false)
    @Schema(description = "Título de la película", example = "El señor de los anillos")
    private String titulo;

    @Column(nullable = false)
    @Schema(description = "Género", example = "Fantasía")
    private String genero;

    @Column(nullable = false)
    @Schema(description = "Duración en minutos", example = "178")
    private Integer duracion;

    @Schema(description = "Sinopsis")
    private String sinopsis;

    @Column(name = "actores_principales")
    @Schema(description = "Actores principales")
    private String actoresPrincipales;

    @Column(name = "actores_secundarios")
    @Schema(description = "Actores secundarios")
    private String actoresSecundarios;

    @Column(nullable = false)
    @Schema(description = "Director", example = "Peter Jackson")
    private String director;

    @OneToMany(mappedBy = "pelicula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Entrada> entradas = new ArrayList<>();

    @Builder.Default
    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    @Schema(description = "Fecha de actualización")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
