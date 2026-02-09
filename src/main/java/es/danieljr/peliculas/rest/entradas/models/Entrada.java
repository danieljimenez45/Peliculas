package es.danieljr.peliculas.rest.entradas.models;

import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import es.danieljr.peliculas.rest.users.models.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "entradas")
@Builder
@ToString(exclude = {"usuario", "pelicula"})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor // JPA necesita un constructor vacío
public class Entrada {

    @Id // Indicamos que es el ID de la tabla
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Indicamos que es autoincremental
    @Column(name = "id_entrada")
    private Long idEntrada;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Double precio;

    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;

    // Relación con película, muchas entradas pueden pertenecer a una película
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pelicula_id", nullable = false) // Así se va a llamar en la BD
    private Pelicula pelicula;

    // Relación con usuario, muchas entradas pueden pertenecer a un usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id") // Así se va a llamar en la BD (nullable para permitir entradas sin usuario asignado)
    private User usuario;

    @Builder.Default
    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

