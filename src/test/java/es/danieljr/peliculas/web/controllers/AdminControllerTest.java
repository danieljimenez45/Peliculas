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
    @DisplayName("Devuelve vista admin/peliculas/lista")
    void returnsListaView() {
      var pageable = PageRequest.of(0, 4, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_RESPONSE);
      var page = new PageImpl<>(list, pageable, 1);
      when(peliculasService.findAll(eq(Optional.empty()), eq(Optional.empty()), any()))
          .thenReturn(page);

      var result = mockMvcTester.get()
          .uri("/admin/peliculas")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("admin/peliculas/lista");
      verify(peliculasService).findAll(Optional.empty(), Optional.empty(), pageable);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Sin rol ADMIN devuelve 403")
    void withoutAdminRole_returns403() {
      var result = mockMvcTester.get()
          .uri("/admin/peliculas")
          .exchange();

      assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
      verify(peliculasService, never()).findAll(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("GET /admin/peliculas/filter")
  class FilterPeliculas {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Con titulo devuelve fragmento listaPeliculas")
    void withTitulo_returnsFragment() {
      var pageable = PageRequest.of(0, 4, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_RESPONSE);
      var page = new PageImpl<>(list, pageable, 1);
      when(peliculasService.findAll(eq(Optional.of("Padrino")), eq(Optional.empty()), any()))
          .thenReturn(page);

      var result = mockMvcTester.get()
          .uri("/admin/peliculas/filter?titulo=Padrino")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("fragments/listaPeliculas");
      verify(peliculasService).findAll(Optional.of("Padrino"), Optional.empty(), pageable);
    }
  }

  @Nested
  @DisplayName("GET /admin/peliculas/{id}")
  class GetById {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devuelve vista admin/peliculas/detalle")
    void returnsDetalleView() {
      when(peliculasService.buscarPorId(1L)).thenReturn(Optional.of(PELICULA_ENTITY));

      var result = mockMvcTester.get()
          .uri("/admin/peliculas/1")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("admin/peliculas/detalle");
      verify(peliculasService).buscarPorId(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Película no existe devuelve vista detalle con mensaje")
    void whenNotFound() {
      when(peliculasService.buscarPorId(999L)).thenReturn(Optional.empty());

      var result = mockMvcTester.get()
          .uri("/admin/peliculas/999")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("admin/peliculas/detalle");
      verify(peliculasService).buscarPorId(999L);
    }
  }

  @Nested
  @DisplayName("GET /admin/peliculas/new")
  class NewForm {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devuelve vista admin/peliculas/form")
    void returnsFormView() {
      var result = mockMvcTester.get()
          .uri("/admin/peliculas/new")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("admin/peliculas/form");
    }
  }

  @Nested
  @DisplayName("POST /admin/peliculas/new")
  class NewSubmit {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Datos válidos redirige a /admin/peliculas")
    void validData_redirectsToLista() {
      when(peliculasService.save(any())).thenReturn(PELICULA_RESPONSE);

      var result = mockMvcTester.post()
          .uri("/admin/peliculas/new")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "Nueva")
          .param("genero", "Drama")
          .param("duracion", "120")
          .param("director", "Director")
          .exchange();

      assertThat(result)
          .hasStatus3xxRedirection();
      verify(peliculasService).save(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Datos inválidos devuelve vista form")
    void invalidData_returnsForm() {
      var result = mockMvcTester.post()
          .uri("/admin/peliculas/new")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "")
          .param("genero", "")
          .param("duracion", "-1")
          .param("director", "")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("admin/peliculas/form");
      verify(peliculasService, never()).save(any());
    }
  }

  @Nested
  @DisplayName("GET /admin/peliculas/{id}/edit")
  class EditForm {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Película existe devuelve vista form")
    void whenExists_returnsFormView() {
      when(peliculasService.buscarPorId(1L)).thenReturn(Optional.of(PELICULA_ENTITY));

      var result = mockMvcTester.get()
          .uri("/admin/peliculas/1/edit")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("admin/peliculas/form");
      verify(peliculasService).buscarPorId(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Película no existe redirige a /admin/peliculas/new")
    void whenNotFound_redirectsToNew() {
      when(peliculasService.buscarPorId(999L)).thenReturn(Optional.empty());

      var result = mockMvcTester.get()
          .uri("/admin/peliculas/999/edit")
          .exchange();

      assertThat(result)
          .hasStatus3xxRedirection();
      verify(peliculasService).buscarPorId(999L);
    }
  }

  @Nested
  @DisplayName("POST /admin/peliculas/{id}/edit")
  class EditSubmit {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Datos válidos redirige a detalle")
    void validData_redirectsToDetalle() {
      when(peliculasService.update(eq(1L), any())).thenReturn(PELICULA_RESPONSE);

      var result = mockMvcTester.post()
          .uri("/admin/peliculas/1/edit")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "Actualizado")
          .param("genero", "Drama")
          .param("duracion", "180")
          .param("director", "Director")
          .exchange();

      assertThat(result)
          .hasStatus3xxRedirection();
      verify(peliculasService).update(eq(1L), any());
    }
  }

  @Nested
  @DisplayName("GET /admin/peliculas/{id}/delete/confirm")
  class DeleteConfirm {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Devuelve fragmento deleteModal con token en sesión")
    void returnsDeleteModal() {
      when(peliculasService.buscarPorId(1L)).thenReturn(Optional.of(PELICULA_ENTITY));
      when(i18nService.getMessage(eq("peliculas.borrar.mensaje"), any())).thenReturn("¿Borrar?");
      when(i18nService.getMessage(eq("peliculas.borrar.titulo"))).thenReturn("Confirmar borrado");

      var result = mockMvcTester.get()
          .uri("/admin/peliculas/1/delete/confirm")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("fragments/deleteModal");
      verify(peliculasService).buscarPorId(1L);
      verify(i18nService).getMessage(eq("peliculas.borrar.mensaje"), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Película no existe redirige")
    void whenNotFound_redirects() {
      when(peliculasService.buscarPorId(999L)).thenReturn(Optional.empty());

      var result = mockMvcTester.get()
          .uri("/admin/peliculas/999/delete/confirm")
          .exchange();

      assertThat(result).hasStatus3xxRedirection();
      verify(peliculasService).buscarPorId(999L);
    }
  }

  @Nested
  @DisplayName("POST /admin/peliculas/{id}/delete")
  class DeleteSubmit {

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Sin token correcto redirige con error")
    void withoutValidToken_redirectsWithError() {
      var result = mockMvcTester.post()
          .uri("/admin/peliculas/1/delete")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("deleteToken", "invalid-token")
          .exchange();

      assertThat(result)
          .hasStatus3xxRedirection();
      verify(peliculasService, never()).deleteById(anyLong());
    }
  }
}
