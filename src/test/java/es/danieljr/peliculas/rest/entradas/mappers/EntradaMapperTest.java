package es.danieljr.peliculas.rest.entradas.mappers;

import es.danieljr.peliculas.rest.entradas.dto.EntradaCreateDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaUpdateDto;
import es.danieljr.peliculas.rest.entradas.models.Entrada;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntradaMapperTest {
  private final Pelicula pelicula = Pelicula.builder()
      .idPelicula(1L)
      .titulo("El Padrino")
      .genero("Drama")
      .duracion(175)
      .director("Francis Ford Coppola")
      .build();

  // Inyectamos el mapper
  private final EntradaMapper entradaMapper = new EntradaMapper();

  @Test
  void toEntrada_create() {
    // Arrange
    EntradaCreateDto entradaCreateDto = EntradaCreateDto.builder()
        .fecha(LocalDate.of(2025, 12, 15))
        .precio(10.0)
        .metodoPago("Tarjeta")
        .peliculaId(1L)
        .build();
    // Act
    var res = entradaMapper.toEntrada(entradaCreateDto);

    // Assert
    assertAll(
        () -> assertEquals(entradaCreateDto.getFecha(), res.getFecha()),
        () -> assertEquals(entradaCreateDto.getPrecio(), res.getPrecio()),
        () -> assertEquals(entradaCreateDto.getMetodoPago(), res.getMetodoPago())
    );
  }

  @Test
  void toEntrada_update() {
    // Arrange
    Long id = 1L;
    EntradaUpdateDto entradaUpdateDto = EntradaUpdateDto.builder()
        .fecha(LocalDate.of(2025, 12, 20))
        .precio(15.0)
        .metodoPago("Efectivo")
        .build();

    Entrada entrada = Entrada.builder()
        .idEntrada(id)
        .fecha(LocalDate.of(2025, 12, 15))
        .precio(10.0)
        .metodoPago("Tarjeta")
        .pelicula(pelicula)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    // Act
    var res = entradaMapper.toEntrada(entradaUpdateDto, entrada);
    // Assert
    assertAll(
        () -> assertEquals(id, res.getIdEntrada()),
        () -> assertEquals(entradaUpdateDto.getFecha(), res.getFecha()),
        () -> assertEquals(entradaUpdateDto.getPrecio(), res.getPrecio()),
        () -> assertEquals(entradaUpdateDto.getMetodoPago(), res.getMetodoPago())
    );
  }

  @Test
  void toEntradaResponseDto() {
    // Arrange
    Entrada entrada = Entrada.builder()
        .idEntrada(1L)
        .fecha(LocalDate.of(2025, 12, 15))
        .precio(10.0)
        .metodoPago("Tarjeta")
        .pelicula(pelicula)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    // Act
    var res = entradaMapper.toEntradaResponseDto(entrada);
    // Assert
    assertAll(
        () -> assertEquals(entrada.getIdEntrada(), res.getIdEntrada()),
        () -> assertEquals(entrada.getFecha(), res.getFecha()),
        () -> assertEquals(entrada.getPrecio(), res.getPrecio()),
        () -> assertEquals(entrada.getMetodoPago(), res.getMetodoPago()),
        () -> assertEquals(entrada.getPelicula().getIdPelicula(), res.getPeliculaId())
    );
  }
}








