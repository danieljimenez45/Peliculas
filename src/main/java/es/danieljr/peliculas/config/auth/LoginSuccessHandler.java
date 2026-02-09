package es.danieljr.peliculas.config.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
  private static final String COOKIE_NAME = "visitasApp";
  private static final int MAX_AGE = 365 * 24 * 60 * 60; // 1 año

  public LoginSuccessHandler() {
    // valor por defecto si no hay saved-request
    setDefaultTargetUrl("/public");
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
    int val = 0;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie c : cookies) {
        if (COOKIE_NAME.equals(c.getName())) {
          try {
            val = Integer.parseInt(c.getValue());
          } catch (NumberFormatException ignored) {}
        }
      }
    }
    val++;

    Cookie newCookie = new Cookie(COOKIE_NAME, Integer.toString(val));
    newCookie.setPath("/");
    newCookie.setMaxAge(MAX_AGE);
    // Debe ser accesible desde JS para la plantilla que muestra el contador
    newCookie.setHttpOnly(false);
    newCookie.setSecure(request.isSecure());
    response.addCookie(newCookie);

    super.onAuthenticationSuccess(request, response, authentication);
  }
}
