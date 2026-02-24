package es.danieljr.peliculas.rest.entradas.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.danieljr.peliculas.config.websockets.WebSocketConfig;
import es.danieljr.peliculas.config.websockets.WebSocketHandler;
import es.danieljr.peliculas.rest.entradas.dto.EntradaCreateDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaResponseDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaUpdateDto;
import es.danieljr.peliculas.rest.entradas.exceptions.EntradaNotFoundException;
import es.danieljr.peliculas.rest.entradas.mappers.EntradaMapper;
import es.danieljr.peliculas.rest.entradas.models.Entrada;
import es.danieljr.peliculas.rest.entradas.repositories.EntradasRepository;
import es.danieljr.peliculas.rest.peliculas.exceptions.PeliculaNotFoundException;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import es.danieljr.peliculas.rest.peliculas.repositories.PeliculasRepository;
import es.danieljr.peliculas.rest.users.exceptions.UserNotFound;
import es.danieljr.peliculas.rest.users.models.User;
import es.danieljr.peliculas.rest.users.repositories.UsersRepository;
import es.danieljr.peliculas.websockets.notifications.mappers.EntradaNotificationMapper;
import es.danieljr.peliculas.websockets.notifications.models.Notificacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests del servicio de Entradas (lógica de negocio).
 * Se mockean repositorios (Entradas, Películas, Usuarios), WebSocket y mappers.
 */
@ExtendWith(MockitoExtension.class)
class EntradasServiceImplTest {
  // ========== Datos de prueba ==========
  private final Pelicula pelicula = Pelicula.builder()
      .idPelicula(1L)
      .titulo("El Padrino")
      .genero("Drama")
      .duracion(175)
      .director("Francis Ford Coppola")
      .build();

  private final Entrada entrada1 = Entrada.builder()
      .idEntrada(1L)
      .fecha(LocalDate.of(2025, 12, 15))
      .precio(10.0)
      .metodoPago("Tarjeta")
      .pelicula(pelicula)
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();

  private final Entrada entrada2 = Entrada.builder()
      .idEntrada(2L)
      .fecha(LocalDate.of(2025, 12, 16))
      .precio(12.0)
      .metodoPago("Efectivo")
      .pelicula(pelicula)
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();

  private final User usuario = User.builder()
      .id(1L)
      .nombre("Test")
      .apellidos("User")
      .username("testuser")
      .email("test@test.com")
      .password("password")
      .build();

  // ========== Mocks (repositorios, mappers, WebSocket) ==========
  @Mock
  private EntradasRepository entradasRepository;
  @Mock
  private PeliculasRepository peliculasRepository;
  @Mock
  private UsersRepository usersRepository;
  @Spy
  private EntradaMapper entradaMapper;
  @Mock
  private WebSocketConfig webSocketConfig;
  @Mock
  private EntradaNotificationMapper entradaNotificationMapper;
  @Mock
  private ObjectMapper objectMapper;
  @Mock
  private WebSocketHandler webSocketService;

  @InjectMocks
  private EntradasServiceImpl entradasService;
  @Captor
  private ArgumentCaptor<Entrada> entradaCaptor;

  /** Los tests siguen patrón Arrange (when/thenReturn) → Act (llamar al servicio) → Assert → Verify. */
  @Test
  void findAll_ShouldReturnAllEntradas_WhenNoParametersProvided() {
    // Arrange
    List<Entrada> expectedEntradas = List.of(entrada1, entrada2);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("idEntrada").ascending());
    Page<Entrada> expectedPage = new PageImpl<>(expectedEntradas);
    when(entradasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    Page<EntradaResponseDto> actualPage =
        entradasService.findAll(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), pageable);

    // Assert
    assertAll("findAll",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );

    // Verify
    verify(entradasRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void findAll_ShouldReturnEntradasByPeliculaId_WhenPeliculaIdParameterProvided() {
    // Arrange
    Optional<Long> peliculaId = Optional.of(1L);
    List<Entrada> expectedEntradas = List.of(entrada1);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("idEntrada").ascending());
    Page<Entrada> expectedPage = new PageImpl<>(expectedEntradas);
    when(entradasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    Page<EntradaResponseDto> actualPage =
        entradasService.findAll(peliculaId, Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), pageable);

    // Assert
    assertAll("findAll",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );

    // Verify
    verify(entradasRepository, only()).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void findAll_ShouldReturnEntradasOrderedAscending_WhenSortByAscending() {
    // Arrange
    List<Entrada> expectedEntradas = List.of(entrada1, entrada2);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("precio").ascending());
    Page<Entrada> expectedPage = new PageImpl<>(expectedEntradas);
    when(entradasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    Page<EntradaResponseDto> actualPage =
        entradasService.findAll(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), pageable);

    // Assert
    assertAll("findAll with ascending sort",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );

    // Verify
    verify(entradasRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void findAll_ShouldReturnEntradasOrderedDescending_WhenSortByDescending() {
    // Arrange
    List<Entrada> expectedEntradas = List.of(entrada2, entrada1);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("precio").descending());
    Page<Entrada> expectedPage = new PageImpl<>(expectedEntradas);
    when(entradasRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(expectedPage);

    // Act
    Page<EntradaResponseDto> actualPage =
        entradasService.findAll(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), pageable);

    // Assert
    assertAll("findAll with descending sort",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );

    // Verify
    verify(entradasRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void findById_ShouldReturnEntrada_WhenValidIdProvided() {
    // Arrange
    Long id = 1L;
    EntradaResponseDto expectedEntradaResponse = entradaMapper.toEntradaResponseDto(entrada1);
    when(entradasRepository.findById(id)).thenReturn(Optional.of(entrada1));

    // Act
    EntradaResponseDto actualEntradaResponse = entradasService.findById(id);

    // Assert
    assertEquals(expectedEntradaResponse, actualEntradaResponse);

    // Verify
    verify(entradasRepository, only()).findById(id);
  }

  @Test
  void findById_ShouldThrowEntradaNotFound_WhenInvalidIdProvided() {
    // Arrange
    Long id = 1L;
    when(entradasRepository.findById(id)).thenReturn(Optional.empty());

    // Act & Assert
    var res = assertThrows(EntradaNotFoundException.class, () -> entradasService.findById(id));
    assertEquals("Entrada con id " + id + " no encontrada", res.getMessage());

    // Verify
    verify(entradasRepository).findById(id);
  }

  @Test
  void save_ShouldReturnSavedEntrada_WhenValidEntradaCreateDtoProvided() throws IOException {
    // Arrange
    EntradaCreateDto entradaCreateDto = EntradaCreateDto.builder()
        .fecha(LocalDate.of(2025, 12, 20))
        .precio(15.0)
        .metodoPago("Tarjeta")
        .peliculaId(1L)
        .build();
    Entrada expectedEntrada = Entrada.builder()
        .idEntrada(1L)
        .fecha(LocalDate.of(2025, 12, 20))
        .precio(15.0)
        .metodoPago("Tarjeta")
        .pelicula(pelicula)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    EntradaResponseDto expectedEntradaResponse = entradaMapper.toEntradaResponseDto(expectedEntrada);
    when(peliculasRepository.findById(entradaCreateDto.getPeliculaId())).thenReturn(Optional.of(pelicula));
    when(entradasRepository.save(any(Entrada.class))).thenReturn(expectedEntrada);
    entradasService.setWebSocketService(webSocketService);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    EntradaResponseDto actualEntradaResponse = entradasService.save(entradaCreateDto);

    // Assert
    assertEquals(expectedEntradaResponse, actualEntradaResponse);

    // Verify
    verify(entradasRepository).save(entradaCaptor.capture());
    verify(webSocketService, atLeastOnce()).sendMessage(anyString());
    Entrada entradaCaptured = entradaCaptor.getValue();
    assertEquals(expectedEntrada.getFecha(), entradaCaptured.getFecha());
  }

  @Test
  void save_ShouldThrowPeliculaNotFound_WhenInvalidPeliculaIdProvided() {
    // Arrange
    EntradaCreateDto entradaCreateDto = EntradaCreateDto.builder()
        .fecha(LocalDate.of(2025, 12, 20))
        .precio(15.0)
        .metodoPago("Tarjeta")
        .peliculaId(999L)
        .build();
    when(peliculasRepository.findById(entradaCreateDto.getPeliculaId())).thenReturn(Optional.empty());

    // Act & Assert
    var res = assertThrows(PeliculaNotFoundException.class, () -> entradasService.save(entradaCreateDto));
    assertEquals("Película con id " + entradaCreateDto.getPeliculaId() + " no encontrada", res.getMessage());

    // Verify
    verify(peliculasRepository).findById(entradaCreateDto.getPeliculaId());
    verify(entradasRepository, never()).save(any(Entrada.class));
  }

  @Test
  void update_ShouldReturnUpdatedEntrada_WhenValidIdAndEntradaUpdateDtoProvided() throws IOException {
    // Arrange
    Long id = 1L;
    Double nuevoPrecio = 20.0;
    when(entradasRepository.findById(id)).thenReturn(Optional.of(entrada1));

    EntradaUpdateDto entradaUpdateDto = EntradaUpdateDto.builder()
        .precio(nuevoPrecio)
        .build();
    Entrada entradaUpdate = entradaMapper.toEntrada(entradaUpdateDto, entrada1);
    when(entradasRepository.save(any(Entrada.class))).thenReturn(entradaUpdate);

    EntradaResponseDto expectedEntradaResponse = entradaMapper.toEntradaResponseDto(entradaUpdate);
    entradasService.setWebSocketService(webSocketService);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    EntradaResponseDto actualEntradaResponse = entradasService.update(id, entradaUpdateDto);

    // Assert
    assertThat(actualEntradaResponse)
        .usingRecursiveComparison()
        .ignoringFields("updatedAt")
        .isEqualTo(expectedEntradaResponse);

    // Verify
    verify(entradasRepository).findById(id);
    verify(entradasRepository).save(any(Entrada.class));
    verify(webSocketService, atLeastOnce()).sendMessage(anyString());
  }

  @Test
  void update_ShouldThrowEntradaNotFound_WhenInvalidIdProvided() {
    // Arrange
    Long id = 1L;
    EntradaUpdateDto entradaUpdateDto = EntradaUpdateDto.builder()
        .precio(20.0)
        .build();
    when(entradasRepository.findById(id)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(
        () -> entradasService.update(id, entradaUpdateDto))
        .isInstanceOf(EntradaNotFoundException.class)
        .hasMessage("Entrada con id " + id + " no encontrada");

    // Verify
    verify(entradasRepository).findById(id);
    verify(entradasRepository, never()).save(any(Entrada.class));
  }

  @Test
  void deleteById_ShouldDeleteEntrada_WhenValidIdProvided() throws IOException {
    // Arrange
    Long id = 1L;
    when(entradasRepository.findById(id)).thenReturn(Optional.of(entrada1));
    entradasService.setWebSocketService(webSocketService);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    assertThatCode(() -> entradasService.deleteById(id))
        .doesNotThrowAnyException();

    // Assert
    verify(entradasRepository).findById(id);
    verify(entradasRepository).deleteById(id);
    verify(webSocketService, atLeastOnce()).sendMessage(anyString());
  }

  @Test
  void deleteById_ShouldThrowEntradaNotFound_WhenInvalidIdProvided() {
    // Arrange
    Long id = 1L;
    when(entradasRepository.findById(id)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> entradasService.deleteById(id))
        .isInstanceOf(EntradaNotFoundException.class)
        .hasMessage("Entrada con id " + id + " no encontrada");

    // Verify
    verify(entradasRepository, never()).deleteById(id);
  }

  @Test
  void onChange_ShouldSendMessage_WhenValidDataProvided() throws IOException {
    // Arrange
    entradasService.setWebSocketService(webSocketService);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    entradasService.onChange(Notificacion.Tipo.CREATE, entrada1);

    // Assert
    verify(webSocketService, atLeastOnce()).sendMessage(anyString());
  }

  @Test
  void findByUsuarioId_ShouldReturnPageOfEntradas_WhenValidUsuarioIdProvided() {
    // Arrange
    Long usuarioId = 1L;
    List<Entrada> expectedEntradas = List.of(entrada1, entrada2);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("idEntrada").ascending());
    Page<Entrada> expectedPage = new PageImpl<>(expectedEntradas);
    when(entradasRepository.findByUsuarioId(usuarioId, pageable)).thenReturn(expectedPage);

    // Act
    Page<EntradaResponseDto> actualPage = entradasService.findByUsuarioId(usuarioId, pageable);

    // Assert
    assertAll("findByUsuarioId",
        () -> assertNotNull(actualPage),
        () -> assertFalse(actualPage.isEmpty()),
        () -> assertTrue(actualPage.getTotalElements() > 0)
    );

    // Verify
    verify(entradasRepository, only()).findByUsuarioId(usuarioId, pageable);
  }

  @Test
  void findByUsuarioId_ShouldReturnEntrada_WhenValidUsuarioIdAndIdEntradaProvided() {
    // Arrange
    Long usuarioId = 1L;
    Long idEntrada = 1L;
    entrada1.setUsuario(usuario);
    when(entradasRepository.findByUsuarioId(usuarioId)).thenReturn(List.of(entrada1, entrada2));

    // Act
    EntradaResponseDto actualEntradaResponse = entradasService.findByUsuarioId(usuarioId, idEntrada);

    // Assert
    assertNotNull(actualEntradaResponse);
    assertEquals(idEntrada, actualEntradaResponse.getIdEntrada());

    // Verify
    verify(entradasRepository, times(1)).findByUsuarioId(usuarioId);
  }

  @Test
  void findByUsuarioId_ShouldThrowEntradaBadRequest_WhenEntradaDoesNotBelongToUsuario() {
    // Arrange
    Long usuarioId = 1L;
    Long idEntrada = 999L;
    when(entradasRepository.findByUsuarioId(usuarioId)).thenReturn(List.of(entrada1, entrada2));

    // Act & Assert
    assertThatThrownBy(() -> entradasService.findByUsuarioId(usuarioId, idEntrada))
        .isInstanceOf(es.danieljr.peliculas.rest.entradas.exceptions.EntradaBadRequestException.class)
        .hasMessageContaining("no corresponde a este usuario");

    // Verify
    verify(entradasRepository, times(1)).findByUsuarioId(usuarioId);
  }

  @Test
  void save_ShouldReturnSavedEntrada_WhenValidEntradaCreateDtoAndUsuarioIdProvided() throws IOException {
    // Arrange
    Long usuarioId = 1L;
    EntradaCreateDto entradaCreateDto = EntradaCreateDto.builder()
        .fecha(LocalDate.of(2025, 12, 20))
        .precio(15.0)
        .metodoPago("Tarjeta")
        .peliculaId(1L)
        .build();
    Entrada expectedEntrada = Entrada.builder()
        .idEntrada(1L)
        .fecha(LocalDate.of(2025, 12, 20))
        .precio(15.0)
        .metodoPago("Tarjeta")
        .pelicula(pelicula)
        .usuario(usuario)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    EntradaResponseDto expectedEntradaResponse = entradaMapper.toEntradaResponseDto(expectedEntrada);
    when(peliculasRepository.findById(entradaCreateDto.getPeliculaId())).thenReturn(Optional.of(pelicula));
    when(usersRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
    when(entradasRepository.save(any(Entrada.class))).thenReturn(expectedEntrada);
    entradasService.setWebSocketService(webSocketService);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    EntradaResponseDto actualEntradaResponse = entradasService.save(entradaCreateDto, usuarioId);

    // Assert
    assertEquals(expectedEntradaResponse, actualEntradaResponse);

    // Verify
    verify(entradasRepository).save(entradaCaptor.capture());
    verify(webSocketService, atLeastOnce()).sendMessage(anyString());
    Entrada entradaCaptured = entradaCaptor.getValue();
    assertEquals(expectedEntrada.getFecha(), entradaCaptured.getFecha());
    assertEquals(usuarioId, entradaCaptured.getUsuario().getId());
  }

  @Test
  void save_ShouldThrowUserNotFound_WhenInvalidUsuarioIdProvided() {
    // Arrange
    Long usuarioId = 999L;
    EntradaCreateDto entradaCreateDto = EntradaCreateDto.builder()
        .fecha(LocalDate.of(2025, 12, 20))
        .precio(15.0)
        .metodoPago("Tarjeta")
        .peliculaId(1L)
        .build();
    when(peliculasRepository.findById(entradaCreateDto.getPeliculaId())).thenReturn(Optional.of(pelicula));
    when(usersRepository.findById(usuarioId)).thenReturn(Optional.empty());

    // Act & Assert
    var res = assertThrows(UserNotFound.class, () -> entradasService.save(entradaCreateDto, usuarioId));
    assertEquals("Usuario con id " + usuarioId + " no encontrado", res.getMessage());

    // Verify
    verify(usersRepository).findById(usuarioId);
    verify(entradasRepository, never()).save(any(Entrada.class));
  }

  @Test
  void update_ShouldReturnUpdatedEntrada_WhenValidIdAndEntradaUpdateDtoAndUsuarioIdProvided() throws IOException {
    // Arrange
    Long id = 1L;
    Long usuarioId = 1L;
    Double nuevoPrecio = 20.0;
    entrada1.setUsuario(usuario);
    when(entradasRepository.findById(id)).thenReturn(Optional.of(entrada1));

    EntradaUpdateDto entradaUpdateDto = EntradaUpdateDto.builder()
        .precio(nuevoPrecio)
        .build();
    Entrada entradaUpdate = entradaMapper.toEntrada(entradaUpdateDto, entrada1);
    entradaUpdate.setUsuario(usuario);
    when(entradasRepository.save(any(Entrada.class))).thenReturn(entradaUpdate);

    EntradaResponseDto expectedEntradaResponse = entradaMapper.toEntradaResponseDto(entradaUpdate);
    entradasService.setWebSocketService(webSocketService);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    EntradaResponseDto actualEntradaResponse = entradasService.update(id, entradaUpdateDto, usuarioId);

    // Assert
    assertThat(actualEntradaResponse)
        .usingRecursiveComparison()
        .ignoringFields("updatedAt")
        .isEqualTo(expectedEntradaResponse);

    // Verify
    verify(entradasRepository).findById(id);
    verify(entradasRepository).save(any(Entrada.class));
    verify(webSocketService, atLeastOnce()).sendMessage(anyString());
  }

  @Test
  void update_ShouldThrowEntradaBadRequest_WhenEntradaDoesNotBelongToUsuario() {
    // Arrange
    Long id = 1L;
    Long usuarioId = 2L; // Usuario que intenta actualizar
    // La entrada pertenece al usuario con id=1, pero se intenta actualizar con usuarioId=2
    entrada1.setUsuario(usuario); // usuario tiene id=1
    when(entradasRepository.findById(id)).thenReturn(Optional.of(entrada1));

    EntradaUpdateDto entradaUpdateDto = EntradaUpdateDto.builder()
        .precio(20.0)
        .build();

    // Act & Assert
    assertThatThrownBy(() -> entradasService.update(id, entradaUpdateDto, usuarioId))
        .isInstanceOf(es.danieljr.peliculas.rest.entradas.exceptions.EntradaBadRequestException.class)
        .hasMessageContaining("no corresponde a este usuario");

    // Verify
    verify(entradasRepository).findById(id);
    verify(entradasRepository, never()).save(any(Entrada.class));
  }

  @Test
  void deleteById_ShouldDeleteEntrada_WhenValidIdAndUsuarioIdProvided() throws IOException {
    // Arrange
    Long id = 1L;
    Long usuarioId = 1L;
    entrada1.setUsuario(usuario);
    when(entradasRepository.findById(id)).thenReturn(Optional.of(entrada1));
    entradasService.setWebSocketService(webSocketService);
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"json\"}");
    doNothing().when(webSocketService).sendMessage(anyString());

    // Act
    assertThatCode(() -> entradasService.deleteById(id, usuarioId))
        .doesNotThrowAnyException();

    // Assert
    verify(entradasRepository).findById(id);
    verify(entradasRepository).deleteById(id);
    verify(webSocketService, atLeastOnce()).sendMessage(anyString());
  }

  @Test
  void deleteById_ShouldThrowEntradaBadRequest_WhenEntradaDoesNotBelongToUsuario() {
    // Arrange
    Long id = 1L;
    Long usuarioId = 2L; // Usuario que intenta eliminar
    // La entrada pertenece al usuario con id=1, pero se intenta eliminar con usuarioId=2
    entrada1.setUsuario(usuario); // usuario tiene id=1
    when(entradasRepository.findById(id)).thenReturn(Optional.of(entrada1));

    // Act & Assert
    assertThatThrownBy(() -> entradasService.deleteById(id, usuarioId))
        .isInstanceOf(es.danieljr.peliculas.rest.entradas.exceptions.EntradaBadRequestException.class)
        .hasMessageContaining("no corresponde a este usuario");

    // Verify
    verify(entradasRepository).findById(id);
    verify(entradasRepository, never()).deleteById(id);
  }
}

