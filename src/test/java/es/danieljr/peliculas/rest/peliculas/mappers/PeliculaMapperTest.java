package es.danieljr.peliculas.rest.peliculas.mappers;

import es.danieljr.peliculas.rest.peliculas.dto.PeliculaCreateDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaUpdateDto;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del mapper de Películas (conversión entre entidad y DTOs).
 * No usa Spring ni mocks: se prueba el mapper de forma aislada.
 */
class PeliculaMapperTest {

  /** Mapper a probar; instancia real para verificar que los campos se copian bien. */
  private final PeliculaMapper peliculaMapper = new PeliculaMapper();

  /** Comprueba que toPelicula(CreateDto) crea una entidad con los datos del DTO (sin id). */
  @Test
  void toPelicula_create() {
    // Arrange
    PeliculaCreateDto peliculaCreateDto = PeliculaCreateDto.builder()
        .titulo("El Padrino")
        .genero("Drama")
        .duracion(175)
        .sinopsis("La historia de una familia de la mafia")
        .actoresPrincipales("Marlon Brando, Al Pacino")
        .actoresSecundarios("James Caan, Robert Duvall")
        .director("Francis Ford Coppola")
        .build();
    // Act
    var res = peliculaMapper.toPelicula(peliculaCreateDto);

    // Assert
    assertAll(
        () -> assertEquals(peliculaCreateDto.getTitulo(), res.getTitulo()),
        () -> assertEquals(peliculaCreateDto.getGenero(), res.getGenero()),
        () -> assertEquals(peliculaCreateDto.getDuracion(), res.getDuracion()),
        () -> assertEquals(peliculaCreateDto.getSinopsis(), res.getSinopsis()),
        () -> assertEquals(peliculaCreateDto.getActoresPrincipales(), res.getActoresPrincipales()),
        () -> assertEquals(peliculaCreateDto.getActoresSecundarios(), res.getActoresSecundarios()),
        () -> assertEquals(peliculaCreateDto.getDirector(), res.getDirector())
    );
  }

  /** Comprueba que toPelicula(UpdateDto, entidad) actualiza solo los campos del DTO y mantiene el id. */
  @Test
  void toPelicula_update() {
    // Arrange
    Long id = 1L;
    PeliculaUpdateDto peliculaUpdateDto = PeliculaUpdateDto.builder()
        .titulo("El Padrino: Parte II")
        .genero("Drama")
        .duracion(200)
        .build();

    Pelicula pelicula = Pelicula.builder()
        .idPelicula(id)
        .titulo("El Padrino")
        .genero("Drama")
        .duracion(175)
        .sinopsis("La historia de una familia de la mafia")
        .actoresPrincipales("Marlon Brando, Al Pacino")
        .actoresSecundarios("James Caan, Robert Duvall")
        .director("Francis Ford Coppola")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    // Act
    var res = peliculaMapper.toPelicula(peliculaUpdateDto, pelicula);
    // Assert
    assertAll(
        () -> assertEquals(id, res.getIdPelicula()),
        () -> assertEquals(peliculaUpdateDto.getTitulo(), res.getTitulo()),
        () -> assertEquals(peliculaUpdateDto.getGenero(), res.getGenero()),
        () -> assertEquals(peliculaUpdateDto.getDuracion(), res.getDuracion())
    );
  }

  /** Comprueba que toPeliculaResponseDto(entidad) devuelve un DTO con todos los campos de la entidad. */
  @Test
  void toPeliculaResponseDto() {
    // Arrange
    Pelicula pelicula = Pelicula.builder()
        .idPelicula(1L)
        .titulo("El Padrino")
        .genero("Drama")
        .duracion(175)
        .sinopsis("La historia de una familia de la mafia")
        .actoresPrincipales("Marlon Brando, Al Pacino")
        .actoresSecundarios("James Caan, Robert Duvall")
        .director("Francis Ford Coppola")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    // Act
    var res = peliculaMapper.toPeliculaResponseDto(pelicula);
    // Assert
    assertAll(
        () -> assertEquals(pelicula.getIdPelicula(), res.getIdPelicula()),
        () -> assertEquals(pelicula.getTitulo(), res.getTitulo()),
        () -> assertEquals(pelicula.getGenero(), res.getGenero()),
        () -> assertEquals(pelicula.getDuracion(), res.getDuracion()),
        () -> assertEquals(pelicula.getSinopsis(), res.getSinopsis()),
        () -> assertEquals(pelicula.getActoresPrincipales(), res.getActoresPrincipales()),
        () -> assertEquals(pelicula.getActoresSecundarios(), res.getActoresSecundarios()),
        () -> assertEquals(pelicula.getDirector(), res.getDirector())
    );
  }
}








