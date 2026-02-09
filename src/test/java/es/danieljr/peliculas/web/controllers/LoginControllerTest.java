package es.danieljr.peliculas.web.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
class LoginControllerTest {

  @Autowired
  private MockMvcTester mockMvcTester;

  @Nested
  @DisplayName("GET /")
  class Welcome {

    @Test
    @DisplayName("Redirige a /public/")
    void redirectsToPublic() {
      var result = mockMvcTester.get()
          .uri("/")
          .exchange();

      assertThat(result).hasStatus(HttpStatus.FOUND);
    }
  }

  @Nested
  @DisplayName("GET /auth/login")
  class LoginPage {

    @Test
    @DisplayName("Devuelve vista login sin parámetro error")
    void returnsLoginViewWithoutError() {
      var result = mockMvcTester.get()
          .uri("/auth/login")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("login");
    }

    @Test
    @DisplayName("Devuelve vista login con parámetro error=true")
    void returnsLoginViewWithError() {
      var result = mockMvcTester.get()
          .uri("/auth/login?error=true")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("login");
    }
  }
}
