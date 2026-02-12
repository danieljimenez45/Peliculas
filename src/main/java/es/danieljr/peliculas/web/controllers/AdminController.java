package es.danieljr.peliculas.web.controllers;

import es.danieljr.peliculas.rest.peliculas.dto.PeliculaCreateDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaUpdateDto;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import es.danieljr.peliculas.rest.peliculas.services.PeliculasService;
import es.danieljr.peliculas.web.services.I18nService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final PeliculasService peliculasService;
  private final I18nService i18nService;

  @GetMapping("/peliculas")
  public String peliculas(Model model,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "4") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("idPelicula").ascending());
    Page<PeliculaResponseDto> peliculasPage = peliculasService.findAll(
        Optional.empty(), Optional.empty(), pageable);

    model.addAttribute("page", peliculasPage);
    model.addAttribute("useAdminUrls", true);
    return "admin/peliculas/lista";
  }

  @GetMapping("/peliculas/filter")
  public String peliculasFiltrar(Model model,
      @RequestParam(required = false) Optional<String> titulo,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "4") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("idPelicula").ascending());
    Page<PeliculaResponseDto> peliculasPage = peliculasService.findAll(
        titulo, Optional.empty(), pageable);

    model.addAttribute("page", peliculasPage);
    model.addAttribute("useAdminUrls", true);
    return "fragments/listaPeliculas";
  }

  @GetMapping("/peliculas/{id}")
  public String getById(@PathVariable Long id, Model model) {
    Pelicula pelicula = peliculasService.buscarPorId(id).orElse(null);
    model.addAttribute("pelicula", pelicula);
    return "admin/peliculas/detalle";
  }

  // Creamos una nueva película
  @GetMapping("/peliculas/new")
  public String nuevaPeliculaForm(Model model, HttpSession session) {
    // Intentar recuperar datos de sesión si hay errores previos
    PeliculaCreateDto pelicula = (PeliculaCreateDto) session.getAttribute("formData_admin_pelicula_new");
    if (pelicula == null) {
      pelicula = PeliculaCreateDto.builder().build();
    } else {
      // Limpiar datos de sesión después de recuperarlos
      session.removeAttribute("formData_admin_pelicula_new");
    }
    model.addAttribute("pelicula", pelicula);
    model.addAttribute("modoEditar", false);
    return "admin/peliculas/form";
  }

  @PostMapping("/peliculas/new")
  public String nuevaPeliculaSubmit(@Valid @ModelAttribute("pelicula") PeliculaCreateDto pelicula,
      BindingResult bindingResult,
      HttpSession session) {

    log.info("Datos recibidos del formulario: {}", pelicula);
    // Si no tiene errores...
    if (bindingResult.hasErrors()) {
      log.info("hay errores en la validación");
      // Guardar datos del formulario en sesión para recuperarlos después
      session.setAttribute("formData_admin_pelicula_new", pelicula);
      return "admin/peliculas/form";
    } else {
      // Limpiar datos de sesión si existen
      session.removeAttribute("formData_admin_pelicula_new");
      // insertamos
      peliculasService.save(pelicula);
      return "redirect:/admin/peliculas";
    }
  }

  @GetMapping("/peliculas/{id}/edit")
  public String editarPelicualForm(@PathVariable Long id, Model model, HttpSession session) {
    Pelicula peliculaEncontrada = peliculasService.buscarPorId(id).orElse(null);
    if (peliculaEncontrada == null) {
      return "redirect:/admin/peliculas/new";
    } else {
      // Intentar recuperar datos de sesión si hay errores previos
      PeliculaUpdateDto pelicula = (PeliculaUpdateDto) session.getAttribute("formData_admin_pelicula_edit_" + id);
      if (pelicula == null) {
        // Si no hay datos en sesión, usar los datos de la BD
        pelicula = PeliculaUpdateDto.builder()
            .titulo(peliculaEncontrada.getTitulo())
            .genero(peliculaEncontrada.getGenero())
            .duracion(peliculaEncontrada.getDuracion())
            .sinopsis(peliculaEncontrada.getSinopsis())
            .actoresPrincipales(peliculaEncontrada.getActoresPrincipales())
            .actoresSecundarios(peliculaEncontrada.getActoresSecundarios())
            .director(peliculaEncontrada.getDirector())
            .build();
      } else {
        // Limpiar datos de sesión después de recuperarlos
        session.removeAttribute("formData_admin_pelicula_edit_" + id);
      }
      model.addAttribute("pelicula", pelicula);
      model.addAttribute("peliculaId", id);
      model.addAttribute("modoEditar", true);
      return "admin/peliculas/form";
    }
  }

  @PostMapping("/peliculas/{id}/edit")
  public String editarPeliculaSubmit(@PathVariable("id") Long id,
      @Valid @ModelAttribute("pelicula") PeliculaUpdateDto pelicula,
      BindingResult result,
      Model model,
      HttpSession session,
      RedirectAttributes redirectAttributes) {
    if (result.hasErrors()) {
      redirectAttributes.addFlashAttribute("error",
          "Ha ocurrido un error al actualizar la pelicula.");
      // Guardar datos del formulario en sesión para recuperarlos después
      session.setAttribute("formData_admin_pelicula_edit_" + id, pelicula);
      model.addAttribute("peliculaId", id);
      model.addAttribute("modoEditar", true);
      return "admin/peliculas/form";
    }

    // Limpiar datos de sesión si existen
    session.removeAttribute("formData_admin_pelicula_edit_" + id);
    peliculasService.update(id, pelicula);
    redirectAttributes.addFlashAttribute("success",
        "Pelicula actualizada correctamente.");
    return "redirect:/admin/peliculas/{id}";
  }

  // ahora requiere POST y un token de confirmación almacenado en sesión
  @PostMapping("/peliculas/{id}/delete")
  public String borrarPelicula(@PathVariable Long id,
      @RequestParam("deleteToken") String deleteToken,
      HttpSession session,
      RedirectAttributes redirectAttributes) {
    String sessionKey = "deleteToken_" + id;
    String tokenInSession = (String) session.getAttribute(sessionKey);

    if (tokenInSession == null || !tokenInSession.equals(deleteToken)) {
      redirectAttributes.addFlashAttribute("error", "Confirmación inválida o caducada.");
      return "redirect:/admin/peliculas";
    }

    // invalidar token y proceder al borrado
    session.removeAttribute(sessionKey);
    peliculasService.deleteById(id);
    redirectAttributes.addFlashAttribute("success", "Pelicula borrada correctamente.");
    return "redirect:/admin/peliculas";
  }

  @GetMapping("/peliculas/{id}/delete/confirm")
  public String showModalBorrar(@PathVariable("id") Long id, Model model, HttpSession session) {
    Optional<Pelicula> pelicula = peliculasService.buscarPorId(id);
    String deleteMessage;
    if (pelicula.isPresent()) {
      deleteMessage = i18nService.getMessage("peliculas.borrar.mensaje",
          new Object[] { pelicula.get().getTitulo() });
    } else {
      return "redirect:/peliculas/?error=true";
    }

    // generar token de un solo uso y guardarlo en sesión
    String token = UUID.randomUUID().toString();
    String sessionKey = "deleteToken_" + id;
    session.setAttribute(sessionKey, token);

    model.addAttribute("deleteUrl", "/admin/peliculas/" + id + "/delete");
    model.addAttribute("deleteToken", token);
    model.addAttribute("deleteTitle",
        i18nService.getMessage("peliculas.borrar.titulo"));
    model.addAttribute("deleteMessage", deleteMessage);
    return "fragments/deleteModal";
  }

}
