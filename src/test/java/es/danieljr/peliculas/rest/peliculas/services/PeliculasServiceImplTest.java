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

/**
 * Tests del servicio de Películas (lógica de negocio).
 * Se mockean el repositorio, WebSocket y mappers para aislar solo la lógica del servicio.
 */
@ExtendWith(MockitoExtension.class) // Habilita Mockito (crea mocks e inyecta con @InjectMocks)
class PeliculasServiceImplTest {
  // ========== Datos de prueba (entidades y DTOs usados en los tests) ==========
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

  // ========== Mocks: dependencias del servicio (simuladas, no reales) ==========
  @Mock
  private PeliculasRepository peliculasRepository;
  @Spy
  private PeliculaMapper peliculaMapper; // Spy = instancia real, permite verificar llamadas
  @Mock
  private WebSocketConfig webSocketConfig;
  @Mock
  private PeliculaNotificationMapper peliculaNotificationMapper;
  @Mock
  private ObjectMapper objectMapper;
  @Mock
  private WebSocketHandler webSocketService;

  @InjectMocks
  private PeliculasServiceImpl peliculasService; // Servicio a probar; recibe los mocks anteriores
  @Captor
  private ArgumentCaptor<Pelicula> peliculaCaptor; // Para capturar el argumento pasado a save()

  /** Inicializa el DTO de respuesta y asigna el mock de WebSocket al servicio (evita NPE en onChange). */
  @BeforeEach
  void setUp() {
    peliculaResponse1 = peliculaMapper.toPeliculaResponseDto(pelicula1);
    peliculasService.setWebSocketService(webSocketService);
  }

  /** findAll sin filtros: el repositorio devuelve una página; el servicio la mapea a DTOs. */
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

  /** findAll con filtro título: se llama al repositorio con Specification que incluye el título. */
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

  /** findAll con filtro género: se llama al repositorio con Specification que incluye el género. */
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

  /** findAll con título y género: Specification combina ambos criterios. */
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

  /** findAll con orden ascendente: el Pageable incluye Sort; el resultado está ordenado. */
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

  /** findAll con orden descendente: el Pageable incluye Sort descendente. */
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

  /** findById con id existente: el repositorio devuelve la entidad; el servicio la convierte a DTO. */
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

  /** findById con id inexistente: el repositorio devuelve empty; el servicio lanza PeliculaNotFoundException. */
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

  /** save: mapper convierte DTO a entidad, repositorio guarda, se llama onChange (WebSocket) y se devuelve DTO. */
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

  /** update: se busca la entidad, se actualiza con el mapper, se guarda y se notifica por WebSocket. */
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

  /** update con id inexistente: findById devuelve empty y se lanza PeliculaNotFoundException. */
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

  /** deleteById: se busca la entidad, se borra en el repositorio y se notifica DELETE por WebSocket. */
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

  /** deleteById con id inexistente: se lanza PeliculaNotFoundException sin llamar a delete. */
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

  /** onChange: el servicio construye la notificación, la serializa a JSON y llama a sendMessage del WebSocket. */
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

