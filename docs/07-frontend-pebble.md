# 7. Frontend con Pebble

El proyecto **PELICULAS REPO** usa el motor de plantillas **Pebble** para generar HTML en el servidor (MVC clásico). Los controladores en el paquete **web** devuelven nombres de vistas y un **Model** con datos; Pebble resuelve las plantillas y las fragmenta con **layouts** y **macros**.

---

## 7.1 Dependencia y configuración

```xml
<dependency>
    <groupId>io.pebbletemplates</groupId>
    <artifactId>pebble-legacy-spring-boot-starter</artifactId>
    <version>4.1.0</version>
</dependency>
```

En **application.properties**:

```properties
pebble.suffix=.peb.html
pebble.cache=false
pebble.charset=UTF-8
```

- Las plantillas tienen extensión **.peb.html** y se buscan en `src/main/resources/templates/`.
- **pebble.cache=false** en desarrollo para ver cambios sin reiniciar.

---

## 7.2 Estructura de plantillas (PELICULAS REPO)

```
templates/
├── fragments/
│   ├── layout.peb.html      # Layout base (HTML, head, navbar, footer, bloques)
│   ├── head.peb.html       # Meta, CSS, título
│   ├── navbar.peb.html      # Barra de navegación
│   ├── footer.peb.html      # Pie
│   ├── messages.peb.html    # Mensajes flash / errores
│   ├── inputField.peb.html  # Macro para un campo de formulario
│   ├── pager.peb.html       # Macro de paginación
│   ├── listaPeliculas.peb.html
│   └── deleteModal.peb.html
├── index.peb.html
├── login.peb.html
├── error.peb.html
├── peliculas/
│   ├── lista.peb.html
│   ├── detalle.peb.html
│   └── form.peb.html
├── admin/
│   └── peliculas/
│       ├── lista.peb.html
│       ├── form.peb.html
│       └── detalle.peb.html
└── app/
    └── perfil.peb.html
```

---

## 7.3 Layout y herencia

El **layout** define la estructura común y **bloques** que las páginas rellenan.

**fragments/layout.peb.html** (idea según el proyecto):

```html
<!doctype html>
<html>
{% include "fragments/head" %}

<body class="d-flex flex-column min-vh-100">
    {% include "fragments/navbar" %}
    <div class="container mb-5">
        {% block body %}{% endblock %}
    </div>
    {% include "fragments/footer" %}

    <script src="{{ href('/webjars/bootstrap/dist/js/bootstrap.bundle.min.js') }}"></script>
    <script src="{{ href('/js/app.js') }}"></script>
    {% block scripts %}{% endblock %}
</body>
</html>
```

Una página que use el layout:

```html
{% extends "fragments/layout" %}
{% block body %}
<div class="container">
    <h2>{{ message('nav.lista.peliculas') }}</h2>
    {% include "fragments/listaPeliculas" %}
    {{ pager("peliculas", page, _context) }}
</div>
{% endblock %}
```

- **extends**: hereda de otro template.
- **block body**: rellena el bloque `body` del layout.
- **include**: inserta otro template (fragmentos).
- **message('clave')**: función de i18n (en el proyecto se expone vía variable o extensión).

---

## 7.4 Fragmentos (include)

Se usan para no repetir navbar, footer, mensajes y bloques de lista/modal.

Ejemplo de uso en **peliculas/lista.peb.html**:

```html
{% extends "fragments/layout" %}
{% import "fragments/pager" %}

{% block body %}
<div class="container">
    <h2><i class="bi bi-film"></i> {{ message('nav.lista.peliculas') }}</h2>
    {% include "fragments/messages" %}
    {% include "fragments/listaPeliculas" %}
    {{ pager("peliculas", page, _context) }}
</div>
{% endblock %}
```

- **import "fragments/pager"**: importa macros del template pager (p. ej. la macro `pager`).
- **include "fragments/listaPeliculas"**: incluye el fragmento que pinta la tabla/cards de películas (con variable `page` o lista que venga del controller).

---

## 7.5 Macros (pager, inputField)

**Macro**: función reutilizable que genera HTML. Se define en un template y se usa en otros.

**fragments/pager.peb.html** (resumen):

```html
{% macro pager(entityKey, page, _context) %}
{% if page.totalPages > 1 %}
<nav class="mt-4">
  <p class="text-muted">
    Mostrando {{ (page.number * page.size) + 1 }} a {{ ... }} de {{ page.totalElements }}
  </p>
  <ul class="pagination">
    {% if page.hasPrevious %}
    <li class="page-item">
      <a class="page-link" href="?page={{ page.number - 1 }}&size={{ page.size }}">Anterior</a>
    </li>
    {% endif %}
    ...
  </ul>
</nav>
{% endif %}
{% endmacro %}
```

Uso: `{{ pager("peliculas", page, _context) }}` (tras importar el template).

**inputField** (idea): macro que recibe nombre del bean, etiqueta, nombre del campo, tipo, valor y contexto, y genera un `<input>` (o `<textarea>`) con posible mensaje de error de validación (BindingResult).

---

## 7.6 Formularios y CSRF

En Películas los formularios van por POST y llevan el token CSRF para la cadena de seguridad que usa formulario de login:

```html
<form method="post" action="/peliculas/new">
    <input name="{{ csrfParamName }}" type="hidden" value="{{ csrfToken }}">
    ...
</form>
```

**csrfToken**, **csrfParamName** (y **csrfHeaderName**) se exponen desde un **@ControllerAdvice** que lee `CsrfToken` del request y lo añade al modelo. Así todas las vistas tienen esas variables.

---

## 7.7 Conexión con controladores (flujo MVC)

1. El usuario pide una URL (p. ej. `GET /peliculas/lista`).
2. **DispatcherServlet** enruta a un **@Controller** (no RestController) del paquete web.
3. El controller llama al servicio, pone resultados en el **Model** y devuelve el **nombre de la vista** (string).

Ejemplo **PeliculasController** (resumen):

```java
@Controller
@RequestMapping("peliculas")
public class PeliculasController {
    private final PeliculasService peliculasService;

    @GetMapping({"", "/", "/lista"})
    public String lista(Model model,
                        @RequestParam(name = "page", defaultValue = "0") int page,
                        @RequestParam(name = "size", defaultValue = "4") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("idPelicula").ascending());
        Page<PeliculaResponseDto> peliculasPage = peliculasService.findAll(
            Optional.empty(), Optional.empty(), pageable);
        model.addAttribute("page", peliculasPage);
        return "peliculas/lista";   // → templates/peliculas/lista.peb.html
    }

    @GetMapping("/{id}")
    public String getById(@PathVariable Long id, Model model) {
        PeliculaResponseDto pelicula = peliculasService.findById(id);
        model.addAttribute("pelicula", pelicula);
        return "peliculas/detalle";
    }

    @GetMapping("/new")
    public String nuevaPeliculaForm(Model model, HttpSession session) {
        PeliculaCreateDto pelicula = (PeliculaCreateDto) session.getAttribute("formData_pelicula_new");
        if (pelicula == null) pelicula = PeliculaCreateDto.builder().build();
        else session.removeAttribute("formData_pelicula_new");
        model.addAttribute("pelicula", pelicula);
        model.addAttribute("modoEditar", false);
        return "peliculas/form";
    }

    @PostMapping("/new")
    public String nuevaPeliculaSubmit(@Valid @ModelAttribute("pelicula") PeliculaCreateDto pelicula,
                                      BindingResult bindingResult, HttpSession session) {
        if (bindingResult.hasErrors()) {
            session.setAttribute("formData_pelicula_new", pelicula);
            return "peliculas/form";
        }
        session.removeAttribute("formData_pelicula_new");
        peliculasService.save(pelicula);
        return "redirect:/peliculas/lista";
    }
}
```

- **model.addAttribute("page", peliculasPage)** → en la plantilla se usa `page` (content, totalPages, number, size, etc.).
- **return "peliculas/lista"** → Spring resuelve a `templates/peliculas/lista.peb.html` (según el sufijo .peb.html).
- **redirect:** evita reenvío del POST al recargar.

---

## 7.8 GlobalControllerAdvice (variables globales para vistas)

Para no repetir en cada controller datos como el usuario actual o si está autenticado, en Películas se usa un **@ControllerAdvice** con **@ModelAttribute**:

```java
@ControllerAdvice
public class GlobalControllerAdvice {
    @ModelAttribute("currentUser")
    public User getCurrentUser(Authentication authentication) { ... }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(Authentication authentication) { ... }

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) { ... }

    @ModelAttribute("csrfToken")
    public String getCsrfToken(HttpServletRequest request) { ... }
}
```

Así en cualquier plantilla se puede usar `{{ isAuthenticated }}`, `{% if isAdmin %}`, `{{ currentUser.nombre }}`, etc.

---

## 7.9 Resumen para el examen

1. **Pebble**: motor de plantillas; archivos `.peb.html` en `templates/`.
2. **extends** + **block**: layout único y páginas que rellenan bloques.
3. **include**: fragmentos (navbar, footer, mensajes, listas).
4. **macro**: funciones reutilizables (pager, inputField).
5. **Controller** devuelve string (nombre de vista) y usa **Model** para datos.
6. **Formularios**: token CSRF en un input hidden; nombres de campo alineados con el DTO (`pelicula.titulo`, etc.) y **@ModelAttribute** para binding y validación con **BindingResult**.

Con esto tienes el flujo MVC con Pebble tal como en PELICULAS REPO.
