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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
    @DisplayName("Sin código de estado devuelve vista error con 500")
    void withoutStatusCode_returnsErrorViewWith500() {
      when(i18nService.getMessage(eq("error.general"))).thenReturn("Error general");
      when(i18nService.getMessage(eq("error.500"))).thenReturn("Error servidor");

      var result = mockMvcTester.get()
          .uri("/error")
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("error");
    }

    @Test
    @DisplayName("Con 404 devuelve vista error con mensaje 404")
    void with404_returnsErrorViewWith404Message() {
      when(i18nService.getMessage(eq("error.404"))).thenReturn("No encontrado");
      when(i18nService.getMessage(anyString())).thenReturn("Error");

      var result = mockMvcTester.get()
          .uri("/error")
          .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value())
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("error");
    }

    @Test
    @DisplayName("Con 403 devuelve vista error con mensaje 403")
    void with403_returnsErrorViewWith403Message() {
      when(i18nService.getMessage(eq("error.403"))).thenReturn("Forbidden");
      when(i18nService.getMessage(anyString())).thenReturn("Error");

      var result = mockMvcTester.get()
          .uri("/error")
          .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.FORBIDDEN.value())
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("error");
    }

    @Test
    @DisplayName("Con 500 devuelve vista error con mensaje 500")
    void with500_returnsErrorViewWith500Message() {
      when(i18nService.getMessage(eq("error.500"))).thenReturn("Error interno");
      when(i18nService.getMessage(eq("error.general"))).thenReturn("Error general");

      var result = mockMvcTester.get()
          .uri("/error")
          .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value())
          .exchange();

      assertThat(result)
          .hasStatusOk()
          .viewName().isEqualTo("error");
    }
  }
}
