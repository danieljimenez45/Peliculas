package es.danieljr.peliculas.rest.auth.controllers;

import es.danieljr.peliculas.rest.auth.dto.JwtAuthResponse;
import es.danieljr.peliculas.rest.auth.dto.UserSignInRequest;
import es.danieljr.peliculas.rest.auth.dto.UserSignUpRequest;
import es.danieljr.peliculas.rest.auth.exceptions.AuthDifferentPasswords;
import es.danieljr.peliculas.rest.auth.exceptions.AuthExistingUsernameOrEmail;
import es.danieljr.peliculas.rest.auth.exceptions.AuthSignInNotValid;
import es.danieljr.peliculas.rest.auth.services.authentication.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests del controlador REST de autenticación (login, registro).
 * Se mockea AuthenticationService; se prueban POST /signup y POST /signin y casos de error.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationRestControllerTest {

  private final String ENDPOINT = "/api/v1/auth";

  @Autowired
  private MockMvcTester mockMvcTester;

  @MockitoBean
  private AuthenticationService authenticationService;

  /** POST /auth/signup con datos válidos: servicio devuelve JWT; respuesta 200 con token. */
  @Test
  void signUp() {
    String requestBody = """
          {
           "nombre": "Test",
           "apellidos": "Test",
           "username": "test2",
           "email": "test@test.com",
           "password": "12345",
           "passwordComprobacion": "12345"
           }
          """;

    var jwtAuthResponse = JwtAuthResponse.builder().token("token").build();
    // Arrange
    when(authenticationService.signUp(any(UserSignUpRequest.class))).thenReturn(jwtAuthResponse);

    // Llamada al endpoint
    var result = mockMvcTester.post()
        .uri(ENDPOINT + "/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson()
        .convertTo(JwtAuthResponse.class)
        .isEqualTo(jwtAuthResponse);

    // Verify
    verify(authenticationService, times(1)).signUp(any(UserSignUpRequest.class));
  }

  /** POST /signup con contraseñas distintas: servicio lanza AuthDifferentPasswords; respuesta 400. */
  @Test
  void signUp_WhenPasswordsDoNotMatch_ShouldThrowException() {
    String requestBody = """
          {
           "nombre": "Test",
           "apellidos": "Test",
           "username": "test2",
           "email": "test@test.com",
           "password": "12345",
           "passwordComprobacion": "54321"
           }
          """;

    // Mock del servicio
    when(authenticationService.signUp(any(UserSignUpRequest.class)))
        .thenThrow(new AuthDifferentPasswords("Las contraseñas no coinciden"));

    // Llamada al endpoint
    var result = mockMvcTester.post()
        .uri(ENDPOINT + "/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasFailed().failure()
        .isInstanceOf(AuthDifferentPasswords.class)
        .hasMessageContaining("no coinciden");

    // Verify
    verify(authenticationService, times(1)).signUp(any(UserSignUpRequest.class));
  }

  @Test
  void signUp_WhenUsernameOrEmailAlreadyExist_ShouldThrowException() {
    String requestBody = """
          {
           "nombre": "Test",
           "apellidos": "Test",
           "username": "test",
           "email": "test@test.com",
           "password": "12345",
           "passwordComprobacion": "12345"
           }
          """;

    // Mock del servicio
    when(authenticationService.signUp(any(UserSignUpRequest.class)))
        .thenThrow(new AuthExistingUsernameOrEmail("El usuario con username XXX o email XXX ya existe"));

    // Llamada al endpoint
    var result = mockMvcTester.post()
        .uri(ENDPOINT + "/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasFailed().failure()
        .isInstanceOf(AuthExistingUsernameOrEmail.class)
        .hasMessageContaining("ya existe");

    // Verify
    verify(authenticationService, times(1)).signUp(any(UserSignUpRequest.class));

  }

  // Comprobar todas las validaciones
  @Test
  void signUp_BadRequest_When_Nombre_Apellidos_Email_Username_Empty_ShouldThrowException() {
    String requestBody = """
          {
           "nombre": "",
           "apellidos": "",
           "username": "",
           "email": "",
           "password": "12345",
           "passwordComprobacion": "12345"
           }
          """;

    // Llamada al endpoint
    var result = mockMvcTester.post()
        .uri(ENDPOINT + "/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPathSatisfying("$.errores", path -> {
          assertThat(path).hasFieldOrProperty("nombre");
          assertThat(path).hasFieldOrProperty("apellidos");
          assertThat(path).hasFieldOrProperty("username");
          assertThat(path).hasFieldOrProperty("email");
        });

    // Verify
    verify(authenticationService, never()).signUp(any(UserSignUpRequest.class));

  }

  @Test
  void signIn() {
    String requestBody = """
          {
           "username": "test2",
           "password": "12345"
           }
          """;

    var jwtAuthResponse = JwtAuthResponse.builder().token("token").build();
    // Arrange
    when(authenticationService.signIn(any(UserSignInRequest.class))).thenReturn(jwtAuthResponse);

    // Llamada al endpoint
    var result = mockMvcTester.post()
        .uri(ENDPOINT + "/signin")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson()
        .convertTo(JwtAuthResponse.class)
        .isEqualTo(jwtAuthResponse);

    // Verify
    verify(authenticationService, times(1)).signIn(any(UserSignInRequest.class));

  }

  @Test
  void signIn_NotValid() {
    String requestBody = """
          {
           "username": "test2",
           "password": "password"
           }
          """;

    // Mock del servicio
    when(authenticationService.signIn(any(UserSignInRequest.class)))
        .thenThrow(new AuthSignInNotValid("Usuario o contraseña incorrectos"));

    // Llamada al endpoint
    var result = mockMvcTester.post()
        .uri(ENDPOINT + "/signin")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatus4xxClientError()
        .hasFailed().failure()
        .isInstanceOf(AuthSignInNotValid.class)
        .hasMessageContaining("incorrectos");

    // Verify
    verify(authenticationService, times(1)).signIn(any(UserSignInRequest.class));

  }

  // Comprobar todas las validaciones
  @Test
  void signIn_BadRequest_When_Username_Password_Empty_ShouldThrowException() {
    String requestBody = """
          {
           "username": "",
           "password": ""
           }
          """;

    // Llamada al endpoint
    var result = mockMvcTester.post()
        .uri(ENDPOINT + "/signin")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPathSatisfying("$.errores", path -> {
          assertThat(path).hasFieldOrProperty("username");
          assertThat(path).hasFieldOrProperty("password");
        });

    // Verify
    verify(authenticationService, never()).signIn(any(UserSignInRequest.class));
  }

}

