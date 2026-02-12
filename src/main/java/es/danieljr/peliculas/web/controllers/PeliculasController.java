package es.danieljr.peliculas.web.controllers;

import es.danieljr.peliculas.rest.peliculas.dto.PeliculaCreateDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaUpdateDto;
import es.danieljr.peliculas.rest.peliculas.services.PeliculasService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("peliculas")
public class PeliculasController {
    private final PeliculasService peliculasService;

    @GetMapping("/{id}")
    public String getById(@PathVariable Long id, Model model) {
        PeliculaResponseDto pelicula = peliculasService.findById(id);
        model.addAttribute("pelicula", pelicula);
        return "peliculas/detalle";
    }

    @GetMapping({"", "/", "/lista"})
    public String lista(Model model,
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "4") int size){
        Pageable pageable = PageRequest.of(page, size, Sort.by("idPelicula").ascending());
        Page<PeliculaResponseDto> peliculasPage = peliculasService.findAll(
            Optional.empty(), Optional.empty(), pageable);

        model.addAttribute("page", peliculasPage);
        return "peliculas/lista";
    }

    // Creamos una nueva película
    @GetMapping("/new")
    public String nuevaPeliculaForm(Model model, HttpSession session) {
        // Intentar recuperar datos de sesión si hay errores previos
        PeliculaCreateDto pelicula = (PeliculaCreateDto) session.getAttribute("formData_pelicula_new");
        if (pelicula == null) {
            pelicula = PeliculaCreateDto.builder().build();
        } else {
            // Limpiar datos de sesión después de recuperarlos
            session.removeAttribute("formData_pelicula_new");
        }
        model.addAttribute("pelicula", pelicula);
        model.addAttribute("modoEditar", false);
        return "/peliculas/form";
    }

    @PostMapping("/new")
    public String nuevaPeliculaSubmit(@Valid @ModelAttribute("pelicula") PeliculaCreateDto pelicula,
                                      BindingResult bindingResult,
                                      HttpSession session) {
        // Si no tiene errores...
        if (bindingResult.hasErrors()) {
            log.info("hay errores en la validación");
            // Guardar datos del formulario en sesión para recuperarlos después
            session.setAttribute("formData_pelicula_new", pelicula);
            return "/peliculas/form";
        } else {
            // Limpiar datos de sesión si existen
            session.removeAttribute("formData_pelicula_new");
            //insertamos
            peliculasService.save(pelicula);
            return "redirect:/peliculas/lista";
        }
    }

    @GetMapping("/{id}/edit")
    public String editarPeliculaForm(@PathVariable Long id, Model model, HttpSession session) {
        PeliculaResponseDto peliculaEncontrada = peliculasService.findById(id);
        if (peliculaEncontrada == null) {
            return "redirect:/peliculas/new";
        } else {
            // Intentar recuperar datos de sesión si hay errores previos
            PeliculaUpdateDto pelicula = (PeliculaUpdateDto) session.getAttribute("formData_pelicula_edit_" + id);
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
                session.removeAttribute("formData_pelicula_edit_" + id);
            }
            model.addAttribute("pelicula", pelicula);
            model.addAttribute("peliculaId", id);
            model.addAttribute("modoEditar", true);
            return "peliculas/form";
        }
    }

    @PostMapping("/{id}/edit")
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
            session.setAttribute("formData_pelicula_edit_" + id, pelicula);
            model.addAttribute("peliculaId", id);
            model.addAttribute("modoEditar", true);
            return "/peliculas/form";
        }

        // Limpiar datos de sesión si existen
        session.removeAttribute("formData_pelicula_edit_" + id);
        peliculasService.update(id, pelicula);
        redirectAttributes.addFlashAttribute("message",
                "Pelicula actualizada correctamente.");
        return "redirect:/peliculas/{id}";
    }

    @GetMapping("/{id}/delete")
    public String borrarPelicula(@PathVariable Long id) {

        // TO DO Borrar con confirmación mediante ventana modal

        peliculasService.deleteById(id);
        return "redirect:/peliculas/lista";
    }

}
