package es.danieljr.peliculas.web.controllers;

import es.danieljr.peliculas.rest.users.models.User;
import es.danieljr.peliculas.rest.users.services.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
public class LoginController {
    private final UsersService usuarioServicio;

    @GetMapping("/")
    public String welcome(){
        return "redirect:/public/";
    }

    @GetMapping("/auth/login")
    public String Login(Model model, @RequestParam(required = false) String error){
        //CSFR token is handled by GlobalControllerAdvice
        //Para el formulario de registro
        model.addAttribute("usuario", new User());
        if (error != null) {
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Usuario o contraseña incorrectos. Por favor, inténtalo de nuevo.");
        }
        return "login";
    }
}
