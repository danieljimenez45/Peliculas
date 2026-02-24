package es.danieljr.peliculas.rest.users.services;

import es.danieljr.peliculas.rest.entradas.models.Entrada;
import es.danieljr.peliculas.rest.entradas.repositories.EntradasRepository;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import es.danieljr.peliculas.rest.users.dto.UserInfoResponse;
import es.danieljr.peliculas.rest.users.dto.UserRequest;
import es.danieljr.peliculas.rest.users.dto.UserResponse;
import es.danieljr.peliculas.rest.users.exceptions.UserNameOrEmailExists;
import es.danieljr.peliculas.rest.users.exceptions.UserNotFound;
import es.danieljr.peliculas.rest.users.mappers.UsersMapper;
import es.danieljr.peliculas.rest.users.models.User;
import es.danieljr.peliculas.rest.users.repositories.UsersRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests del servicio de Usuarios (lógica de negocio).
 * Se mockean UsersRepository, EntradasRepository y se usa Spy del UsersMapper.
 */
@ExtendWith(MockitoExtension.class)
class UsersServiceImplTest {
  private final UserRequest userRequest = UserRequest.builder()
      .username("test").email("test@test.com").nombre("Test").apellidos("User").password("password123").build();
  private final User user = User.builder()
      .id(99L).username("test").email("test@test.com").nombre("Test").apellidos("User").password("password123").build();

  @Mock
  private UsersRepository usersRepository;
  @Mock
  private EntradasRepository entradasRepository;
  @Spy
  private UsersMapper usersMapper;
  @InjectMocks
  private UsersServiceImpl usersService;

  /** findAll sin filtros: repositorio devuelve página; servicio la mapea a UserResponse. */
  @Test
  public void testFindAll_NoFilters_ReturnsPageOfUsers() {
    // Arrange
    List<User> users = Arrays.asList(new User(), new User());
    Page<User> page = new PageImpl<>(users);
    when(usersRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

    // Act
    Page<UserResponse> result = usersService.findAll(
        Optional.empty(), Optional.empty(), Optional.empty(), Pageable.unpaged());

    // Assert
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(2, result.getTotalElements())
    );

    // Verify
    verify(usersRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
  }

  /** findById: repositorio devuelve usuario; entradasRepository devuelve entradas; se construye UserInfoResponse. */
  @Test
  public void testFindById() {
    // Arrange
    Long userId = 1L;
    Pelicula pelicula = Pelicula.builder().idPelicula(1L).titulo("Test Movie").build();
    Entrada entrada = Entrada.builder().idEntrada(1L).pelicula(pelicula).build();
    when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
    when(entradasRepository.findByUsuarioId(userId)).thenReturn(List.of(entrada));

    // Act
    UserInfoResponse result = usersService.findById(userId);

    //Assert
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(userRequest.getUsername(), result.getUsername()),
        () -> assertEquals(userRequest.getEmail(), result.getEmail())
    );

    // Verify
    verify(usersRepository, times(1)).findById(userId);
    verify(entradasRepository, times(1)).findByUsuarioId(userId);

  }

  @Test
  public void testFindById_UserNotFound_ThrowsUserNotFound() {
    // Arrange
    Long userId = 1L;
    when(usersRepository.findById(userId)).thenReturn(Optional.empty());

    // Act and Assert
    assertThrows(UserNotFound.class, () -> usersService.findById(userId));

    // Verify
    verify(usersRepository, times(1)).findById(userId);
  }

  @Test
  public void testSave_ValidUserRequest_ReturnsUserResponse() {
    // Arrange
    when(usersRepository.findByUsernameEqualsIgnoreCaseOrEmailEqualsIgnoreCase(
        anyString(), anyString())).thenReturn(Optional.empty());
    when(usersRepository.save(any(User.class))).thenReturn(user);

    // Act
    UserResponse result = usersService.save(userRequest);

    // Assert
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(userRequest.getUsername(), result.getUsername()),
        () -> assertEquals(userRequest.getEmail(), result.getEmail())
    );

    // Verify
    verify(usersRepository, times(1))
        .findByUsernameEqualsIgnoreCaseOrEmailEqualsIgnoreCase(anyString(), anyString());
    verify(usersRepository, times(1)).save(any(User.class));

  }

  @Test
  public void testSave_DuplicateUsernameOrEmail_ThrowsUserNameOrEmailExists() {
    // Arrange
    when(usersRepository.findByUsernameEqualsIgnoreCaseOrEmailEqualsIgnoreCase(
        anyString(), anyString())).thenReturn(Optional.of(user));

    // Act and Assert
    assertThrows(UserNameOrEmailExists.class, () -> usersService.save(userRequest));
  }

  @Test
  public void testUpdate_ValidUserRequest_ReturnsUserResponse() {
    // Arrange
    Long userId = 1L;
    when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
    when(usersRepository.findByUsernameEqualsIgnoreCaseOrEmailEqualsIgnoreCase(
        anyString(), anyString())).thenReturn(Optional.empty());
    when(usersRepository.save(any(User.class))).thenReturn(user);

    // Act
    UserResponse result = usersService.update(userId, userRequest);

    // Assert
    assertAll(
        () -> assertNotNull(result),
        () -> assertEquals(userRequest.getUsername(), result.getUsername()),
        () -> assertEquals(userRequest.getEmail(), result.getEmail())
    );

    // Verify
    verify(usersRepository, times(1)).findById(userId);
    verify(usersRepository, times(1))
        .findByUsernameEqualsIgnoreCaseOrEmailEqualsIgnoreCase(anyString(), anyString());
    verify(usersRepository, times(1)).save(any(User.class));
  }

  @Test
  public void testUpdate_DuplicateUsernameOrEmail_ThrowsUserNameOrEmailExists() {
    // Arrange
    Long userId = 1L;
    User otroUsuario = User.builder().id(2L).build();
    when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
    when(usersRepository.findByUsernameEqualsIgnoreCaseOrEmailEqualsIgnoreCase(
        anyString(), anyString())).thenReturn(Optional.of(otroUsuario));

    // Act and Assert
    assertThrows(UserNameOrEmailExists.class, () -> usersService.update(userId, userRequest));
  }

  @Test
  public void testUpdate_UserNotFound_ThrowsUserNotFound() {
    // Arrange
    Long userId = 1L;
    when(usersRepository.findById(userId)).thenReturn(Optional.empty());

    // Act and Assert
    assertThrows(UserNotFound.class, () -> usersService.update(userId, userRequest));
  }

  @Test
  public void testDeleteById_PhisicalDelete() {
    // Arrange
    Long userId = 1L;
    when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
    when(entradasRepository.existsByUsuarioId(userId)).thenReturn(false);

    // Act
    usersService.deleteById(userId);

    // Verify
    verify(usersRepository, times(1)).delete(user);
    verify(entradasRepository, times(1)).existsByUsuarioId(userId);
  }

  @Test
  public void testDeleteById_LogicalDelete() {
    // Arrange
    Long userId = 1L;
    when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
    when(entradasRepository.existsByUsuarioId(userId)).thenReturn(true);
    doNothing().when(usersRepository).updateIsDeletedToTrueById(userId);

    // Act
    usersService.deleteById(userId);

    // Assert

    // Verify
    verify(usersRepository, times(1)).updateIsDeletedToTrueById(userId);
    verify(entradasRepository, times(1)).existsByUsuarioId(userId);
  }

  @Test
  public void testDeleteByIdNotExists() {
    // Arrange
    Long userId = 1L;
    User user = new User();
    when(usersRepository.findById(userId)).thenReturn(Optional.of(user));
    when(entradasRepository.existsByUsuarioId(userId)).thenReturn(true);

    // Act
    usersService.deleteById(userId);

    // Verify
    verify(usersRepository, times(1)).updateIsDeletedToTrueById(userId);
    verify(entradasRepository, times(1)).existsByUsuarioId(userId);
  }

}

