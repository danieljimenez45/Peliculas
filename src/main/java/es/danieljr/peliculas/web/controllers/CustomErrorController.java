package es.danieljr.peliculas.web.controllers;

import es.danieljr.peliculas.web.services.I18nService;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
public class CustomErrorController implements ErrorController {

  private final I18nService i18nService;

  @RequestMapping("/error")
  public String handleError(HttpServletRequest request, Model model) {
    Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

    String errorTitle = "Error";
    String errorMessage = i18nService.getMessage("error.general");
    String errorCode = "500";
    int statusCode = 500;

    if (status != null) {
      statusCode = Integer.parseInt(status.toString());
      errorCode = String.valueOf(statusCode);

      if (statusCode == HttpStatus.NOT_FOUND.value()) {
        errorTitle = i18nService.getMessage("error.404");
        errorMessage = "La página que buscas no existe o ha sido movida.";
      } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
        errorTitle = i18nService.getMessage("error.403");
        errorMessage = "No tienes permisos para acceder a esta página.";
      } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
        errorTitle = i18nService.getMessage("error.500");
        errorMessage = "Algo salió mal en el servidor. Estamos trabajando para solucionarlo.";
      }
    }

    model.addAttribute("status", statusCode);
    model.addAttribute("errorCode", errorCode);
    model.addAttribute("errorTitle", errorTitle);
    model.addAttribute("errorMessage", errorMessage);

    return "error";
  }
}
