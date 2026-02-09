package es.danieljr.peliculas.rest.entradas.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class EntradaCreateDto {
    @NotNull(message = "La fecha es obligatoria")
    private final LocalDate fecha;

    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser positivo")
    private final Double precio;

    @NotBlank(message = "El método de pago es obligatorio")
    @Size(max = 50, message = "El método de pago no puede exceder los 50 caracteres")
    private final String metodoPago;

    @NotNull(message = "El ID de la película es obligatorio")
    private final Long peliculaId;
}









