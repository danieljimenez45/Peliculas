package es.danieljr.peliculas.rest.peliculas.controllers;

import es.danieljr.peliculas.rest.peliculas.dto.PeliculaCreateDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaUpdateDto;
import es.danieljr.peliculas.rest.peliculas.exceptions.PeliculaNotFoundException;
import es.danieljr.peliculas.rest.peliculas.services.PeliculasService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
class PeliculasRestControllerTest {

  private final String ENDPOINT = "/api/v1/peliculas";

  private final PeliculaResponseDto peliculaResponse1 = PeliculaResponseDto.builder()
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

  private final PeliculaResponseDto peliculaResponse2 = PeliculaResponseDto.builder()
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

  @Autowired
  private MockMvcTester mockMvcTester;

  @MockitoBean
  private PeliculasService peliculasService;

  @Test
  void getAll() {
    // Arrange
    var peliculaResponses = List.of(peliculaResponse1, peliculaResponse2);
    var pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());
    var page = new PageImpl<>(peliculaResponses);
    when(peliculasService.findAll(Optional.empty(), Optional.empty(), pageable))
        .thenReturn(page);

    // Act. Consultar el endpoint
    var result = mockMvcTester.get()
        .uri(ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson().satisfies(json -> {
          assertThat(json).extractingPath("$.content.length()").isEqualTo(peliculaResponses.size());
          assertThat(json).extractingPath("$.content[0]")
              .convertTo(PeliculaResponseDto.class).isEqualTo(peliculaResponse1);
          assertThat(json).extractingPath("$.content[1]")
              .convertTo(PeliculaResponseDto.class).isEqualTo(peliculaResponse2);
        });

    // Verify
    verify(peliculasService, times(1))
        .findAll(Optional.empty(), Optional.empty(), pageable);
  }

  @Test
  void getAllByTitulo() {
    // Arrange
    var peliculaResponses = List.of(peliculaResponse2);
    String queryString = "?titulo=" + peliculaResponse2.getTitulo();
    Optional<String> titulo = Optional.of(peliculaResponse2.getTitulo());
    var pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());
    var page = new PageImpl<>(peliculaResponses);
    when(peliculasService.findAll(titulo, Optional.empty(), pageable))
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
          assertThat(json).extractingPath("$.content.length()").isEqualTo(peliculaResponses.size());
          assertThat(json).extractingPath("$.content[0]")
              .convertTo(PeliculaResponseDto.class).isEqualTo(peliculaResponse2);
        });

    // Verify
    verify(peliculasService, times(1))
        .findAll(titulo, Optional.empty(), pageable);
  }

  @Test
  void getAllByGenero() {
    // Arrange
    var peliculaResponses = List.of(peliculaResponse2);
    String queryString = "?genero=" + peliculaResponse2.getGenero();
    Optional<String> genero = Optional.of(peliculaResponse2.getGenero());
    var pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());
    var page = new PageImpl<>(peliculaResponses);
    when(peliculasService.findAll(Optional.empty(), genero, pageable))
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
          assertThat(json).extractingPath("$.content.length()").isEqualTo(peliculaResponses.size());
          assertThat(json).extractingPath("$.content[0]")
              .convertTo(PeliculaResponseDto.class).isEqualTo(peliculaResponse2);
        });

    // Verify
    verify(peliculasService, only())
        .findAll(Optional.empty(), genero, pageable);
  }

  @Test
  void getAllByTituloAndGenero() {
    // Arrange
    var peliculaResponses = List.of(peliculaResponse2);
    String queryString = "?titulo=" + peliculaResponse2.getTitulo() + "&"
        + "genero=" + peliculaResponse2.getGenero();
    Optional<String> titulo = Optional.of(peliculaResponse2.getTitulo());
    Optional<String> genero = Optional.of(peliculaResponse2.getGenero());
    var pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());
    var page = new PageImpl<>(peliculaResponses);
    when(peliculasService.findAll(titulo, genero, pageable)).thenReturn(page);

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT + queryString)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson().satisfies(json -> {
          assertThat(json).extractingPath("$.content.length()").isEqualTo(peliculaResponses.size());
          assertThat(json).extractingPath("$.content[0]")
              .convertTo(PeliculaResponseDto.class).isEqualTo(peliculaResponse2);
        });

    // Verify
    verify(peliculasService, only()).findAll(titulo, genero, pageable);
  }


  @Test
  void getById_shouldReturnJsonWithPelicula_whenValidIdProvided() {
    // Arrange
    Long id = peliculaResponse1.getIdPelicula();
    when(peliculasService.findById(id)).thenReturn(peliculaResponse1);

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT + "/" + id.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson()
        .convertTo(PeliculaResponseDto.class)
        .isEqualTo(peliculaResponse1);

    // Verify
    verify(peliculasService, only()).findById(anyLong());

  }

  @Test
  void getById_shouldThrowPeliculaNotFound_whenInvalidIdProvided() {
    // Arrange
    Long id = 3L;
    when(peliculasService.findById(anyLong())).thenThrow(new PeliculaNotFoundException(id));

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT + "/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(result)
        .hasStatus4xxClientError()
        .hasFailed().failure()
        .isInstanceOf(PeliculaNotFoundException.class)
        .hasMessageContaining("no encontrada");

    // Verify
    verify(peliculasService, only()).findById(anyLong());

  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create() {
    // Arrange
    String requestBody = """
           {
              "titulo": "Inception",
              "genero": "Ciencia Ficción",
              "duracion": 148,
              "sinopsis": "Un ladrón que roba secretos del subconsciente",
              "actoresPrincipales": "Leonardo DiCaprio",
              "actoresSecundarios": "Marion Cotillard, Tom Hardy",
              "director": "Christopher Nolan"
           }
           """;

    var peliculaSaved = PeliculaResponseDto.builder()
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

    when(peliculasService.save(any(PeliculaCreateDto.class))).thenReturn(peliculaSaved);

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
        .convertTo(PeliculaResponseDto.class)
        .isEqualTo(peliculaSaved);

    verify(peliculasService, only()).save(any(PeliculaCreateDto.class));


  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_whenBadRequest() {
    // Arrange
    String requestBody = """
           {
              "titulo": "",
              "genero": "Ciencia Ficción",
              "duracion": -1,
              "director": ""
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
              assertThat(path).hasFieldOrProperty("titulo");
              assertThat(path).hasFieldOrProperty("duracion");
              assertThat(path).hasFieldOrProperty("director");
            });

    verify(peliculasService, never()).save(any(PeliculaCreateDto.class));

  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update() {
    // Arrange
    Long id = 1L;
    String requestBody = """
           {
              "titulo": "El Padrino: Parte II"
           }
           """;

    var peliculaSaved = PeliculaResponseDto.builder()
        .idPelicula(1L)
        .titulo("El Padrino: Parte II")
        .genero("Drama")
        .duracion(175)
        .sinopsis("La historia de una familia de la mafia")
        .actoresPrincipales("Marlon Brando, Al Pacino")
        .actoresSecundarios("James Caan, Robert Duvall")
        .director("Francis Ford Coppola")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    when(peliculasService.update(anyLong(), any(PeliculaUpdateDto.class))).thenReturn(peliculaSaved);

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
        .convertTo(PeliculaResponseDto.class)
        .isEqualTo(peliculaSaved);

    verify(peliculasService, only()).update(anyLong(), any(PeliculaUpdateDto.class));

  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_shouldThrowPeliculaNotFound_whenInvalidIdProvided() {
    // Arrange
    Long id = 3L;
    String requestBody = """
           {
              "titulo": "Nuevo Título"
           }
           """;
    when(peliculasService.update(anyLong(), any(PeliculaUpdateDto.class))).thenThrow(new PeliculaNotFoundException(id));

    // Act
    var result = mockMvcTester.put()
        .uri(ENDPOINT + "/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasFailed().failure()
        .isInstanceOf(PeliculaNotFoundException.class)
        .hasMessageContaining("no encontrada");

    // Verify
    verify(peliculasService, only()).update(anyLong(), any(PeliculaUpdateDto.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updatePartial() {
    // Arrange
    Long id = 1L;
    String requestBody = """
           {
              "titulo": "El Padrino: Parte II"
           }
           """;

    var peliculaSaved = PeliculaResponseDto.builder()
        .idPelicula(1L)
        .titulo("El Padrino: Parte II")
        .genero("Drama")
        .duracion(175)
        .sinopsis("La historia de una familia de la mafia")
        .actoresPrincipales("Marlon Brando, Al Pacino")
        .actoresSecundarios("James Caan, Robert Duvall")
        .director("Francis Ford Coppola")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    when(peliculasService.update(anyLong(), any(PeliculaUpdateDto.class))).thenReturn(peliculaSaved);

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
        .convertTo(PeliculaResponseDto.class)
        .isEqualTo(peliculaSaved);

    verify(peliculasService, only()).update(anyLong(), any(PeliculaUpdateDto.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete() {
    // Arrange
    Long id = 1L;
    doNothing().when(peliculasService).deleteById(anyLong());
    // Act
    var result = mockMvcTester.delete()
        .uri(ENDPOINT+ "/" + id)
        .exchange();
    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.NO_CONTENT);

    verify(peliculasService, only()).deleteById(anyLong());

  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_shouldThrowPeliculaNotFound_whenInvalidIdProvided() {
    // Arrange
    Long id = 3L;
    doThrow(new PeliculaNotFoundException(id)).when(peliculasService).deleteById(anyLong());

    // Act
    var result = mockMvcTester.delete()
        .uri(ENDPOINT + "/" + id)
        .exchange();

    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasFailed().failure()
        .isInstanceOf(PeliculaNotFoundException.class)
        .hasMessageContaining("no encontrada");

    // Verify
    verify(peliculasService, only()).deleteById(anyLong());

  }

  @Test
  void getAll_WithSortAscending_ShouldReturnSortedResults() {
    // Arrange
    var peliculaResponses = List.of(peliculaResponse1, peliculaResponse2);
    var pageable = PageRequest.of(0, 10, Sort.by("titulo").ascending());
    var page = new PageImpl<>(peliculaResponses);
    when(peliculasService.findAll(Optional.empty(), Optional.empty(), pageable))
        .thenReturn(page);

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT + "?sortBy=titulo&direction=asc")
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson().satisfies(json -> {
          assertThat(json).extractingPath("$.content.length()").isEqualTo(peliculaResponses.size());
        });

    // Verify
    verify(peliculasService, times(1))
        .findAll(Optional.empty(), Optional.empty(), pageable);
  }

  @Test
  void getAll_WithSortDescending_ShouldReturnSortedResults() {
    // Arrange
    var peliculaResponses = List.of(peliculaResponse2, peliculaResponse1);
    var pageable = PageRequest.of(0, 10, Sort.by("titulo").descending());
    var page = new PageImpl<>(peliculaResponses);
    when(peliculasService.findAll(Optional.empty(), Optional.empty(), pageable))
        .thenReturn(page);

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT + "?sortBy=titulo&direction=desc")
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson().satisfies(json -> {
          assertThat(json).extractingPath("$.content.length()").isEqualTo(peliculaResponses.size());
        });

    // Verify
    verify(peliculasService, times(1))
        .findAll(Optional.empty(), Optional.empty(), pageable);
  }

  @Test
  void getAll_WithPagination_ShouldReturnPagedResults() {
    // Arrange
    var peliculaResponses = List.of(peliculaResponse1);
    var pageable = PageRequest.of(0, 1, Sort.by("idPelicula").ascending());
    var page = new PageImpl<>(peliculaResponses, pageable, 2);
    when(peliculasService.findAll(Optional.empty(), Optional.empty(), pageable))
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
    verify(peliculasService, times(1))
        .findAll(Optional.empty(), Optional.empty(), pageable);
  }
}

