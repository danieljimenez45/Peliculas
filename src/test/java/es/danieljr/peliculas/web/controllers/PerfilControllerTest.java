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
    @DisplayName("Sin autenticación devuelve 302 o 403")
    void withoutAuth_redirectsOrForbidden() {
      var result = mockMvcTester.get()
          .uri("/app/perfil")
          .exchange();

      assertThat(result).hasStatus3xxRedirection();
      verify(usersService, never()).findByUsername(anyString());
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("Con autenticación devuelve vista app/perfil")
    void withAuth_returnsProfileView() {
      when(usersService.findByUsername("admin")).thenReturn(Optional.of(USER_ADMIN));

      var result = mockMvcTester.get()
          .uri("/app/perfil")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("app/perfil");
      verify(usersService).findByUsername("admin");
    }

    @Test
    @WithMockUser(username = "unknown")
    @DisplayName("Usuario no encontrado devuelve vista con usuario null")
    void userNotFound_returnsViewWithNullUser() {
      when(usersService.findByUsername("unknown")).thenReturn(Optional.empty());

      var result = mockMvcTester.get()
          .uri("/app/perfil")
          .exchange();

      assertThat(result).hasStatusOk().viewName().isEqualTo("app/perfil");
      verify(usersService).findByUsername("unknown");
    }
  }

  @Nested
  @DisplayName("POST /app/perfil/edit")
  class UpdateProfile {

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("Actualización correcta redirige a /app/perfil")
    void validUpdate_redirectsToProfile() {
      when(usersService.findByUsername("admin")).thenReturn(Optional.of(USER_ADMIN));

      var result = mockMvcTester.post()
          .uri("/app/perfil/edit")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("nombre", "AdminUpdated")
          .param("apellidos", "ApellidosUpdated")
          .param("username", "admin")
          .param("email", "admin@prueba.net")
          .param("password", "Admin1")
          .exchange();

      assertThat(result).hasStatus3xxRedirection();
      verify(usersService).findByUsername("admin");
      verify(usersService).save(any(User.class));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("Validación fallida devuelve vista app/perfil con errores")
    void validationErrors_returnsProfileView() {
      var result = mockMvcTester.post()
          .uri("/app/perfil/edit")
          .with(SecurityMockMvcRequestPostProcessors.csrf())
          .param("nombre", "")
          .param("apellidos", "")
          .param("username", "admin")
          .param("email", "admin@prueba.net")
          .param("password", "Admin1")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("app/perfil");
      // Cuando hay errores de validación el controlador hace return antes de llamar a
      // findByUsername
      verify(usersService, never()).findByUsername(anyString());
      verify(usersService, never()).save(any(User.class));
    }
  }
}
