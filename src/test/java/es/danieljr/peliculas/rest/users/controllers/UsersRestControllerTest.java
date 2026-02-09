package es.danieljr.peliculas.rest.users.controllers;

import es.danieljr.peliculas.rest.entradas.dto.EntradaResponseDto;
import es.danieljr.peliculas.rest.entradas.services.EntradasService;
import es.danieljr.peliculas.rest.users.dto.UserInfoResponse;
import es.danieljr.peliculas.rest.users.dto.UserRequest;
import es.danieljr.peliculas.rest.users.dto.UserResponse;
import es.danieljr.peliculas.rest.users.exceptions.UserNotFound;
import es.danieljr.peliculas.rest.users.services.UsersService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


// Podemos usar el contexto de Spring para autenticar o usar un usuario mockeado:
// @WithMockUser(username = "pepe", roles = {"USER"})
// @WithUserDetails(value = "admin", userDetailsServiceBeanName = "userDetailsService")
// Porque está dado de alta en la base de datos data.sql
// @WithUserDetails(value = "admin")
// En el ejemplo siguiente, se va a ejecutar usando el usuario admin con roles de usuario y admin
@WithMockUser(username = "admin", password = "admin", roles = {"ADMIN", "USER"})
@SpringBootTest
@AutoConfigureMockMvc
class UsersRestControllerTest {

  private final String ENDPOINT = "/api/v1/users";

  private final UserResponse userResponse = UserResponse.builder()
      .id(99L)
      .nombre("test")
      .apellidos("test")
      .username("test")
      .email("test@test.com")
      .build();
  private final UserInfoResponse userInfoResponse = UserInfoResponse.builder()
      .id(99L)
      .nombre("test")
      .apellidos("test")
      .username("test")
      .email("test@test.com")
      .build();

  @Autowired
  MockMvcTester mockMvcTester;

  @MockitoBean
  private UsersService usersService;

  @MockitoBean
  private EntradasService entradasService;

  @Test
  @WithAnonymousUser
  void NotAuthenticated() {
    // Act. Consultar el endpoint
    var result = mockMvcTester.get()
        .uri(ENDPOINT)
        .exchange();

    assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
  }

  @Test
  void findAll() {
    var userResponses = List.of(userResponse);
    Page<UserResponse> page = new PageImpl<>(userResponses);
    Pageable pageable = PageRequest.of(0, 10, Sort.by("id").ascending());

    // Arrange
    when(usersService.findAll(Optional.empty(), Optional.empty(), Optional.empty(), pageable))
        .thenReturn(page);

    // Consulto el endpoint
    var result = mockMvcTester.get()
        .uri(ENDPOINT)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson().satisfies(json -> {
          assertThat(json).extractingPath("$.content.length()").isEqualTo(userResponses.size());
          assertThat(json).extractingPath("$.content[0]")
              .convertTo(UserResponse.class).isEqualTo(userResponse);
        });

    // Verify
    verify(usersService, times(1))
        .findAll(Optional.empty(), Optional.empty(), Optional.empty(), pageable);
  }

  @Test
  void findById() {
    // Arrange
    Long id = userResponse.getId();
    when(usersService.findById(anyLong())).thenReturn(userInfoResponse);

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT + "/" + id.toString())
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson()
        .convertTo(UserInfoResponse.class)
        .isEqualTo(userInfoResponse);

    // Verify
    verify(usersService, only()).findById(anyLong());

  }

  @Test
  void findById_NotFound() {
    // Arrange
    Long id = userResponse.getId();
    when(usersService.findById(anyLong())).thenThrow(new UserNotFound("No existe el usuario"));

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT + "/" + id.toString())
        .exchange();

    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasFailed().failure()
        .isInstanceOf(UserNotFound.class)
        .hasMessageContaining("No existe el usuario");

    // Verify
    verify(usersService, only()).findById(anyLong());
  }

  @Test
  void createUser() {
    // Arrange
    String requestBody = """
          {
           "nombre": "test",
           "apellidos": "test",
           "username": "test",
           "email": "test@test.com",
           "password": "test1234"
           }
          """;

    when(usersService.save(any(UserRequest.class))).thenReturn(userResponse);

    // Act
    var result = mockMvcTester.post()
        .uri(ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .convertTo(UserResponse.class)
        .isEqualTo(userResponse);

    // Verify
    verify(usersService, only()).save(any(UserRequest.class));
  }

  @Test
  void createUserBadRequestPasswordMenosDe5Caracteres() {
    // Arrange
    String requestBody = """
          {
           "nombre": "test",
           "apellidos": "test",
           "username": "test",
           "email": "test@test.com",
           "password": "1234"
           }
          """;
    when(usersService.save(any(UserRequest.class))).thenReturn(userResponse);

    // Act
    var result = mockMvcTester.post()
        .uri(ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .hasPathSatisfying("$.errores", path -> {
          assertThat(path).hasFieldOrProperty("password");
        });

    // Verify
    verify(usersService, never()).save(any(UserRequest.class));
  }

  // Lo normal es hacer uno para cada validación
  @Test
  void createUser_BadRequestNombreApellidosEmailTodoEnBlanco() {
    // Arrange
    String requestBody = """
          {
           "nombre": "",
           "apellidos": "",
           "username": "test",
           "email": "",
           "password": "test1234"
           }
          """;
    when(usersService.save(any(UserRequest.class))).thenReturn(userResponse);

    // Act
    var result = mockMvcTester.post()
        .uri(ENDPOINT)
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
          assertThat(path).hasFieldOrProperty("email");
        });

    // Verify
    verify(usersService, never()).save(any(UserRequest.class));
  }

  @Test
  void updateUser() {
    // Arrange
    Long id = userResponse.getId();
    String requestBody = """
          {
           "nombre": "test",
           "apellidos": "test",
           "username": "test",
           "email": "test@test.com",
           "password": "test1234"
           }
          """;

    // Arrange
    when(usersService.update(anyLong(), any(UserRequest.class))).thenReturn(userResponse);

    // Act
    var result = mockMvcTester.put()
        .uri(ENDPOINT+ "/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson()
        .convertTo(UserResponse.class)
        .isEqualTo(userResponse);

    // Verify
    verify(usersService, only()).update(anyLong(), any(UserRequest.class));
  }

  @Test
  void updateUser_NotFound() {
    // Arrange
    Long id = userResponse.getId();
    String requestBody = """
          {
           "nombre": "test",
           "apellidos": "test",
           "username": "test",
           "email": "test@test.com",
           "password": "test1234"
           }
          """;

    when(usersService.update(anyLong(), any(UserRequest.class)))
        .thenThrow(new UserNotFound("No existe el usuario"));

    // Act
    var result = mockMvcTester.put()
        .uri(ENDPOINT+ "/" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody)
        .exchange();

    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasFailed().failure()
        .isInstanceOf(UserNotFound.class)
        .hasMessageContaining("No existe el usuario");

    // Verify
    verify(usersService, only()).update(anyLong(), any(UserRequest.class));
  }

  @Test
  void deleteUser() {
    // Arrange
    Long id = userResponse.getId();
    doNothing().when(usersService).deleteById(anyLong());

    // Act
    var result = mockMvcTester.delete()
        .uri(ENDPOINT+ "/" + id)
        .exchange();
    // Assert
    assertThat(result)
        .hasStatus(HttpStatus.NO_CONTENT);

    // Verify
    verify(usersService, times(1)).deleteById(anyLong());
  }

  @Test
  void deleteUser_NotFound() {
    // Arrange
    Long id = userResponse.getId();
    doThrow(new UserNotFound("No existe el usuario")).when(usersService).deleteById(anyLong());
    // Act
    var result = mockMvcTester.delete()
        .uri(ENDPOINT+ "/" + id)
        .exchange();

    // Assert
    assertThat(result)
    .hasStatus(HttpStatus.NOT_FOUND)
        .hasFailed().failure()
        .isInstanceOf(UserNotFound.class)
        .hasMessageContaining("No existe el usuario");

    // Verify
    verify(usersService, only()).deleteById(anyLong());
  }

  @Test
  // Este endpoint se puede hacer con cualquier usuario autenticado,
  // pero AuthenticationPrincipal necesita un usuario de verdad, por eso usamos admin o user
  // que están en la base de datos data.sql y que lo buscará a través del userDetailsService
  @WithUserDetails("admin")
  void me() {
    // Arrange
    when(usersService.findById(anyLong())).thenReturn(userInfoResponse);

    // Act
    var result = mockMvcTester.get()
        .uri(ENDPOINT+ "/me/profile")
        .exchange();

    // Assert
    assertThat(result)
        .hasStatusOk()
        .bodyJson()
        .convertTo(UserInfoResponse.class)
        .isEqualTo(userInfoResponse);

    // Verify
    verify(usersService, only()).findById(anyLong());
  }

  @Test
  @WithAnonymousUser
  void me_AnonymousUser() {
    // Act. Consultar el endpoint
    var result = mockMvcTester.get()
        .uri(ENDPOINT + "/me/profile")
        .exchange();

    assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
  }

}

