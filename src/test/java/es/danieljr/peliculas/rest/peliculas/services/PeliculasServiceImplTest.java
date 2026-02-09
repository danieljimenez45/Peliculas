package es.danieljr.peliculas.rest.peliculas.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.danieljr.peliculas.config.websockets.WebSocketConfig;
import es.danieljr.peliculas.config.websockets.WebSocketHandler;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaCreateDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaUpdateDto;
import es.danieljr.peliculas.rest.peliculas.exceptions.PeliculaNotFoundException;
import es.danieljr.peliculas.rest.peliculas.mappers.PeliculaMapper;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import es.danieljr.peliculas.rest.peliculas.repositories.PeliculasRepository;
import es.danieljr.peliculas.websockets.notifications.mappers.PeliculaNotificationMapper;
import es.danieljr.peliculas.websockets.notifications.models.Notificacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeliculasServiceImplTest {
  private final Pelicula pelicula1 = Pelicula.builder()
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

  private final Pelicula pelicula2 = Pelicula.builder()
      .idPelicula(2L)
      .titulo("Matrix")
      .genero("Ciencia Ficción")
      .duracion(136)
      .sinopsis("Un programador descubre la verdad sobre la realidad")
      .actoresPrincipales("Keanu Reeves, Laurence Fishburne")
      .actoresSecundarios("Carrie-Anne Moss, Hugo Weaving")
      .director("Lana Wachowski, Lilly Wachowski")
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();

  private PeliculaResponseDto peliculaResponse1;

  @Mock
  private PeliculasRepository peliculasRepository;
  @Spy
  private PeliculaMapper peliculaMapper;
  @Mock
  private WebSocketConfig webSocketConfig;
  @Mock
  private PeliculaNotificationMapper peliculaNotificationMapper;
  @Mock
  private ObjectMapper objectMapper;
  @Mock
  private WebSocketHandler webSocketService;

  @InjectMocks
  private PeliculasServiceImpl peliculasService;
  @Captor
  private ArgumentCaptor<Pelicula> peliculaCaptor;

  @BeforeEach
  void setUp() {
    peliculaResponse1 = peliculaMapper.toPeliculaResponseDto(pelicula1);
    peliculasService.setWebSocketService(webSocketService);
  }

  @Test
  void findAll_ShouldReturnAllPeliculas_WhenNoParametersProvided() {
    // Arrange
    List<Pelicula> expectedPeliculas = List.of(pelicula1, pelicula2);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());
    Page<Pelicula> expectedPage = new PageImpl<>(expectedPeliculas);
    when(peliculasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    Page<PeliculaResponseDto> actualPage =
        peliculasService.findAll(Optional.empty(), Optional.empty(), pageable);

    // Assert
    assertAll("findAll",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );

    // Verify
    verify(peliculasRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void findAll_ShouldReturnPeliculasByTitulo_WhenTituloParameterProvided() {
    // Arrange
    Optional<String> titulo = Optional.of("Padrino");
    List<Pelicula> expectedPeliculas = List.of(pelicula1);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());
    Page<Pelicula> expectedPage = new PageImpl<>(expectedPeliculas);
    when(peliculasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    Page<PeliculaResponseDto> actualPage =
        peliculasService.findAll(titulo, Optional.empty(), pageable);

    // Assert
    assertAll("findAll",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );

    // Verify
    verify(peliculasRepository, only()).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void findAll_ShouldReturnPeliculasByGenero_WhenGeneroParameterProvided() {
    // Arrange
    Optional<String> genero = Optional.of("Drama");
    List<Pelicula> expectedPeliculas = List.of(pelicula1);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());
    Page<Pelicula> expectedPage = new PageImpl<>(expectedPeliculas);
    when(peliculasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    Page<PeliculaResponseDto> actualPage =
        peliculasService.findAll(Optional.empty(), genero, pageable);

    // Assert
    assertAll("findAll",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );

    // Verify
    verify(peliculasRepository, only()).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void findAll_ShouldReturnPeliculasByTituloAndGenero_WhenBothParametersProvided() {
    // Arrange
    Optional<String> titulo = Optional.of("Padrino");
    Optional<String> genero = Optional.of("Drama");
    List<Pelicula> expectedPeliculas = List.of(pelicula1);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());
    Page<Pelicula> expectedPage = new PageImpl<>(expectedPeliculas);
    when(peliculasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    Page<PeliculaResponseDto> actualPage =
        peliculasService.findAll(titulo, genero, pageable);

    // Assert
    assertAll("findAll",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );
    // Verify
    verify(peliculasRepository, only()).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void findAll_ShouldReturnPeliculasOrderedAscending_WhenSortByAscending() {
    // Arrange
    List<Pelicula> expectedPeliculas = List.of(pelicula1, pelicula2);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("titulo").ascending());
    Page<Pelicula> expectedPage = new PageImpl<>(expectedPeliculas);
    when(peliculasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    Page<PeliculaResponseDto> actualPage =
        peliculasService.findAll(Optional.empty(), Optional.empty(), pageable);

    // Assert
    assertAll("findAll with ascending sort",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );

    // Verify
    verify(peliculasRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void findAll_ShouldReturnPeliculasOrderedDescending_WhenSortByDescending() {
    // Arrange
    List<Pelicula> expectedPeliculas = List.of(pelicula2, pelicula1);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("titulo").descending());
    Page<Pelicula> expectedPage = new PageImpl<>(expectedPeliculas);
    when(peliculasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    Page<PeliculaResponseDto> actualPage =
        peliculasService.findAll(Optional.empty(), Optional.empty(), pageable);

    // Assert
    assertAll("findAll with descending sort",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );

    // Verify
    verify(peliculasRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void findById_ShouldReturnPelicula_WhenValidIdProvided() {
    // Arrange
    Long id = 1L;
    PeliculaResponseDto expectedPeliculaResponse = peliculaResponse1;
    when(peliculasRepository.findById(id)).thenReturn(Optional.of(pelicula1));

    // Act
    PeliculaResponseDto actualPeliculaResponse = peliculasService.findById(id);

    // Assert
    assertEquals(expectedPeliculaResponse, actualPeliculaResponse);

    // Verify
    verify(peliculasRepository, only()).findById(id);
  }

  @Test
  void findById_ShouldThrowPeliculaNotFound_WhenInvalidIdProvided() {
    // Arrange
    Long id = 1L;
    when(peliculasRepository.findById(id)).thenReturn(Optional.empty());

    // Act & Assert
    var res = assertThrows(PeliculaNotFoundException.class, () -> peliculasService.findById(id));
    assertEquals("Película con id " + id + " no encontrada", res.getMessage());

    // Verify
    verify(peliculasRepository).findById(id);
  }

  @Test
  void save_ShouldReturnSavedPelicula_WhenValidPeliculaCreateDtoProvided() throws IOException {
    // Arrange
    PeliculaCreateDto peliculaCreateDto = PeliculaCreateDto.builder()
        .titulo("Inception")
        .genero("Ciencia Ficción")
        .duracion(148)
        .sinopsis("Un ladrón que roba secretos del subconsciente")
        .actoresPrincipales("Leonardo DiCaprio")
        .actoresSecundarios("Marion Cotillard, Tom Hardy")
        .director("Christopher Nolan")
        .build();
    Pelicula expectedPelicula = Pelicula.builder()
        .idPelicula(1L)
        .titulo("Inception")
        .genero("Ciencia Ficción")
        .duracion(148)
        .sinopsis("Un ladrón que roba secretos del subconsciente")
        .actoresPrincipales("Leonardo DiCaprio")
        .actoresSecundarios("Marion Cotillard, Tom Hardy")
        .director("Christopher Nolan")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    PeliculaResponseDto expectedPeliculaResponse = peliculaMapper.toPeliculaResponseDto(expectedPelicula);
    when(peliculasRepository.save(any(Pelicula.class))).thenReturn(expectedPelicula);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    PeliculaResponseDto actualPeliculaResponse = peliculasService.save(peliculaCreateDto);

    // Assert
    assertEquals(expectedPeliculaResponse, actualPeliculaResponse);

    // Verify
    verify(peliculasRepository).save(peliculaCaptor.capture());
    verify(webSocketService, atLeastOnce()).sendMessage(anyString());

    Pelicula peliculaCaptured = peliculaCaptor.getValue();
    assertEquals(expectedPelicula.getTitulo(), peliculaCaptured.getTitulo());
  }

  @Test
  void update_ShouldReturnUpdatedPelicula_WhenValidIdAndPeliculaUpdateDtoProvided() throws IOException {
    // Arrange
    Long id = 1L;
    String nuevoTitulo = "El Padrino: Parte II";
    when(peliculasRepository.findById(id)).thenReturn(Optional.of(pelicula1));

    PeliculaUpdateDto peliculaUpdateDto = PeliculaUpdateDto.builder()
        .titulo(nuevoTitulo)
        .build();
    Pelicula peliculaUpdate = peliculaMapper.toPelicula(peliculaUpdateDto, pelicula1);
    when(peliculasRepository.save(any(Pelicula.class))).thenReturn(peliculaUpdate);

    peliculaResponse1.setTitulo(nuevoTitulo);
    PeliculaResponseDto expectedPeliculaResponse = peliculaResponse1;
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    PeliculaResponseDto actualPeliculaResponse = peliculasService.update(id, peliculaUpdateDto);

    // Assert
    assertThat(actualPeliculaResponse)
        .usingRecursiveComparison()
        .ignoringFields("updatedAt")
        .isEqualTo(expectedPeliculaResponse);

    // Verify
    verify(peliculasRepository).findById(id);
    verify(peliculasRepository).save(any(Pelicula.class));
  }

  @Test
  void update_ShouldThrowPeliculaNotFound_WhenInvalidIdProvided() {
    // Arrange
    Long id = 1L;
    PeliculaUpdateDto peliculaUpdateDto = PeliculaUpdateDto.builder()
        .titulo("Nuevo Título")
        .build();
    when(peliculasRepository.findById(id)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(
        () -> peliculasService.update(id, peliculaUpdateDto))
        .isInstanceOf(PeliculaNotFoundException.class)
        .hasMessage("Película con id " + id + " no encontrada");

    // Verify
    verify(peliculasRepository).findById(id);
    verify(peliculasRepository, never()).save(any(Pelicula.class));
  }

  @Test
  void deleteById_ShouldDeletePelicula_WhenValidIdProvided() throws IOException {
    // Arrange
    Long id = 1L;
    when(peliculasRepository.findById(id)).thenReturn(Optional.of(pelicula1));
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    assertThatCode(() -> peliculasService.deleteById(id))
        .doesNotThrowAnyException();

    // Assert
    verify(peliculasRepository).findById(id);
    verify(peliculasRepository).deleteById(id);
    verify(webSocketService, atLeastOnce()).sendMessage(anyString());
  }

  @Test
  void deleteById_ShouldThrowPeliculaNotFound_WhenInvalidIdProvided() {
    // Arrange
    Long id = 1L;
    when(peliculasRepository.findById(id)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> peliculasService.deleteById(id))
        .isInstanceOf(PeliculaNotFoundException.class)
        .hasMessage("Película con id " + id + " no encontrada");

    // Verify
    verify(peliculasRepository, never()).deleteById(id);
  }

  @Test
  void onChange_ShouldSendMessage_WhenValidDataProvided() throws IOException {
    // Arrange
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    peliculasService.onChange(Notificacion.Tipo.CREATE, pelicula1);

    // Assert
    verify(webSocketService, atLeastOnce()).sendMessage(anyString());
  }
}

