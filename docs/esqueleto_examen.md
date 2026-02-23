# 🏆 Esqueleto Definitivo de Examen – Spring Boot Profesional

Este documento contiene el esqueleto completo de un examen profesional de Spring Boot, alineado con los proyectos **DWES 25-26** (tarjetas/titulares) y **PELICULAS REPO** (películas/entradas). Incluye JWT, WebSocket (sin STOMP), paginación, Pebble y tests.

---

## 1️⃣ Estructura Base del Proyecto

<!-- CORREGIDO: estructura por dominio y nombres de paquetes como en los proyectos -->
```text
src/main/java/es.xxx.app/
├── Application.java
├── config/
│   ├── auth/              # SecurityConfig, JwtAuthenticationFilter
│   └── websockets/        # WebSocketConfig, WebSocketHandler, WebSocketSender
├── recurso/               # por dominio (tarjetas, peliculas, entradas...)
│   ├── controllers/       # XxxRestController
│   ├── dto/               # XxxCreateDto, XxxUpdateDto, XxxResponseDto
│   ├── exceptions/        # XxxNotFoundException, XxxBadRequestException
│   ├── mappers/           # XxxMapper
│   ├── models/            # entidad JPA
│   ├── repositories/      # XxxRepository
│   └── services/          # XxxService (interface), XxxServiceImpl
├── auth/                  # solo si hay JWT
│   ├── controllers/       # AuthenticationRestController
│   ├── dto/               # UserSignInRequest, JwtAuthResponse
│   └── services/          # AuthenticationService, JwtService
├── utils/
│   └── pagination/        # PageResponse, PaginationLinksUtils
└── websockets/notifications/   # DTO y mappers de notificación (opcional)
```

---

## 2️⃣ Flujo Mental de Cualquier Endpoint

```text
HTTP → Controller → Service → Repository → DB
              ↓         ↓
           @Valid    Mapper (Entity ↔ DTO)
           DTOs
```

> El controller no usa el repositorio ni devuelve entidades; solo DTOs y servicio.

---

## 3️⃣ Plantilla JWT – Examen Real (según PELICULAS REPO)

### 3.1 Componentes mínimos

<!-- CORREGIDO: nombres y ubicación como en los proyectos -->
```text
config/auth/
├── JwtAuthenticationFilter
├── SecurityConfig
└── (opcional) LoginSuccessHandler

rest/auth/
├── controllers/   AuthenticationRestController
├── dto/           UserSignInRequest, UserSignUpRequest, JwtAuthResponse
└── services/      AuthenticationService, JwtService (auth/services/jwt/)
```

### 3.2 Flujo JWT mental

```text
POST /api/v1/auth/signin → AuthenticationManager → JwtService.generateToken
       → { "token": "eyJ..." }

Peticiones: Header Authorization: Bearer <token>
       → JwtAuthenticationFilter → JwtService.extractUserName + isTokenValid
       → SecurityContextHolder.setAuthentication
       → Controller (+ @PreAuthorize si aplica)
```

### 3.3 AuthenticationRestController (esqueleto)

<!-- CORREGIDO: ruta signin, JwtAuthResponse, AuthenticationService, nombres de métodos -->
```java
@RestController
@RequestMapping("api/${api.version}/auth")
public class AuthenticationRestController {
    private final AuthenticationService authenticationService;

    @PostMapping("/signin")
    public ResponseEntity<JwtAuthResponse> signIn(@Valid @RequestBody UserSignInRequest request) {
        return ResponseEntity.ok(authenticationService.signIn(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<JwtAuthResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        return ResponseEntity.ok(authenticationService.signUp(request));
    }
}
```

### 3.4 JwtAuthenticationFilter

<!-- CORREGIDO: OncePerRequestFilter, JwtService + UserDetailsService, extractUserName + loadUserByUsername + isTokenValid, UserDetails en el token -->
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;  // ej. AuthUsersService

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String userName;
        try {
            userName = jwtService.extractUserName(jwt);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token no válido");
            return;
        }

        if (StringUtils.hasText(userName) && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authToken);
                SecurityContextHolder.setContext(context);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

### 3.5 SecurityConfig (cadena API con JWT)

<!-- CORREGIDO: addFilterBefore con jwtFilter, STATELESS, requestMatchers con api.version -->
```java
@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    @Value("${api.version}") private String apiVersion;

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**", "/ws/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(m -> m.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/" + apiVersion + "/auth/signin", "/api/" + apiVersion + "/auth/signup").permitAll()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/ws/**").permitAll())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

---

## 4️⃣ WebSocket – Plantilla de Examen (según DWES 25-26 / PELICULAS REPO)

> Los proyectos usan **WebSocket con TextWebSocketHandler**, no STOMP.

### 4.1 Configuración mínima

<!-- CORREGIDO: @EnableWebSocket + WebSocketConfigurer, registerWebSocketHandlers, addHandler con ruta /ws/v1/... -->
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${api.version}")
    private String apiVersion;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketTarjetasHandler(), "/ws/" + apiVersion + "/tarjetas");
    }

    @Bean
    public WebSocketHandler webSocketTarjetasHandler() {
        return new WebSocketHandler("Tarjetas");
    }
}
```

### 4.2 WebSocketHandler (TextWebSocketHandler)

<!-- CORREGIDO: Set<WebSocketSession>, afterConnectionEstablished/Closed, sendMessage(String) -->
```java
public class WebSocketHandler extends TextWebSocketHandler implements WebSocketSender {
    private final String entity;
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    public WebSocketHandler(String entity) { this.entity = entity; }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    public void sendMessage(String message) throws IOException {
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) s.sendMessage(new TextMessage(message));
        }
    }
}
```

### 4.3 Envío de notificaciones desde el Service

<!-- CORREGIDO: no SimpMessagingTemplate; se obtiene el handler y se llama sendMessage(json) -->
```java
// En XxxServiceImpl: tras save/update/delete
private final WebSocketConfig webSocketConfig;
private final ObjectMapper objectMapper;
private WebSocketHandler webSocketHandler;  // se obtiene en afterPropertiesSet

public void afterPropertiesSet() {
    this.webSocketHandler = webSocketConfig.webSocketTarjetasHandler();
}

void onChange(Notificacion.Tipo tipo, Tarjeta data) {
    Notificacion<XxxNotificationResponse> notif = new Notificacion<>("TARJETAS", tipo, mapper.toDto(data), now);
    String json = objectMapper.writeValueAsString(notif);
    webSocketHandler.sendMessage(json);
}
```

Conexión cliente: `ws://localhost:3000/ws/v1/tarjetas`

---

## 5️⃣ Paginación + Filtros + Ordenación (según proyectos)

### 5.1 Repositorio

<!-- CORREGIDO: JpaSpecificationExecutor para filtros opcionales; Pageable -->
```java
@Repository
public interface PeliculasRepository extends JpaRepository<Pelicula, Long>, JpaSpecificationExecutor<Pelicula> {
    // Con Specification en el service para filtros opcionales (titulo, genero...)
}
```

### 5.2 Service (Specification + Page)

```java
public Page<PeliculaResponseDto> findAll(Optional<String> titulo, Optional<String> genero, Pageable pageable) {
    Specification<Pelicula> specTitulo = (root, q, cb) -> titulo
        .map(t -> cb.like(cb.lower(root.get("titulo")), "%" + t.toLowerCase() + "%"))
        .orElseGet(() -> cb.isTrue(cb.literal(true)));
    Specification<Pelicula> specGenero = (root, q, cb) -> genero
        .map(g -> cb.equal(cb.lower(root.get("genero")), g.toLowerCase()))
        .orElseGet(() -> cb.isTrue(cb.literal(true)));
    Specification<Pelicula> criterio = Specification.allOf(specTitulo, specGenero);
    return repository.findAll(criterio, pageable).map(mapper::toPeliculaResponseDto);
}
```

### 5.3 Controlador

<!-- CORREGIDO: PageResponse, sortBy/direction, PaginationLinksUtils, cabecera Link -->
```java
@GetMapping
public ResponseEntity<PageResponse<PeliculaResponseDto>> getAll(
        @RequestParam(required = false) Optional<String> titulo,
        @RequestParam(required = false) Optional<String> genero,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "idPelicula") String sortBy,
        @RequestParam(defaultValue = "asc") String direction,
        HttpServletRequest request) {
    Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<PeliculaResponseDto> pageResult = peliculasService.findAll(titulo, genero, pageable);
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(request.getRequestURL().toString());
    return ResponseEntity.ok()
        .header("link", paginationLinksUtils.createLinkHeader(pageResult, uriBuilder))
        .body(PageResponse.of(pageResult, sortBy, direction));
}
```

### 5.4 Llamada real

```text
GET /api/v1/peliculas?page=0&size=10&sortBy=idPelicula&direction=asc&titulo=matrix
```

---

## 6️⃣ Pebble – Plantilla MVC (según PELICULAS REPO)

### 6.1 Controlador MVC

<!-- CORREGIDO: @RequestMapping("peliculas") sin /web, return "peliculas/lista", Page para lista paginada -->
```java
@Controller
@RequestMapping("peliculas")
public class PeliculasController {
    private final PeliculasService peliculasService;

    @GetMapping({"", "/", "/lista"})
    public String listar(Model model,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "4") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("idPelicula").ascending());
        Page<PeliculaResponseDto> peliculasPage = peliculasService.findAll(Optional.empty(), Optional.empty(), pageable);
        model.addAttribute("page", peliculasPage);
        return "peliculas/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("pelicula", peliculasService.findById(id));
        return "peliculas/detalle";
    }
}
```

### 6.2 Layout (fragments/layout.peb.html)

<!-- CORREGIDO: block body como en el proyecto; include fragments -->
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
    {% block scripts %}{% endblock %}
</body>
</html>
```

### 6.3 Vista lista (peliculas/lista.peb.html)

<!-- CORREGIDO: extends "fragments/layout", block body, nombre archivo .peb.html, variable page -->
```html
{% extends "fragments/layout" %}

{% block body %}
<div class="container">
    <h2>Lista de películas</h2>
    {% include "fragments/listaPeliculas" %}
    {% if page.totalPages > 1 %}
    <nav>
        <a href="?page={{ page.number - 1 }}&size={{ page.size }}">Anterior</a>
        <a href="?page={{ page.number + 1 }}&size={{ page.size }}">Siguiente</a>
    </nav>
    {% endif %}
</div>
{% endblock %}
```

Sufijo en `application.properties`: `pebble.suffix=.peb.html`

---

## 7️⃣ Tests – Esqueleto de Examen (según proyectos)

### 7.1 Test de controlador REST

<!-- CORREGIDO: @SpringBootTest + @AutoConfigureMockMvc, @MockitoBean del Service, endpoint api/v1/... -->
```java
@SpringBootTest
@AutoConfigureMockMvc
class PeliculasRestControllerTest {

    private static final String ENDPOINT = "/api/v1/peliculas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PeliculasService peliculasService;

    @Test
    void getById_Exists() throws Exception {
        when(peliculasService.findById(1L)).thenReturn(peliculaResponseDto);

        mockMvc.perform(get(ENDPOINT + "/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idPelicula").value(1));

        verify(peliculasService, times(1)).findById(1L);
    }

    @Test
    void getById_NotFound() throws Exception {
        when(peliculasService.findById(999L)).thenThrow(new PeliculaNotFoundException(999L));

        mockMvc.perform(get(ENDPOINT + "/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_AsAdmin_Returns201() throws Exception {
        when(peliculasService.save(any(PeliculaCreateDto.class))).thenReturn(peliculaResponseDto);

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(jsonCreate))
            .andExpect(status().isCreated());
    }
}
```

### 7.2 Test de repositorio

```java
@DataJpaTest
@Sql(scripts = "/reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PeliculasRepositoryTest {

    @Autowired
    private PeliculasRepository repository;

    @Test
    void findByTitulo() {
        Page<Pelicula> page = repository.findAll(PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
    }
}
```

### 7.3 Test de servicio (unitario)

```java
@ExtendWith(MockitoExtension.class)
class PeliculasServiceImplTest {

    @Mock
    private PeliculasRepository repository;
    @Mock
    private PeliculaMapper mapper;
    @InjectMocks
    private PeliculasServiceImpl service;

    @Test
    void findById_NotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(999L))
            .isInstanceOf(PeliculaNotFoundException.class);
    }
}
```

---

## 8️⃣ Orden de Ataque en Examen

1. Entidades + Repository (+ JpaSpecificationExecutor si filtros)
2. DTOs + Mapper + Service + RestController (CRUD)
3. PageResponse + PaginationLinksUtils + paginación y filtros en GET lista
4. JWT (JwtService, Filtro, SecurityConfig, auth/signin)
5. WebSocket (WebSocketConfig, WebSocketHandler, envío desde Service)
6. Pebble (Controller MVC, layout, vistas)
7. Tests (controller con MockitoBean, servicio con @Mock)

---

## 9️⃣ Checklist Final de Examen

- [ ] API bajo `api/${api.version}/recursos` (api.version en properties)
- [ ] Controller no usa Repository; solo Service. Respuestas en DTOs (nunca entidad)
- [ ] POST → 201, DELETE → 204, validación con @Valid y manejo 400/404
- [ ] JWT: signin devuelve `{ "token": "..." }`; filtro rellena SecurityContext; @PreAuthorize donde aplique
- [ ] Paginación: Pageable, PageResponse, opcional cabecera Link (PaginationLinksUtils)
- [ ] WebSocket: @EnableWebSocket + WebSocketConfigurer; handler con sendMessage(String); servicio llama sendMessage tras create/update/delete
- [ ] Pebble: extends "fragments/layout", block body; rutas como "peliculas/lista"; sufijo .peb.html
- [ ] Tests: al menos GET por id (200/404), POST (201), DELETE (204); mock del Service en tests de controller
