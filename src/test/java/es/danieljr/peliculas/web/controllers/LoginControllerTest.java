package es.danieljr.peliculas.web.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del controlador de login (rutas /, /auth/login).
 * Comprueba redirecciones (GET / → /public/) y que la vista de login se devuelve correctamente.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginControllerTest {

  @Autowired
  private MockMvcTester mockMvcTester;

  @Nested
  @DisplayName("GET /")
  class Welcome {

    @Test
    @DisplayName("GET / - Redirige a /public/")
    void redirectsToPublic() {
      // Act
      var result = mockMvcTester.get()
          .uri("/")
          .exchange();

      // Assert
      assertThat(result).hasStatus(HttpStatus.FOUND);
    }
  }

  @Nested
  @DisplayName("GET /auth/login")
  class LoginPage {

    @Test
    @DisplayName("GET /auth/login - Devuelve vista login sin parámetro error")
    void returnsLoginViewWithoutError() {
      // Act
      var result = mockMvcTester.get()
          .uri("/auth/login")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("login");
    }

    @Test
    @DisplayName("GET /auth/login - Devuelve vista login con parámetro error=true")
    void returnsLoginViewWithError() {
      // Act
      var result = mockMvcTester.get()
          .uri("/auth/login?error=true")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("login");
    }
  }
}
