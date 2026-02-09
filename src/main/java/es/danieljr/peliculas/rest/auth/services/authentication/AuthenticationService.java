package es.danieljr.peliculas.rest.auth.services.authentication;


import es.danieljr.peliculas.rest.auth.dto.JwtAuthResponse;
import es.danieljr.peliculas.rest.auth.dto.UserSignInRequest;
import es.danieljr.peliculas.rest.auth.dto.UserSignUpRequest;

public interface AuthenticationService {
    JwtAuthResponse signUp(UserSignUpRequest request);

    JwtAuthResponse signIn(UserSignInRequest request);
}