package es.danieljr.peliculas.web.controllers;

import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import es.danieljr.peliculas.rest.peliculas.services.PeliculasService;
import es.danieljr.peliculas.web.services.I18nService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests del controlador MVC de administración (rutas /admin/**).
 * Se mockean PeliculasService e I18nService; se comprueban vistas (viewName) y redirecciones.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

  @Autowired
  private MockMvcTester mockMvcTester;

  @MockitoBean
  private PeliculasService peliculasService;

  @MockitoBean
  private I18nService i18nService;

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

  private static final Pelicula PELICULA_ENTITY = Pelicula.builder()
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
  @DisplayName("GET /admin/peliculas")
  class ListaPeliculas {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/peliculas - Devuelve vista admin/peliculas/lista")
    void returnsListaView() {
      // Arrange
      var pageable = PageRequest.of(0, 4, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_RESPONSE);
      var page = new PageImpl<>(list, pageable, 1);
      when(peliculasService.findAll(eq(Optional.empty()), eq(Optional.empty()), any()))
          .thenReturn(page);

      // Act
      var result = mockMvcTester.get()
          .uri("/admin/peliculas")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("admin/peliculas/lista")
          .model()
          .containsKeys("page");

      // Verify
      verify(peliculasService, times(1)).findAll(Optional.empty(), Optional.empty(), pageable);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /admin/peliculas - Sin rol ADMIN devuelve 403")
    void withoutAdminRole_returns403() {
      // Act
      var result = mockMvcTester.get()
          .uri("/admin/peliculas")
          .exchange();

      // Assert
      assertThat(result).hasStatus(HttpStatus.FORBIDDEN);

      // Verify
      verify(peliculasService, never()).findAll(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("GET /admin/peliculas/filter")
  class FilterPeliculas {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/peliculas/filter - Con titulo devuelve fragmento listaPeliculas")
    void withTitulo_returnsFragment() {
      // Arrange
      var pageable = PageRequest.of(0, 4, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_RESPONSE);
      var page = new PageImpl<>(list, pageable, 1);
      when(peliculasService.findAll(eq(Optional.of("Padrino")), eq(Optional.empty()), any()))
          .thenReturn(page);

      // Act
      var result = mockMvcTester.get()
          .uri("/admin/peliculas/filter?titulo=Padrino")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("fragments/listaPeliculas");

      // Verify
      verify(peliculasService, times(1)).findAll(Optional.of("Padrino"), Optional.empty(), pageable);
    }
  }

  @Nested
  @DisplayName("GET /admin/peliculas/{id}")
  class GetById {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/peliculas/{id} - Devuelve vista admin/peliculas/detalle")
    void returnsDetalleView() {
      // Arrange
      Long id = 1L;
      when(peliculasService.buscarPorId(id)).thenReturn(Optional.of(PELICULA_ENTITY));

      // Act
      var result = mockMvcTester.get()
          .uri("/admin/peliculas/" + id)
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      var mvcAssert = assertThat(result)
          .hasStatusOk()
          .hasViewName("admin/peliculas/detalle");
      mvcAssert.model()
          .containsKeys("pelicula")
          .containsEntry("pelicula", PELICULA_ENTITY);

      // Verify
      verify(peliculasService, only()).buscarPorId(id);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/peliculas/{id} - Película no existe devuelve vista detalle")
    void whenNotFound() {
      // Arrange
      when(peliculasService.buscarPorId(999L)).thenReturn(Optional.empty());

      // Act
      var result = mockMvcTester.get()
          .uri("/admin/peliculas/999")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("admin/peliculas/detalle");

      // Verify
      verify(peliculasService, only()).buscarPorId(999L);
    }
  }

  @Nested
  @DisplayName("GET /admin/peliculas/new")
  class NewForm {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/peliculas/new - Devuelve vista admin/peliculas/form")
    void returnsFormView() {
      // Act
      var result = mockMvcTester.get()
          .uri("/admin/peliculas/new")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("admin/peliculas/form");
    }
  }

  @Nested
  @DisplayName("POST /admin/peliculas/new")
  class NewSubmit {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/peliculas/new - Datos válidos redirige a /admin/peliculas")
    void validData_redirectsToLista() {
      // Arrange
      when(peliculasService.save(any())).thenReturn(PELICULA_RESPONSE);

      // Act
      var result = mockMvcTester.post()
          .uri("/admin/peliculas/new")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "Nueva")
          .param("genero", "Drama")
          .param("duracion", "120")
          .param("director", "Director")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(peliculasService, times(1)).save(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/peliculas/new - Datos inválidos devuelve vista form")
    void invalidData_returnsForm() {
      // Act
      var result = mockMvcTester.post()
          .uri("/admin/peliculas/new")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "")
          .param("genero", "")
          .param("duracion", "-1")
          .param("director", "")
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("admin/peliculas/form");

      // Verify
      verify(peliculasService, never()).save(any());
    }
  }

  @Nested
  @DisplayName("GET /admin/peliculas/{id}/edit")
  class EditForm {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/peliculas/{id}/edit - Película existe devuelve vista form")
    void whenExists_returnsFormView() {
      // Arrange
      when(peliculasService.buscarPorId(1L)).thenReturn(Optional.of(PELICULA_ENTITY));

      // Act
      var result = mockMvcTester.get()
          .uri("/admin/peliculas/1/edit")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("admin/peliculas/form");

      // Verify
      verify(peliculasService, only()).buscarPorId(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/peliculas/{id}/edit - Película no existe redirige a /admin/peliculas/new")
    void whenNotFound_redirectsToNew() {
      // Arrange
      when(peliculasService.buscarPorId(999L)).thenReturn(Optional.empty());

      // Act
      var result = mockMvcTester.get()
          .uri("/admin/peliculas/999/edit")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(peliculasService, only()).buscarPorId(999L);
    }
  }

  @Nested
  @DisplayName("POST /admin/peliculas/{id}/edit")
  class EditSubmit {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/peliculas/{id}/edit - Datos válidos redirige a detalle")
    void validData_redirectsToDetalle() {
      // Arrange
      when(peliculasService.update(eq(1L), any())).thenReturn(PELICULA_RESPONSE);

      // Act
      var result = mockMvcTester.post()
          .uri("/admin/peliculas/1/edit")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "Actualizado")
          .param("genero", "Drama")
          .param("duracion", "180")
          .param("director", "Director")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(peliculasService, times(1)).update(eq(1L), any());
    }
  }

  @Nested
  @DisplayName("GET /admin/peliculas/{id}/delete/confirm")
  class DeleteConfirm {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/peliculas/{id}/delete/confirm - Devuelve fragmento deleteModal")
    void returnsDeleteModal() {
      // Arrange
      when(peliculasService.buscarPorId(1L)).thenReturn(Optional.of(PELICULA_ENTITY));
      when(i18nService.getMessage(eq("peliculas.borrar.mensaje"), any())).thenReturn("¿Borrar?");
      when(i18nService.getMessage(eq("peliculas.borrar.titulo"))).thenReturn("Confirmar borrado");

      // Act
      var result = mockMvcTester.get()
          .uri("/admin/peliculas/1/delete/confirm")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("fragments/deleteModal");

      // Verify
      verify(peliculasService, times(1)).buscarPorId(1L);
      verify(i18nService).getMessage(eq("peliculas.borrar.mensaje"), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /admin/peliculas/{id}/delete/confirm - Película no existe redirige")
    void whenNotFound_redirects() {
      // Arrange
      when(peliculasService.buscarPorId(999L)).thenReturn(Optional.empty());

      // Act
      var result = mockMvcTester.get()
          .uri("/admin/peliculas/999/delete/confirm")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(peliculasService, only()).buscarPorId(999L);
    }
  }

  @Nested
  @DisplayName("POST /admin/peliculas/{id}/delete")
  class DeleteSubmit {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /admin/peliculas/{id}/delete - Sin token correcto redirige con error")
    void withoutValidToken_redirectsWithError() {
      // Act
      var result = mockMvcTester.post()
          .uri("/admin/peliculas/1/delete")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("deleteToken", "invalid-token")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(peliculasService, never()).deleteById(anyLong());
    }
  }
}
