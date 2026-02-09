package es.danieljr.peliculas.rest.entradas.controllers;

import es.danieljr.peliculas.rest.entradas.dto.EntradaCreateDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaResponseDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaUpdateDto;
import es.danieljr.peliculas.rest.entradas.exceptions.EntradaNotFoundException;
import es.danieljr.peliculas.rest.entradas.services.EntradasService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
class EntradasRestControllerTest {
    private final String ENDPOINT = "/api/v1/entradas";

    private final EntradaResponseDto entradaResponse1 = EntradaResponseDto.builder()
            .idEntrada(1L)
            .fecha(LocalDate.of(2025, 12, 15))
            .precio(10.0)
            .metodoPago("Tarjeta")
            .peliculaId(1L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    private final EntradaResponseDto entradaResponse2 = EntradaResponseDto.builder()
            .idEntrada(2L)
            .fecha(LocalDate.of(2025, 12, 16))
            .precio(12.0)
            .metodoPago("Efectivo")
            .peliculaId(1L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    @Autowired
    private MockMvcTester mockMvcTester;

    @MockitoBean
    private EntradasService entradasService;

    @Test
    void getAll() {
        // Arrange
        var entradas = List.of(entradaResponse1, entradaResponse2);
        var pageable = PageRequest.of(0, 10, Sort.by("idEntrada").ascending());
        var page = new PageImpl<>(entradas);
        when(entradasService.findAll(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), pageable)).thenReturn(page);

        // Act. Consultar el endpoint
        var result = mockMvcTester.get()
                .uri(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$.content.length()").isEqualTo(entradas.size());
                    assertThat(json).extractingPath("$.content[0]")
                            .convertTo(EntradaResponseDto.class).isEqualTo(entradaResponse1);
                    assertThat(json).extractingPath("$.content[1]")
                            .convertTo(EntradaResponseDto.class).isEqualTo(entradaResponse2);
                });

        // Verify
        verify(entradasService, times(1))
            .findAll(Optional.empty(), Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), pageable);
    }

    @Test
    void getAllByPeliculaId() {
        // Arrange
        var entradas = List.of(entradaResponse2);
        String queryString = "?peliculaId=" + entradaResponse2.getPeliculaId();
        Optional<Long> peliculaId = Optional.of(entradaResponse2.getPeliculaId());
        var pageable = PageRequest.of(0, 10, Sort.by("idEntrada").ascending());
        var page = new PageImpl<>(entradas);
        when(entradasService.findAll(peliculaId, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), pageable))
            .thenReturn(page);

        // Act
        var result = mockMvcTester.get()
                .uri(ENDPOINT + queryString)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$.content.length()").isEqualTo(entradas.size());
                    assertThat(json).extractingPath("$.content[0]")
                            .convertTo(EntradaResponseDto.class).isEqualTo(entradaResponse2);
                });

        // Verify
        verify(entradasService, times(1))
            .findAll(peliculaId, Optional.empty(), Optional.empty(),
                    Optional.empty(), Optional.empty(), Optional.empty(), pageable);
    }

    @Test
    void getById() {
        // Arrange
        Long id = entradaResponse1.getIdEntrada();
        when(entradasService.findById(id)).thenReturn(entradaResponse1);

        // Act
        var result = mockMvcTester.get()
                .uri(ENDPOINT + "/" + id.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatusOk()
                .bodyJson()
                .convertTo(EntradaResponseDto.class)
                .isEqualTo(entradaResponse1);

        // Verify
        verify(entradasService, only()).findById(anyLong());

    }

    @Test
    void getById_shouldThrowEntradaNotFound_whenInvalidIdProvided() {
        // Arrange
        Long id = 3L;
        when(entradasService.findById(anyLong())).thenThrow(new EntradaNotFoundException(id));

        // Act
        var result = mockMvcTester.get()
                .uri(ENDPOINT + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange();

        assertThat(result)
                .hasStatus4xxClientError()
                .hasFailed().failure()
                .isInstanceOf(EntradaNotFoundException.class)
                .hasMessageContaining("no encontrada");

        // Verify
        verify(entradasService, only()).findById(anyLong());

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create() {
        // Arrange
        String requestBody = """
           {
              "fecha": "2025-12-20",
              "precio": 15.0,
              "metodoPago": "Tarjeta",
              "peliculaId": 1
           }
           """;

        var entradaSaved = EntradaResponseDto.builder()
                .idEntrada(1L)
                .fecha(LocalDate.of(2025, 12, 20))
                .precio(15.0)
                .metodoPago("Tarjeta")
                .peliculaId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(entradasService.save(any(EntradaCreateDto.class))).thenReturn(entradaSaved);

        // Act
        var result = mockMvcTester.post()
                .uri(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .convertTo(EntradaResponseDto.class)
                .isEqualTo(entradaSaved);

        verify(entradasService, only()).save(any(EntradaCreateDto.class));


    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_whenBadRequest() {
        // Arrange
        String requestBody = """
           {
              "fecha": null,
              "precio": -1,
              "metodoPago": "",
              "peliculaId": null
           }
           """;

        // Act
        var result = mockMvcTester.post()
                .uri(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPathSatisfying("$.errores", path -> {
                    assertThat(path).hasFieldOrProperty("fecha");
                    assertThat(path).hasFieldOrProperty("precio");
                    assertThat(path).hasFieldOrProperty("metodoPago");
                    assertThat(path).hasFieldOrProperty("peliculaId");
                });

        verify(entradasService, never()).save(any(EntradaCreateDto.class));

    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update() {
      // Arrange
      long id = 1L;
      String requestBody = """
           {
              "precio": 20.0
           }
           """;

      var entradaSaved = EntradaResponseDto.builder()
          .idEntrada(1L)
          .fecha(LocalDate.of(2025, 12, 15))
          .precio(20.0)
          .metodoPago("Tarjeta")
          .peliculaId(1L)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      when(entradasService.update(anyLong(), any(EntradaUpdateDto.class))).thenReturn(entradaSaved);

      // Act
      var result = mockMvcTester.put()
          .uri(ENDPOINT+ "/" + id)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .bodyJson()
          .convertTo(EntradaResponseDto.class)
          .isEqualTo(entradaSaved);

      verify(entradasService, only()).update(anyLong(), any(EntradaUpdateDto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_shouldThrowEntradaNotFound() {
      // Arrange
      Long id = 3L;
      String requestBody = """
           {
              "precio": 20.0
           }
           """;
      when(entradasService.update(anyLong(), any(EntradaUpdateDto.class))).thenThrow(new EntradaNotFoundException(id));

      // Act
      var result = mockMvcTester.put()
          .uri(ENDPOINT + "/" + id)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
          .exchange();

      assertThat(result)
          .hasStatus(HttpStatus.NOT_FOUND)
          .hasFailed().failure()
          .isInstanceOf(EntradaNotFoundException.class)
          .hasMessageContaining("no encontrada");

      // Verify
      verify(entradasService, only()).update(anyLong(), any(EntradaUpdateDto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatePartial() {
      // Arrange
      long id = 1L;
      String requestBody = """
           {
              "precio": 20.0
           }
           """;

      var entradaSaved = EntradaResponseDto.builder()
          .idEntrada(1L)
          .fecha(LocalDate.of(2025, 12, 15))
          .precio(20.0)
          .metodoPago("Tarjeta")
          .peliculaId(1L)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();

      when(entradasService.update(anyLong(), any(EntradaUpdateDto.class))).thenReturn(entradaSaved);

      // Act
      var result = mockMvcTester.patch()
          .uri(ENDPOINT+ "/" + id)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .bodyJson()
          .convertTo(EntradaResponseDto.class)
          .isEqualTo(entradaSaved);

      verify(entradasService, only()).update(anyLong(), any(EntradaUpdateDto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete() {
      // Arrange
      long id = 1L;
      doNothing().when(entradasService).deleteById(anyLong());
      // Act
      var result = mockMvcTester.delete()
          .uri(ENDPOINT+ "/" + id)
          .exchange();
      // Assert
      assertThat(result)
          .hasStatus(HttpStatus.NO_CONTENT);

      verify(entradasService, only()).deleteById(anyLong());
    }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_shouldThrowEntradaNotFound() {
    // Arrange
    Long id = 1L;
    doThrow(new EntradaNotFoundException(id)).when(entradasService).deleteById(anyLong());
    // Act
    var result = mockMvcTester.delete()
        .uri(ENDPOINT+ "/" + id)
        .exchange();
    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND);

    verify(entradasService, only()).deleteById(anyLong());
  }

  @Test
  void getAll_WithSortAscending_ShouldReturnSortedResults() {
    // Arrange
    var entradas = List.of(entradaResponse1, entradaResponse2);
    var pageable = PageRequest.of(0, 10, Sort.by("precio").ascending());
    var page = new PageImpl<>(entradas);
    when(entradasService.findAll(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), pageable))
        .thenReturn(page);

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT + "?sortBy=precio&direction=asc")
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson().satisfies(json -> {
          assertThat(json).extractingPath("$.content.length()").isEqualTo(entradas.size());
        });

    // Verify
    verify(entradasService, times(1))
        .findAll(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), pageable);
  }

  @Test
  void getAll_WithSortDescending_ShouldReturnSortedResults() {
    // Arrange
    var entradas = List.of(entradaResponse2, entradaResponse1);
    var pageable = PageRequest.of(0, 10, Sort.by("precio").descending());
    var page = new PageImpl<>(entradas);
    when(entradasService.findAll(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), pageable))
        .thenReturn(page);

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT + "?sortBy=precio&direction=desc")
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson().satisfies(json -> {
          assertThat(json).extractingPath("$.content.length()").isEqualTo(entradas.size());
        });

    // Verify
    verify(entradasService, times(1))
        .findAll(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), pageable);
  }

  @Test
  void getAll_WithPagination_ShouldReturnPagedResults() {
    // Arrange
    var entradas = List.of(entradaResponse1);
    var pageable = PageRequest.of(0, 1, Sort.by("idEntrada").ascending());
    var page = new PageImpl<>(entradas, pageable, 2);
    when(entradasService.findAll(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), pageable))
        .thenReturn(page);

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT + "?page=0&size=1")
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson().satisfies(json -> {
          assertThat(json).extractingPath("$.content.length()").isEqualTo(1);
          assertThat(json).extractingPath("$.totalElements").isEqualTo(2);
          assertThat(json).extractingPath("$.totalPages").isEqualTo(2);
        });

    // Verify
    verify(entradasService, times(1))
        .findAll(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty(), Optional.empty(), pageable);
  }
}

