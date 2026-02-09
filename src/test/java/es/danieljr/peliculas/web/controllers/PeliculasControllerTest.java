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
    @DisplayName("Devuelve vista peliculas/detalle cuando existe")
    void whenExists_returnsDetalleView() {
      when(peliculasService.findById(1L)).thenReturn(PELICULA_RESPONSE);

      var result = mockMvcTester.get()
          .uri("/peliculas/1")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("peliculas/detalle");
      verify(peliculasService).findById(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("Cuando findById devuelve null puede devolver vista o error")
    void whenNotFound() {
      when(peliculasService.findById(999L)).thenReturn(null);

      var result = mockMvcTester.get()
          .uri("/peliculas/999")
          .exchange();

      assertThat(result).hasStatusOk().viewName().isEqualTo("peliculas/detalle");
      verify(peliculasService).findById(999L);
    }
  }

  @Nested
  @DisplayName("GET /peliculas lista")
  class Lista {

    @Test
    @WithMockUser
    @DisplayName("GET /peliculas devuelve vista peliculas/lista")
    void returnsListaView() {
      var pageable = PageRequest.of(0, 4, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_RESPONSE);
      var page = new PageImpl<>(list, pageable, 1);
      when(peliculasService.findAll(eq(Optional.empty()), eq(Optional.empty()), any()))
          .thenReturn(page);

      var result = mockMvcTester.get()
          .uri("/peliculas")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("peliculas/lista");
      verify(peliculasService).findAll(Optional.empty(), Optional.empty(), pageable);
    }

    @Test
    @WithMockUser
    @DisplayName("GET /peliculas/lista con paginación")
    void withPagination() {
      var pageable = PageRequest.of(1, 10, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_RESPONSE);
      var page = new PageImpl<>(list, pageable, 25);
      when(peliculasService.findAll(eq(Optional.empty()), eq(Optional.empty()), any()))
          .thenReturn(page);

      var result = mockMvcTester.get()
          .uri("/peliculas/lista?page=1&size=10")
          .exchange();

      assertThat(result).hasStatusOk().viewName().isEqualTo("peliculas/lista");
      verify(peliculasService).findAll(Optional.empty(), Optional.empty(), pageable);
    }
  }

  @Nested
  @DisplayName("GET /peliculas/new")
  class NewForm {

    @Test
    @WithMockUser
    @DisplayName("Devuelve vista form para nueva película")
    void returnsFormView() {
      var result = mockMvcTester.get()
          .uri("/peliculas/new")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("/peliculas/form");
      verify(peliculasService, never()).save(any());
    }
  }

  @Nested
  @DisplayName("POST /peliculas/new")
  class NewSubmit {

    @Test
    @WithMockUser
    @DisplayName("Datos válidos redirige a /peliculas/lista")
    void validData_redirectsToLista() {
      when(peliculasService.save(any(PeliculaCreateDto.class))).thenReturn(PELICULA_RESPONSE);

      var result = mockMvcTester.post()
          .uri("/peliculas/new")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "Nueva Película")
          .param("genero", "Drama")
          .param("duracion", "120")
          .param("director", "Director")
          .exchange();

      assertThat(result).hasStatus3xxRedirection();
      verify(peliculasService).save(any(PeliculaCreateDto.class));
    }

    @Test
    @WithMockUser
    @DisplayName("Datos inválidos devuelve vista form")
    void invalidData_returnsForm() {
      var result = mockMvcTester.post()
          .uri("/peliculas/new")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "")
          .param("genero", "")
          .param("duracion", "-1")
          .param("director", "")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("/peliculas/form");
      verify(peliculasService, never()).save(any(PeliculaCreateDto.class));
    }
  }

  @Nested
  @DisplayName("GET /peliculas/{id}/edit")
  class EditForm {

    @Test
    @WithMockUser
    @DisplayName("Película existe devuelve vista form en modo edición")
    void whenExists_returnsFormView() {
      when(peliculasService.findById(1L)).thenReturn(PELICULA_RESPONSE);

      var result = mockMvcTester.get()
          .uri("/peliculas/1/edit")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("peliculas/form");
      verify(peliculasService).findById(1L);
    }

    @Test
    @WithMockUser
    @DisplayName("Película no existe redirige a /peliculas/new")
    void whenNotFound_redirectsToNew() {
      when(peliculasService.findById(999L)).thenReturn(null);

      var result = mockMvcTester.get()
          .uri("/peliculas/999/edit")
          .exchange();

      assertThat(result).hasStatus3xxRedirection();
      verify(peliculasService).findById(999L);
    }
  }

  @Nested
  @DisplayName("POST /peliculas/{id}/edit")
  class EditSubmit {

    @Test
    @WithMockUser
    @DisplayName("Datos válidos redirige a detalle")
    void validData_redirectsToDetalle() {
      when(peliculasService.update(eq(1L), any(PeliculaUpdateDto.class))).thenReturn(PELICULA_RESPONSE);

      var result = mockMvcTester.post()
          .uri("/peliculas/1/edit")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "Título Actualizado")
          .param("genero", "Drama")
          .param("duracion", "180")
          .param("director", "Director")
          .exchange();

      assertThat(result).hasStatus3xxRedirection();
      verify(peliculasService).update(eq(1L), any(PeliculaUpdateDto.class));
    }

    @Test
    @WithMockUser
    @DisplayName("Datos inválidos devuelve vista form")
    void invalidData_returnsForm() {
      var result = mockMvcTester.post()
          .uri("/peliculas/1/edit")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("titulo", "")
          .param("genero", "")
          .param("duracion", "-1")
          .param("director", "")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("/peliculas/form");
      verify(peliculasService, never()).update(anyLong(), any(PeliculaUpdateDto.class));
    }
  }

  @Nested
  @DisplayName("GET /peliculas/{id}/delete")
  class Delete {

    @Test
    @WithMockUser
    @DisplayName("Borrado redirige a /peliculas/lista")
    void redirectsToLista() {
      doNothing().when(peliculasService).deleteById(1L);

      var result = mockMvcTester.get()
          .uri("/peliculas/1/delete")
          .exchange();

      assertThat(result).hasStatus3xxRedirection();
      verify(peliculasService).deleteById(1L);
    }
  }

  @Nested
  @DisplayName("Acceso sin autenticación")
  class Unauthenticated {

    @Test
    @DisplayName("GET /peliculas requiere autenticación")
    void getLista_requiresAuth() {
      var result = mockMvcTester.get()
          .uri("/peliculas")
          .exchange();

      assertThat(result).hasStatus3xxRedirection();
    }

    @Test
    @DisplayName("GET /peliculas/new requiere autenticación")
    void getNew_requiresAuth() {
      var result = mockMvcTester.get()
          .uri("/peliculas/new")
          .exchange();

      assertThat(result).hasStatus3xxRedirection();
    }
  }
}
