package es.danieljr.peliculas.web.controllers;

import es.danieljr.peliculas.web.services.I18nService;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests del controlador de errores (GET /error).
 * Se mockea I18nService para mensajes; se comprueba que se devuelve la vista "error" con el código adecuado (404, 500).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomErrorControllerTest {

  @Autowired
  private MockMvcTester mockMvcTester;

  @MockitoBean
  private I18nService i18nService;

  @Nested
  @DisplayName("GET /error")
  class HandleError {

    @Test
    @DisplayName("GET /error - Sin código de estado devuelve vista error con 500")
    void withoutStatusCode_returnsErrorViewWith500() {
      // Arrange
      when(i18nService.getMessage(eq("error.general"))).thenReturn("Error general");
      when(i18nService.getMessage(eq("error.500"))).thenReturn("Error servidor");

      // Act
      var result = mockMvcTester.get()
          .uri("/error")
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("error");
    }

    @Test
    @DisplayName("GET /error - Con 404 devuelve vista error con mensaje 404")
    void with404_returnsErrorViewWith404Message() {
      // Arrange
      when(i18nService.getMessage(eq("error.404"))).thenReturn("No encontrado");
      when(i18nService.getMessage(anyString())).thenReturn("Error");

      // Act
      var result = mockMvcTester.get()
          .uri("/error")
          .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value())
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("error");
    }

    @Test
    @DisplayName("GET /error - Con 403 devuelve vista error con mensaje 403")
    void with403_returnsErrorViewWith403Message() {
      // Arrange
      when(i18nService.getMessage(eq("error.403"))).thenReturn("Forbidden");
      when(i18nService.getMessage(anyString())).thenReturn("Error");

      // Act
      var result = mockMvcTester.get()
          .uri("/error")
          .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.FORBIDDEN.value())
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("error");
    }

    @Test
    @DisplayName("GET /error - Con 500 devuelve vista error con mensaje 500")
    void with500_returnsErrorViewWith500Message() {
      // Arrange
      when(i18nService.getMessage(eq("error.500"))).thenReturn("Error interno");
      when(i18nService.getMessage(eq("error.general"))).thenReturn("Error general");

      // Act
      var result = mockMvcTester.get()
          .uri("/error")
          .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value())
          .contentType(MediaType.TEXT_HTML)
          .exchange();

      // Assert
      assertThat(result)
          .hasStatusOk()
          .hasViewName("error");
    }
  }
}
