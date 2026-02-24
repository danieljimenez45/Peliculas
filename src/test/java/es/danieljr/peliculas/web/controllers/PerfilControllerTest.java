package es.danieljr.peliculas.web.controllers;

import es.danieljr.peliculas.rest.users.models.Role;
import es.danieljr.peliculas.rest.users.models.User;
import es.danieljr.peliculas.rest.users.services.UsersService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests del controlador de perfil de usuario (GET /app/perfil).
 * Se mockea UsersService; se comprueba acceso con/sin autenticación y que se pasa el usuario a la vista.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PerfilControllerTest {

  @Autowired
  private MockMvcTester mockMvcTester;

  @MockitoBean
  private UsersService usersService;

  private static final User USER_ADMIN = User.builder()
      .id(1L)
      .nombre("Admin")
      .apellidos("Admin")
      .username("admin")
      .email("admin@prueba.net")
      .password("$2a$10$xxx")
      .roles(new HashSet<>(java.util.Arrays.asList(Role.ADMIN, Role.USER)))
      .build();

  @Nested
  @DisplayName("GET /app/perfil")
  class ShowProfile {

    @Test
    @DisplayName("GET /app/perfil - Sin autenticación devuelve redirección")
    void withoutAuth_redirectsOrForbidden() {
      // Act
      var result = mockMvcTester.get()
          .uri("/app/perfil")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(usersService, never()).findByUsername(anyString());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("GET /app/perfil - Con autenticación devuelve vista app/perfil")
    void withAuth_returnsProfileView() {
      // Arrange
      when(usersService.findByUsername("admin")).thenReturn(Optional.of(USER_ADMIN));

      // Act
      var result = mockMvcTester.get()
          .uri("/app/perfil")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      var mvcAssert = assertThat(result)
          .hasStatusOk()
          .hasViewName("app/perfil");
      mvcAssert.model().containsKeys("usuario").containsEntry("usuario", USER_ADMIN);

      // Verify
      verify(usersService, only()).findByUsername("admin");
    }

    @Test
    @WithMockUser(username = "unknown")
    @DisplayName("GET /app/perfil - Usuario no encontrado devuelve vista app/perfil")
    void userNotFound_returnsViewWithNullUser() {
      // Arrange
      when(usersService.findByUsername("unknown")).thenReturn(Optional.empty());

      // Act
      var result = mockMvcTester.get()
          .uri("/app/perfil")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result).hasStatusOk().hasViewName("app/perfil");

      // Verify
      verify(usersService, only()).findByUsername("unknown");
    }
  }

  @Nested
  @DisplayName("POST /app/perfil/edit")
  class UpdateProfile {

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /app/perfil/edit - Actualización correcta redirige a /app/perfil")
    void validUpdate_redirectsToProfile() {
      // Arrange
      when(usersService.findByUsername("admin")).thenReturn(Optional.of(USER_ADMIN));

      // Act
      var result = mockMvcTester.post()
          .uri("/app/perfil/edit")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("nombre", "AdminUpdated")
          .param("apellidos", "ApellidosUpdated")
          .param("username", "admin")
          .param("email", "admin@prueba.net")
          .param("password", "Admin1")
          .exchange();

      // Assert
      assertThat(result).hasStatus3xxRedirection();

      // Verify
      verify(usersService, times(1)).findByUsername("admin");
      verify(usersService, times(1)).save(any(User.class));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("POST /app/perfil/edit - Validación fallida devuelve vista app/perfil")
    void validationErrors_returnsProfileView() {
      // Act
      var result = mockMvcTester.post()
          .uri("/app/perfil/edit")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("nombre", "")
          .param("apellidos", "")
          .param("username", "admin")
          .param("email", "admin@prueba.net")
          .param("password", "Admin1")
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("app/perfil");

      // Verify: con errores de validación no se llama a findByUsername ni save
      verify(usersService, never()).findByUsername(anyString());
      verify(usersService, never()).save(any(User.class));
    }
  }
}
