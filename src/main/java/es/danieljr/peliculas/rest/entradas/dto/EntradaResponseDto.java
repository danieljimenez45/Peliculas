package es.danieljr.peliculas.rest.entradas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntradaResponseDto {
    private Long idEntrada;
    private LocalDate fecha;
    private Double precio;
    private String metodoPago;
    private Long peliculaId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}








