package es.danieljr.peliculas.rest.auth.services.users;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/** Forma actual: esta interfaz extiende UserDetailsService. */
public interface AuthUsersService extends UserDetailsService {
    @Override
    UserDetails loadUserByUsername(String username);
}

/*
 * Forma alternativa (para usar: comenta el bloque anterior y descomenta este).
 * La interfaz no extiende UserDetailsService; expone un método que lo devuelve.
 *
 * public interface AuthUsersService {
 *     UserDetailsService userDetailsService();
 * }
 */