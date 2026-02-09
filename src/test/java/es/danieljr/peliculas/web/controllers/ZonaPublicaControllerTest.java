package es.danieljr.peliculas.web.controllers;

import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
class ZonaPublicaControllerTest {

  @Autowired
  private MockMvcTester mockMvcTester;

  @MockitoBean
  private PeliculasService peliculasService;

  private static final PeliculaResponseDto PELICULA_1 = PeliculaResponseDto.builder()
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
  @DisplayName("GET /public")
  class Index {

    @Test
    @DisplayName("Devuelve vista index con página de películas")
    void returnsIndexWithPage() {
      var pageable = PageRequest.of(0, 4, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_1);
      var page = new PageImpl<>(list, pageable, 1);
      when(peliculasService.findAll(eq(Optional.empty()), eq(Optional.empty()), any()))
          .thenReturn(page);

      var result = mockMvcTester.get()
          .uri("/public")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("index");
      verify(peliculasService).findAll(Optional.empty(), Optional.empty(), pageable);
    }

    @Test
    @DisplayName("GET /public/ devuelve vista index")
    void returnsIndexWithTrailingSlash() {
      var pageable = PageRequest.of(0, 4, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_1);
      var page = new PageImpl<>(list, pageable, 1);
      when(peliculasService.findAll(eq(Optional.empty()), eq(Optional.empty()), any()))
          .thenReturn(page);

      var result = mockMvcTester.get()
          .uri("/public/")
          .exchange();

      assertThat(result).hasStatusOk().viewName().isEqualTo("index");
    }

    @Test
    @DisplayName("GET /public/index devuelve vista index")
    void returnsIndexWithIndexPath() {
      var pageable = PageRequest.of(0, 4, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_1);
      var page = new PageImpl<>(list, pageable, 1);
      when(peliculasService.findAll(eq(Optional.empty()), eq(Optional.empty()), any()))
          .thenReturn(page);

      var result = mockMvcTester.get()
          .uri("/public/index")
          .exchange();

      assertThat(result).hasStatusOk().viewName().isEqualTo("index");
    }

    @Test
    @DisplayName("Acepta paginación page y size")
    void acceptsPaginationParams() {
      var pageable = PageRequest.of(2, 10, Sort.by("idPelicula").ascending());
      var list = Collections.singletonList(PELICULA_1);
      var page = new PageImpl<>(list, pageable, 1);
      when(peliculasService.findAll(eq(Optional.empty()), eq(Optional.empty()), any()))
          .thenReturn(page);

      var result = mockMvcTester.get()
          .uri("/public?page=2&size=10")
          .exchange();

      assertThat(result).hasStatusOk().viewName().isEqualTo("index");
      verify(peliculasService).findAll(Optional.empty(), Optional.empty(), pageable);
    }
  }
}
