package es.danieljr.peliculas.rest.auth.services.users;

import es.danieljr.peliculas.rest.auth.repositories.AuthUsersRepository;
import es.danieljr.peliculas.rest.users.exceptions.UserNotFound;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

/**
 * Forma actual: implementa AuthUsersService (que extiende UserDetailsService).
 * Spring usa este bean como "userDetailsService" por el nombre del @Service.
 *
 * Forma alternativa: 1) En AuthUsersService.java comenta la interfaz que extiende UserDetailsService
 * y descomenta la que tiene userDetailsService(). 2) Aquí descomenta ", UserDetailsService" y el método
 * userDetailsService() de abajo. 3) En JwtAuthenticationFilter usa:
 * authUsersService.userDetailsService().loadUserByUsername(userName) en lugar de loadUserByUsername(userName).
 */
@RequiredArgsConstructor
@Service("userDetailsService")
public class AuthUsersServiceImpl implements AuthUsersService /* , UserDetailsService */ {

    private final AuthUsersRepository authUsersRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UserNotFound {
        return authUsersRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFound("Usuario con username " + username + " no encontrado"));
    }

    // Forma alternativa: descomenta este método cuando uses la interfaz con userDetailsService()
    // @Override
    // public UserDetailsService userDetailsService() {
    //     return this;
    // }
}