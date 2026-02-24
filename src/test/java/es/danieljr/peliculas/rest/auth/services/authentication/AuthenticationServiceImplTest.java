package es.danieljr.peliculas.rest.auth.services.authentication;

import es.danieljr.peliculas.rest.auth.dto.JwtAuthResponse;
import es.danieljr.peliculas.rest.auth.dto.UserSignInRequest;
import es.danieljr.peliculas.rest.auth.dto.UserSignUpRequest;
import es.danieljr.peliculas.rest.auth.exceptions.AuthDifferentPasswords;
import es.danieljr.peliculas.rest.auth.exceptions.AuthExistingUsernameOrEmail;
import es.danieljr.peliculas.rest.auth.exceptions.AuthSignInNotValid;
import es.danieljr.peliculas.rest.auth.repositories.AuthUsersRepository;
import es.danieljr.peliculas.rest.auth.services.jwt.JwtService;
import es.danieljr.peliculas.rest.users.models.Role;
import es.danieljr.peliculas.rest.users.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests del servicio de autenticación (signUp, signIn).
 * Se mockean AuthUsersRepository, PasswordEncoder, JwtService y AuthenticationManager.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

  @Mock
  private AuthUsersRepository authUsersRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private JwtService jwtService;
  @Mock
  private AuthenticationManager authenticationManager;

  @InjectMocks
  private AuthenticationServiceImpl authenticationService;

  /** signUp con contraseñas coincidentes: se guarda usuario, se genera JWT y se devuelve en JwtAuthResponse. */
  @Test
  public void testSignUp_WhenPasswordsMatch_ShouldReturnToken() {
    // Datos de prueba
    UserSignUpRequest request = UserSignUpRequest.builder()
        .nombre("Test")
        .apellidos("User")
        .username("testuser")
        .email("test@example.com")
        .password("password")
        .passwordComprobacion("password")
        .build();

    // Mock del repositorio de usuarios
    User userStored = new User();
    when(authUsersRepository.save(any(User.class))).thenReturn(userStored);

    // Mock del servicio JWT
    String token = "test_token";
    when(jwtService.generateToken(userStored)).thenReturn(token);

    // Llamada al método a probar
    JwtAuthResponse response = authenticationService.signUp(request);

    // Verificaciones
    assertAll("Sign Up",
        () -> assertNotNull(response),
        () -> assertEquals(token, response.getToken()),
        () -> verify(authUsersRepository, times(1)).save(any(User.class)),
        () -> verify(jwtService, times(1)).generateToken(userStored)
    );
  }

  @Test
  public void testSignUp_WhenPasswordsDoNotMatch_ShouldThrowException() {
    // Datos de prueba
    UserSignUpRequest request = UserSignUpRequest.builder()
        .nombre("Test")
        .apellidos("User")
        .username("testuser")
        .email("test@example.com")
        .password("password1")
        .passwordComprobacion("password2")
        .build();

    // Llamada al método a probar y verificación de excepción
    assertThrows(AuthDifferentPasswords.class, () -> authenticationService.signUp(request));
  }

  /** signUp con username o email ya existente: repositorio lanza DataIntegrityViolation → AuthExistingUsernameOrEmail. */
  @Test
  public void testSignUp_WhenUsernameOrEmailAlreadyExist_ShouldThrowException() {
    // Datos de prueba
    UserSignUpRequest request = UserSignUpRequest.builder()
        .nombre("Test")
        .apellidos("User")
        .username("testuser")
        .email("test@example.com")
        .password("password")
        .passwordComprobacion("password")
        .build();

    // Mock del repositorio de usuarios
    when(authUsersRepository.save(any(User.class))).thenThrow(DataIntegrityViolationException.class);

    // Llamada al método a probar y verificación de excepción
    assertThrows(AuthExistingUsernameOrEmail.class, () -> authenticationService.signUp(request));
  }

  @Test
  public void testSignIn_WhenValidCredentials_ShouldReturnToken() {
    // Datos de prueba
    UserSignInRequest request = UserSignInRequest.builder()
        .username("testuser")
        .password("password")
        .build();

    // Mock del repositorio de usuarios
    User user = new User();
    when(authUsersRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(user));

    // Mock del servicio JWT
    String token = "test_token";
    when(jwtService.generateToken(user)).thenReturn(token);

    // Llamada al método a probar
    JwtAuthResponse response = authenticationService.signIn(request);

    // Verificaciones
    assertAll("Sign In",
        () -> assertNotNull(response),
        () -> assertEquals(token, response.getToken()),
        () -> verify(authenticationManager, times(1))
            .authenticate(any(UsernamePasswordAuthenticationToken.class)),
        () -> verify(authUsersRepository, times(1)).findByUsername(request.getUsername()),
        () -> verify(jwtService, times(1)).generateToken(user)
    );
  }

  @Test
  public void testSignIn_WhenInvalidCredentials_ShouldThrowException() {
    // Datos de prueba
    UserSignInRequest request = UserSignInRequest.builder()
        .username("testuser")
        .password("password")
        .build();

    // Mock del repositorio de usuarios
    when(authUsersRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());

    // Llamada al método a probar y verificación de excepción
    assertThrows(AuthSignInNotValid.class, () -> authenticationService.signIn(request));
  }

}

