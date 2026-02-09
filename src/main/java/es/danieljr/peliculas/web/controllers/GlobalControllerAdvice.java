package es.danieljr.peliculas.web.controllers;

import es.danieljr.peliculas.rest.users.models.Role;
import es.danieljr.peliculas.rest.users.models.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.util.stream.Collectors;

// Aqui exponemos atributos globales para las vistas
@ControllerAdvice
public class GlobalControllerAdvice {

  @Value("${spring.application.name}")
  private String appName;

  @ModelAttribute("appName")
  public String getAppName() {
    return appName;
  }

  @Value("${application.title}")
  private String appDescription;

  @ModelAttribute("appDescription")
  public String getAppDescription() {
    return appDescription;
  }

  @ModelAttribute("currentUser")
  public User getCurrentUser(Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof User) {
      return (User) authentication.getPrincipal();
    }
    return null;
  }

  @ModelAttribute("isAuthenticated")
  public boolean isAuthenticated(Authentication authentication) {
    return authentication != null && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof UserDetails;
  }

  @ModelAttribute("isAdmin")
  public boolean isAdmin(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof UserDetails)) {
      return false;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof User) {
      User user = (User) principal;
      return user.getRoles() != null && user.getRoles().stream()
          .anyMatch(role -> role == Role.ADMIN);
    }
    // Spring Security UserDetails (e.g. @WithMockUser)
    UserDetails userDetails = (UserDetails) principal;
    return userDetails.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_ADMIN"::equals);
  }

  @ModelAttribute("username")
  public String getUsername(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof UserDetails)) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof User) {
      User user = (User) principal;
      return user.getNombre() + " " + user.getApellidos();
    }
    return ((UserDetails) principal).getUsername();
  }

  @ModelAttribute("userRoles")
  public String getUserRoles(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof UserDetails)) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof User) {
      User user = (User) principal;
      if (user.getRoles() != null) {
        return user.getRoles().stream().map(Object::toString)
            .collect(Collectors.joining(","));
      }
    } else {
      return ((UserDetails) principal).getAuthorities().stream()
          .map(GrantedAuthority::getAuthority)
          .collect(Collectors.joining(","));
    }
    return null;
  }

  @ModelAttribute("csrfToken")
  public String getCsrfToken(HttpServletRequest request) {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    return csrfToken != null ? csrfToken.getToken() : "";
  }

  @ModelAttribute("csrfParamName")
  public String getCsrfParamName(HttpServletRequest request) {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    return csrfToken != null ? csrfToken.getParameterName() : "_csrf";
  }

  @ModelAttribute("csrfHeaderName")
  public String getCsrfHeaderName(HttpServletRequest request) {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    return csrfToken != null ? csrfToken.getHeaderName() : "X-CSRF-TOKEN";
  }

  @ModelAttribute("currentYear")
  public int getCurrentYear() {
    return LocalDate.now().getYear();
  }
}
