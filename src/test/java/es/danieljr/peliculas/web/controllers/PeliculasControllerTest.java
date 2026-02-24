package es.danieljr.peliculas.web.controllers;

import es.danieljr.peliculas.rest.peliculas.dto.PeliculaCreateDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaUpdateDto;
import es.danieljr.peliculas.rest.peliculas.services.PeliculasService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests del controlador MVC de películas (rutas /peliculas: listado, detalle, formularios new/edit, delete).
 * Se mockea PeliculasService; se comprueban vistas, redirecciones y que el modelo contiene los datos correctos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PeliculasControllerTest {

  @Autowired
  private MockMvcTester mockMvcTester;

  @MockitoBean
  private PeliculasService peliculasService;

  private static final PeliculaResponseDto PELICULA_RESPONSE = PeliculaResponseDto.builder()
      .idPelicula(1L)
      .titulo("El Padrino")
      .genero("Drama")
      .duracion(175)
      .sinopsis("Sinopsis")
      .actoresPrincipales("Marlon Brando")
      .actoresSecundarios("Al Pacino")
      .director("Francis Ford Coppola")
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();

  @Nested
  @DisplayName("GET /peliculas/{id}")
  class GetById {

    @Test
    @WithMockUser
    @DisplayName("GET /peliculas/{id} - Devuelve vista peliculas/detalle cuando existe")
    void whenExists_returnsDetalleView() {
      // Arrange
      Long id = 1L;
      when(peliculasService.findById(id)).thenReturn(PELICULA_RESPONSE);

      // Act
      var result = mockMvcTester.get()
          .uri("/peliculas/" + id)
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      var mvcAssert = assertThat(result)
          .hasStatusOk()
          .hasViewName("peliculas/detalle");
      mvcAssert.model()
          .containsKeys("pelicula")
          .containsEntry("pelicula", PELICULA_RESPONSE);
      mvcAssert.bodyText()
          .contains(PELICULA_RESPONSE.getTitulo());

      // Verify
      verify(peliculasService, only()).findById(id);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /peliculas/{id} - Cuando findById devuelve null devuelve vista detalle")
    void whenNotFound() {
      // Arrange
      when(peliculasService.findById(999L)).thenReturn(null);

      // Act
      var result = mockMvcTester.get()
          .uri("/peliculas/999")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result).hasStatusOk().hasViewName("peliculas/detalle");

      // Verify
      verify(peliculasService, only()).findById(999L);
    }
  }

  @Nested
  @DisplayName("GET /peliculas lista")
  class Lista {

    @Test
    @WithMockUser
    @DisplayName("GET /peliculas - Devuelve vista peliculas/lista con página de películas")
    void returnsListaView() {
      // Arrange
      var pageable = PageRequest.of(0, 4, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_RESPONSE);
      var page = new PageImpl<>(list, pageable, 1);
      when(peliculasService.findAll(eq(Optional.empty()), eq(Optional.empty()), any()))
          .thenReturn(page);

      // Act
      var result = mockMvcTester.get()
          .uri("/peliculas")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("peliculas/lista")
          .model()
          .containsKeys("page")
          .hasEntrySatisfying("page", pageObj -> assertThat(pageObj).isNotNull());

      // Verify
      verify(peliculasService, times(1)).findAll(Optional.empty(), Optional.empty(), pageable);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /peliculas/lista - Con paginación page y size")
    void withPagination() {
      // Arrange
      var pageable = PageRequest.of(1, 10, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_RESPONSE);
      var page = new PageImpl<>(list, pageable, 25);
      when(peliculasService.findAll(eq(Optional.empty()), eq(Optional.empty()), any()))
          .thenReturn(page);

      // Act
      var result = mockMvcTester.get()
          .uri("/peliculas/lista?page=1&size=10")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result).hasStatusOk().hasViewName("peliculas/lista");

      // Verify
      verify(peliculasService, times(1)).findAll(Optional.empty(), Optional.empty(), pageable);
    }
  }

  @Nested
  @DisplayName("GET /peliculas/new")
  class NewForm {

    @Test
    @WithMockUser
    @DisplayName("GET /peliculas/new - Devuelve vista form para nueva película")
    void returnsFormView() {
      // Act
      var result = mockMvcTester.get()
          .uri("/peliculas/new")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("/peliculas/form");

      // Verify
      verify(peliculasService, never()).save(any());
    }
  }

  @Nested
  @DisplayName("POST /peliculas/new")
  class NewSubmit {

    @Test
    @WithMockUser
    @DisplayName("POST /peliculas/new - Datos válidos redirige a /peliculas/lista")
    void validData_redirectsToLista() {
      // Arrange
      when(peliculasService.save(any(PeliculaCreateDto.class))).thenReturn(PELICULA_RESPONSE);

      // Act
      var result = mockMvcTester.post()
          .uri("/peliculas/new")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "Nueva Película")
          .param("genero", "Drama")
          .param("duracion", "120")
          .param("director", "Director")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(peliculasService, times(1)).save(any(PeliculaCreateDto.class));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /peliculas/new - Datos inválidos devuelve vista form")
    void invalidData_returnsForm() {
      // Act
      var result = mockMvcTester.post()
          .uri("/peliculas/new")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "")
          .param("genero", "")
          .param("duracion", "-1")
          .param("director", "")
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("/peliculas/form");

      // Verify
      verify(peliculasService, never()).save(any(PeliculaCreateDto.class));
    }
  }

  @Nested
  @DisplayName("GET /peliculas/{id}/edit")
  class EditForm {

    @Test
    @WithMockUser
    @DisplayName("GET /peliculas/{id}/edit - Película existe devuelve vista form en modo edición")
    void whenExists_returnsFormView() {
      // Arrange
      when(peliculasService.findById(1L)).thenReturn(PELICULA_RESPONSE);

      // Act
      var result = mockMvcTester.get()
          .uri("/peliculas/1/edit")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("peliculas/form");

      // Verify
      verify(peliculasService, only()).findById(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /peliculas/{id}/edit - Película no existe redirige a /peliculas/new")
    void whenNotFound_redirectsToNew() {
      // Arrange
      when(peliculasService.findById(999L)).thenReturn(null);

      // Act
      var result = mockMvcTester.get()
          .uri("/peliculas/999/edit")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(peliculasService, only()).findById(999L);
    }
  }

  @Nested
  @DisplayName("POST /peliculas/{id}/edit")
  class EditSubmit {

    @Test
    @WithMockUser
    @DisplayName("POST /peliculas/{id}/edit - Datos válidos redirige a detalle")
    void validData_redirectsToDetalle() {
      // Arrange
      when(peliculasService.update(eq(1L), any(PeliculaUpdateDto.class))).thenReturn(PELICULA_RESPONSE);

      // Act
      var result = mockMvcTester.post()
          .uri("/peliculas/1/edit")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "Título Actualizado")
          .param("genero", "Drama")
          .param("duracion", "180")
          .param("director", "Director")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(peliculasService, times(1)).update(eq(1L), any(PeliculaUpdateDto.class));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /peliculas/{id}/edit - Datos inválidos devuelve vista form")
    void invalidData_returnsForm() {
      // Act
      var result = mockMvcTester.post()
          .uri("/peliculas/1/edit")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "")
          .param("genero", "")
          .param("duracion", "-1")
          .param("director", "")
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("/peliculas/form");

      // Verify
      verify(peliculasService, never()).update(anyLong(), any(PeliculaUpdateDto.class));
    }
  }

  @Nested
  @DisplayName("GET /peliculas/{id}/delete")
  class Delete {

    @Test
    @WithMockUser
    @DisplayName("GET /peliculas/{id}/delete - Borrado redirige a /peliculas/lista")
    void redirectsToLista() {
      // Arrange
      doNothing().when(peliculasService).deleteById(1L);

      // Act
      var result = mockMvcTester.get()
          .uri("/peliculas/1/delete")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(peliculasService, only()).deleteById(1L);
    }
  }

  @Nested
  @DisplayName("Acceso sin autenticación")
  class Unauthenticated {

    @Test
    @DisplayName("GET /peliculas - Requiere autenticación")
    void getLista_requiresAuth() {
      // Act
      var result = mockMvcTester.get()
          .uri("/peliculas")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();
    }

    @Test
    @DisplayName("GET /peliculas/new - Requiere autenticación")
    void getNew_requiresAuth() {
      // Act
      var result = mockMvcTester.get()
          .uri("/peliculas/new")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();
    }
  }
}
