package es.danieljr.peliculas.rest.entradas.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class EntradaUpdateDto {
    private final LocalDate fecha;

    @Positive(message = "El precio debe ser positivo")
    private final Double precio;

    @Size(max = 50, message = "El método de pago no puede exceder los 50 caracteres")
    private final String metodoPago;

    private final Long peliculaId;
}
