# Guía completa: extras del examen con ejemplos y posibles cambios

Esta guía complementa la **GUIA_DESDE_CERO_EXAMEN.md**. Aquí tienes **código de ejemplo** y **cómo adaptarlo** cuando en el examen cambien entidades, atributos, rutas o requisitos.

---

## Cómo usar esta guía

- **Ejemplo base:** Se usa la entidad **Pelicula** (titulo, genero, duracion, etc.) como en tu proyecto.
- **Si cambian la entidad:** Sustituye mentalmente Pelicula → Reserva, ServicioExtra, Usuario, etc. y los nombres de atributos (titulo → nombre, fechaReserva, confirmado, etc.).
- **Posibles cambios:** Cada sección termina con una tabla "Si en el examen piden..." para que sepas qué tocar.

---

## 1. Notificaciones con WebSockets

### 1.1 Configuración (WebSocketConfig)

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${api.version}")
    private String apiVersion;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Una ruta por cada tipo de notificación
        registry.addHandler(webSocketPeliculasHandler(), "/ws/" + apiVersion + "/peliculas");
        // Si añades otra entidad (ej. reservas):
        // registry.addHandler(webSocketReservasHandler(), "/ws/" + apiVersion + "/reservas");
    }

    @Bean
    public WebSocketHandler webSocketPeliculasHandler() {
        return new WebSocketHandler("Peliculas");
    }
}
```

**Posibles cambios:**

| Si en el examen piden… | Qué hacer |
|------------------------|-----------|
| Ruta distinta (ej. `/ws/notificaciones`) | Cambiar el primer argumento de `addHandler`: `"/ws/notificaciones"`. |
| Notificar solo Reservas | Un solo handler `webSocketReservasHandler()` y ruta `/ws/v2/reservas`. |
| Notificar varias entidades | Varios `addHandler` con rutas distintas y un bean por cada handler. |

---

### 1.2 Handler (WebSocketHandler)

```java
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler implements WebSocketSender {
    private final String entity;
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    public WebSocketHandler(String entity) {
        this.entity = entity;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        session.sendMessage(new TextMessage("Conectado a notificaciones: " + entity));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    public void sendMessage(String message) throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }
}
```

**Posibles cambios:** Normalmente no cambia; el mismo handler sirve para cualquier entidad (solo cambia el nombre `entity` para logs).

---

### 1.3 Modelo de notificación y DTO

```java
// Notificacion.java (genérico, sirve para cualquier entidad)
public record Notificacion<T>(
    String entity,   // "PELICULAS", "RESERVAS", etc.
    Tipo type,       // CREATE, UPDATE, DELETE
    T data,          // DTO con los datos (PeliculaNotificationResponse, etc.)
    String createdAt
) {
    public enum Tipo { CREATE, UPDATE, DELETE }
}

// DTO específico de la entidad (solo campos que quieras enviar)
public record PeliculaNotificationResponse(
    Long idPelicula,
    String titulo,
    String genero,
    Integer duracion,
    String director,
    String createdAt,
    String updatedAt
) {}
```

**Si la entidad es Reserva:**

```java
public record ReservaNotificationResponse(
    Long idReserva,
    java.time.LocalDate fechaReserva,
    Integer cantidadPersonas,
    Boolean confirmado,
    Long usuarioId,
    String createdAt
) {}
```

---

### 1.4 Mapper entidad → DTO de notificación

```java
@Component
public class PeliculaNotificationMapper {
    public PeliculaNotificationResponse toNotificationDto(Pelicula p) {
        return new PeliculaNotificationResponse(
            p.getIdPelicula(),
            p.getTitulo(),
            p.getGenero(),
            p.getDuracion(),
            p.getDirector(),
            p.getCreatedAt() != null ? p.getCreatedAt().toString() : null,
            p.getUpdatedAt() != null ? p.getUpdatedAt().toString() : null
        );
    }
}
```

**Si la entidad es Reserva:** Crear `ReservaNotificationMapper` con método que reciba `Reserva` y devuelva `ReservaNotificationResponse` (mapeando idReserva, fechaReserva, cantidadPersonas, confirmado, usuario.getId(), etc.).

---

### 1.5 Uso en el servicio (onChange + save/update/delete)

```java
@Service
@RequiredArgsConstructor
public class PeliculasServiceImpl implements PeliculasService {
    private final PeliculasRepository repository;
    private final PeliculaMapper mapper;
    private final WebSocketConfig webSocketConfig;
    private final ObjectMapper objectMapper;
    private final PeliculaNotificationMapper notificationMapper;
    private WebSocketHandler webSocketService;

    @Override
    public void afterPropertiesSet() {
        this.webSocketService = webSocketConfig.webSocketPeliculasHandler();
    }

    @Override
    public PeliculaResponseDto save(PeliculaCreateDto dto) {
        Pelicula saved = repository.save(mapper.toPelicula(dto));
        onChange(Notificacion.Tipo.CREATE, saved);
        return mapper.toPeliculaResponseDto(saved);
    }

    @Override
    public PeliculaResponseDto update(Long id, PeliculaUpdateDto dto) {
        Pelicula actual = repository.findById(id).orElseThrow(() -> new PeliculaNotFoundException(id));
        Pelicula updated = repository.save(mapper.toPelicula(dto, actual));
        onChange(Notificacion.Tipo.UPDATE, updated);
        return mapper.toPeliculaResponseDto(updated);
    }

    @Override
    public void deleteById(Long id) {
        Pelicula toDelete = repository.findById(id).orElseThrow(() -> new PeliculaNotFoundException(id));
        repository.deleteById(id);
        onChange(Notificacion.Tipo.DELETE, toDelete);
    }

    void onChange(Notificacion.Tipo tipo, Pelicula data) {
        if (webSocketService == null) {
            webSocketService = webSocketConfig.webSocketPeliculasHandler();
        }
        try {
            Notificacion<PeliculaNotificationResponse> notif = new Notificacion<>(
                "PELICULAS",
                tipo,
                notificationMapper.toNotificationDto(data),
                java.time.LocalDateTime.now().toString()
            );
            String json = objectMapper.writeValueAsString(notif);
            new Thread(() -> {
                try {
                    webSocketService.sendMessage(json);
                } catch (IOException e) {
                    log.error("Error enviando WS", e);
                }
            }).start();
        } catch (JsonProcessingException e) {
            log.error("Error serializando notificación", e);
        }
    }
}
```

**Posibles cambios:**

| Si en el examen piden… | Qué hacer |
|------------------------|-----------|
| Entidad Reserva | Sustituir Pelicula → Reserva, PeliculasRepository → ReservasRepository, mapper/notificationMapper por los de Reserva; en `onChange` usar "RESERVAS" y ReservaNotificationResponse. |
| Solo notificar en create | Llamar a `onChange(CREATE, ...)` solo en `save`; quitar de `update` y `deleteById`. |
| Notificar también al actualizar ServicioExtra | En ServicioExtraServiceImpl, después de save/update, llamar a un `onChange` que use el handler de servicios extra. |

---

## 2. Paginación, ordenación y criterios de selección (filtros)

### 2.1 Repositorio: dos formas de filtrar

**Opción A – Consultas derivadas (cuando los filtros son pocos y fijos):**

```java
@Repository
public interface PeliculasRepository extends JpaRepository<Pelicula, Long> {

    Page<Pelicula> findAll(Pageable pageable);

    Page<Pelicula> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);
    Page<Pelicula> findByGeneroIgnoreCase(String genero, Pageable pageable);
    Page<Pelicula> findByTituloContainingIgnoreCaseAndGeneroIgnoreCase(
        String titulo, String genero, Pageable pageable);
}
```

**Opción B – Specification (filtros opcionales, cualquier combinación):**

```java
@Repository
public interface PeliculasRepository
    extends JpaRepository<Pelicula, Long>, JpaSpecificationExecutor<Pelicula> {
    // No hace falta declarar métodos; usas findAll(Specification, Pageable)
}
```

**Si la entidad es Reserva con filtros por usuario y confirmado:**

```java
// Consultas derivadas
Page<Reserva> findByUsuarioId(Long usuarioId, Pageable pageable);
Page<Reserva> findByConfirmado(Boolean confirmado, Pageable pageable);
Page<Reserva> findByUsuarioIdAndConfirmado(Long usuarioId, Boolean confirmado, Pageable pageable);
```

---

### 2.2 Servicio: con Specification (filtros opcionales)

```java
@Override
public Page<PeliculaResponseDto> findAll(
    Optional<String> titulo,
    Optional<String> genero,
    Pageable pageable) {

    Specification<Pelicula> specTitulo = (root, query, cb) ->
        titulo
            .map(t -> cb.like(cb.lower(root.get("titulo")), "%" + t.toLowerCase() + "%"))
            .orElseGet(() -> cb.isTrue(cb.literal(true)));

    Specification<Pelicula> specGenero = (root, query, cb) ->
        genero
            .map(g -> cb.equal(cb.lower(root.get("genero")), g.toLowerCase()))
            .orElseGet(() -> cb.isTrue(cb.literal(true)));

    Specification<Pelicula> criterio = Specification.allOf(specTitulo, specGenero);

    return repository.findAll(criterio, pageable).map(mapper::toPeliculaResponseDto);
}
```

**Si la entidad es Reserva (filtro por usuarioId y confirmado):**

```java
Specification<Reserva> specUsuario = (root, query, cb) ->
    usuarioId
        .map(id -> cb.equal(root.get("usuario").get("id"), id))
        .orElseGet(() -> cb.isTrue(cb.literal(true)));

Specification<Reserva> specConfirmado = (root, query, cb) ->
    confirmado
        .map(c -> cb.equal(root.get("confirmado"), c))
        .orElseGet(() -> cb.isTrue(cb.literal(true)));

Specification<Reserva> criterio = Specification.allOf(specUsuario, specConfirmado);
return repository.findAll(criterio, pageable).map(mapper::toResponseDto);
```

**Posibles cambios:**

| Si en el examen piden… | Qué hacer |
|------------------------|-----------|
| Filtro por un solo campo (ej. `confirmado=true`) | Un solo Specification o un solo método derivado `findByConfirmado(true, pageable)`. |
| Orden por otro campo (ej. `fechaReserva`) | En el controlador seguir usando `sortBy`; asegurarse de que la entidad tenga ese atributo y que el nombre coincida (ej. `fechaReserva`). |
| Sin filtros, solo paginación y ordenación | Servicio: `return repository.findAll(pageable).map(mapper::toResponseDto)`. |

---

### 2.3 Controlador: parámetros y PageResponse

```java
@GetMapping()
public ResponseEntity<PageResponse<PeliculaResponseDto>> getAll(
    @RequestParam(required = false) Optional<String> titulo,
    @RequestParam(required = false) Optional<String> genero,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "idPelicula") String sortBy,
    @RequestParam(defaultValue = "asc") String direction,
    HttpServletRequest request) {

    Sort sort = direction.equalsIgnoreCase("asc")
        ? Sort.by(sortBy).ascending()
        : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<PeliculaResponseDto> pageResult = peliculasService.findAll(titulo, genero, pageable);

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(request.getRequestURL().toString());
    String linkHeader = paginationLinksUtils.createLinkHeader(pageResult, uriBuilder);

    return ResponseEntity.ok()
        .header("link", linkHeader)
        .body(PageResponse.of(pageResult, sortBy, direction));
}
```

**PageResponse (record reutilizable):**

```java
public record PageResponse<T>(
    List<T> content,
    int totalPages,
    long totalElements,
    int pageSize,
    int pageNumber,
    int totalPageElements,
    boolean empty,
    boolean first,
    boolean last,
    String sortBy,
    String direction
) {
    public static <T> PageResponse<T> of(Page<T> page, String sortBy, String direction) {
        return new PageResponse<>(
            page.getContent(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.getSize(),
            page.getNumber(),
            page.getNumberOfElements(),
            page.isEmpty(),
            page.isFirst(),
            page.isLast(),
            sortBy,
            direction
        );
    }
}
```

**Posibles cambios:**

| Si en el examen piden… | Qué hacer |
|------------------------|-----------|
| Ruta `/api/v2/reservas` y filtros `usuario`, `confirmado` | `@RequestMapping("api/v2/reservas")`, parámetros `Optional<Long> usuario`, `Optional<Boolean> confirmado`, y pasarlos al servicio. |
| Orden por defecto por fecha descendente | `defaultValue = "fechaReserva"` y `defaultValue = "desc"`. |
| Sin cabecera Link | No llamar a `paginationLinksUtils.createLinkHeader` y no añadir `.header("link", ...)`. |

---

## 3. Seguridad con JWT

### 3.1 Endpoint de login

```java
@RestController
@RequestMapping("api/${api.version}/auth")
@RequiredArgsConstructor
public class AuthenticationRestController {
    private final AuthenticationService authenticationService;

    @PostMapping("/signin")
    public ResponseEntity<JwtAuthResponse> signIn(@Valid @RequestBody UserSignInRequest request) {
        return ResponseEntity.ok(authenticationService.signIn(request));
    }
}
```

El servicio de autenticación valida usuario/contraseña y, si son correctos, genera el JWT y lo devuelve en `JwtAuthResponse` (ej. `{ "token": "...", "type": "Bearer" }`). Si no, lanza excepción → 401.

**Posibles cambios:**

| Si en el examen piden… | Qué hacer |
|------------------------|-----------|
| Ruta `/api/v2/auth/login` | `@PostMapping("/login")` y ajustar la ruta base si usan v2. |
| Devolver además el usuario (sin password) | Añadir al DTO de respuesta un campo `user` con datos del usuario y rellenarlo en el servicio. |

---

### 3.2 Filtro JWT (resumen)

- Lee cabecera `Authorization: Bearer <token>`.
- Extrae el token, valida (firmatura, expiración) y obtiene el username.
- Carga `UserDetails` con tu `UserDetailsService`.
- Si el token es válido, pone `UsernamePasswordAuthenticationToken` en `SecurityContextHolder`.
- Si no hay token o es inválido: no pone nada (o devuelve 401).

**Posibles cambios:** Normalmente no tocas el filtro; lo que cambia es qué rutas son públicas o protegidas en SecurityConfig.

---

### 3.3 SecurityConfig: rutas públicas y protegidas

```java
// Rutas que no requieren JWT
.requestMatchers("/api/" + apiVersion + "/auth/**").permitAll()
.requestMatchers("/ws/**").permitAll()
.requestMatchers("/api/" + apiVersion + "/peliculas").permitAll()  // GET listado público

// El resto de la API requiere autenticación
.requestMatchers("/api/" + apiVersion + "/**").authenticated()

// Añadir el filtro JWT
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

**Proteger solo ciertos métodos (solo ADMIN puede crear/actualizar/borrar):**

```java
@PostMapping()
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<PeliculaResponseDto> create(@Valid @RequestBody PeliculaCreateDto dto) {
    ...
}
```

**Posibles cambios:**

| Si en el examen piden… | Qué hacer |
|------------------------|-----------|
| Solo usuarios autenticados pueden ver el listado | Quitar el permitAll del listado; dejar solo auth/** y ws/** públicos. |
| Solo ADMIN puede crear reservas | Añadir `@PreAuthorize("hasRole('ADMIN')")` en el POST (y PUT/DELETE si aplica) del controlador de reservas. |
| GET público, POST/PUT/DELETE con JWT | Mantener permitAll para GET y usar @PreAuthorize o .authenticated() para el resto. |

---

## 4. Frontend con Pebble

### 4.1 Controlador MVC (listado y detalle)

```java
@Controller
@RequestMapping("peliculas")
@RequiredArgsConstructor
public class PeliculasController {
    private final PeliculasService peliculasService;

    @GetMapping({"", "/", "/lista"})
    public String lista(
        Model model,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("idPelicula").ascending());
        Page<PeliculaResponseDto> pageResult = peliculasService.findAll(
            Optional.empty(), Optional.empty(), pageable);
        model.addAttribute("page", pageResult);
        return "peliculas/lista";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        PeliculaResponseDto pelicula = peliculasService.findById(id);
        model.addAttribute("pelicula", pelicula);
        return "peliculas/detalle";
    }
}
```

**Si la entidad es Reserva:**

- `RequestMapping("reservas")`, inyectar `ReservasService`, usar `reservasService.findAll(...)` y `reservasService.findById(id)`.
- `model.addAttribute("page", ...)` o `model.addAttribute("reservas", ...)` y `model.addAttribute("reserva", ...)`.
- Return `"reservas/lista"` y `"reservas/detalle"` (plantillas en `templates/reservas/`).

---

### 4.2 Plantilla listado (Pebble)

```html
{% extends "fragments/layout" %}
{% block body %}
<div class="container">
    <h2>Listado de películas</h2>
    <table class="table">
        <thead>
            <tr>
                <th>Id</th>
                <th>Título</th>
                <th>Género</th>
                <th>Duración</th>
                <th></th>
            </tr>
        </thead>
        <tbody>
            {% for p in page.content %}
            <tr>
                <td>{{ p.idPelicula }}</td>
                <td>{{ p.titulo }}</td>
                <td>{{ p.genero }}</td>
                <td>{{ p.duracion }}</td>
                <td><a href="/peliculas/{{ p.idPelicula }}">Ver</a></td>
            </tr>
            {% else %}
            <tr><td colspan="5">No hay datos</td></tr>
            {% endfor %}
        </tbody>
    </table>
</div>
{% endblock %}
```

**Si la entidad es Reserva (atributos: idReserva, fechaReserva, cantidadPersonas, confirmado):**

- `page.content` → misma estructura si pasas un `Page`; si pasas lista, usar `reservas` en el modelo y `{% for r in reservas %}`.
- Columnas: `{{ r.idReserva }}`, `{{ r.fechaReserva }}`, `{{ r.cantidadPersonas }}`, `{{ r.confirmado }}`.
- Enlace: `href="/reservas/{{ r.idReserva }}"`.

---

### 4.3 Plantilla detalle (Pebble)

```html
{% extends "fragments/layout" %}
{% block body %}
<div class="container">
    {% if pelicula is empty %}
    <p>No encontrada</p>
    {% else %}
    <h2>Detalle: {{ pelicula.titulo }}</h2>
    <dl class="row">
        <dt class="col-sm-3">ID</dt>
        <dd class="col-sm-9">{{ pelicula.idPelicula }}</dd>
        <dt class="col-sm-3">Género</dt>
        <dd class="col-sm-9">{{ pelicula.genero }}</dd>
        <dt class="col-sm-3">Duración</dt>
        <dd class="col-sm-9">{{ pelicula.duracion }} min</dd>
        <dt class="col-sm-3">Fecha creación</dt>
        <dd class="col-sm-9">{{ pelicula.createdAt | date("dd/MM/yyyy HH:mm") }}</dd>
    </dl>
    <a href="/peliculas/lista">Volver al listado</a>
    {% endif %}
</div>
{% endblock %}
```

**Posibles cambios:** Sustituir nombres de atributos y rutas por los de tu entidad (reserva, servicioExtra, usuario).

---

### 4.4 Tabla resumen: cambios típicos en entidad / rutas

| En el examen | Controlador MVC | Model (addAttribute) | Vista (return) | Plantilla (variables) |
|--------------|-----------------|----------------------|----------------|------------------------|
| Pelicula | `@RequestMapping("peliculas")` | `page`, `pelicula` | `peliculas/lista`, `peliculas/detalle` | `page.content`, `pelicula.titulo`, etc. |
| Reserva | `@RequestMapping("reservas")` | `page`, `reserva` | `reservas/lista`, `reservas/detalle` | `page.content`, `reserva.fechaReserva`, etc. |
| ServicioExtra | `@RequestMapping("servicios-extra")` | `page`, `servicio` | `servicios-extra/lista`, `servicios-extra/detalle` | `servicio.nombre`, `servicio.precio`, etc. |
| Usuario | `@RequestMapping("usuarios")` | `page`, `usuario` | `usuarios/lista`, `usuarios/detalle` | `usuario.nombre`, `usuario.email` (no password) |

---

## 5. Tests

### 5.1 Test de repositorio (@DataJpaTest)

```java
@DataJpaTest
@Sql(scripts = "/reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PeliculasRepositoryTest {

    @Autowired
    private PeliculasRepository repository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findById_existingId_returnsPelicula() {
        Pelicula p = Pelicula.builder().titulo("Test").genero("Drama").duracion(90).director("X").build();
        entityManager.persist(p);
        entityManager.flush();

        Optional<Pelicula> result = repository.findById(p.getIdPelicula());

        assertThat(result).isPresent();
        assertThat(result.get().getTitulo()).isEqualTo("Test");
    }

    @Test
    void findAll_withPageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());
        Page<Pelicula> page = repository.findAll(pageable);

        assertThat(page.getContent()).isNotNull();
        assertThat(page.getTotalPages()).isGreaterThanOrEqualTo(0);
    }
}
```

**Si la entidad es Reserva:** Usar `ReservasRepository`, `Reserva.builder()` con los campos que tenga la entidad (fechaReserva, cantidadPersonas, confirmado, usuario, etc.), y `repository.findByUsuarioId(...)` o el método que pidan probar.

---

### 5.2 Test de servicio (Mockito)

```java
@ExtendWith(MockitoExtension.class)
class PeliculasServiceImplTest {

    @Mock
    private PeliculasRepository repository;
    @Mock
    private PeliculaMapper mapper;
    @Mock
    private WebSocketConfig webSocketConfig;
    @Mock
    private WebSocketHandler webSocketHandler;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PeliculaNotificationMapper notificationMapper;

    @InjectMocks
    private PeliculasServiceImpl service;

    @BeforeEach
    void setUp() {
        service.setWebSocketService(webSocketHandler);
    }

    @Test
    void findById_whenExists_returnsDto() {
        Long id = 1L;
        Pelicula pelicula = Pelicula.builder().idPelicula(id).titulo("X").genero("Drama").duracion(90).director("Y").build();
        PeliculaResponseDto dto = PeliculaResponseDto.builder().idPelicula(id).titulo("X").build();

        when(repository.findById(id)).thenReturn(Optional.of(pelicula));
        when(mapper.toPeliculaResponseDto(pelicula)).thenReturn(dto);

        PeliculaResponseDto result = service.findById(id);

        assertThat(result).isNotNull();
        assertThat(result.getTitulo()).isEqualTo("X");
        verify(repository).findById(id);
    }

    @Test
    void findById_whenNotExists_throws() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
            .isInstanceOf(PeliculaNotFoundException.class);

        verify(repository).findById(999L);
    }
}
```

**Posibles cambios:**

| Si en el examen piden… | Qué hacer |
|------------------------|-----------|
| Probar save | when(repository.save(any())).thenReturn(entidadGuardada); when(mapper.toPelicula(any())).thenReturn(entidad); when(mapper.toPeliculaResponseDto(any())).thenReturn(responseDto); assertThat(service.save(createDto)).isEqualTo(responseDto). |
| Entidad Reserva | ReservasRepository, ReservaMapper, ReservaNotFoundException; mismos patrones con Reserva y ReservaResponseDto. |
| No mockear WebSocket | Puedes usar @Mock y setWebSocketService en setUp para que no lance NPE al llamar a onChange. |

---

### 5.3 Test de controlador (MockMvcTester + AssertJ)

```java
@SpringBootTest
@AutoConfigureMockMvc
class PeliculasRestControllerTest {

    private static final String ENDPOINT = "/api/v1/peliculas";

    @Autowired
    private MockMvcTester mockMvcTester;
    @MockitoBean
    private PeliculasService peliculasService;

    private final PeliculaResponseDto dto1 = PeliculaResponseDto.builder()
        .idPelicula(1L)
        .titulo("El Padrino")
        .genero("Drama")
        .duracion(175)
        .build();

    @Test
    void getById_whenExists_returns200AndBody() {
        when(peliculasService.findById(1L)).thenReturn(dto1);

        var result = mockMvcTester.get()
            .uri(ENDPOINT + "/1")
            .exchange();

        assertThat(result)
            .hasStatusOk()
            .bodyJson()
            .convertTo(PeliculaResponseDto.class)
            .isEqualTo(dto1);
        verify(peliculasService).findById(1L);
    }

    @Test
    void getById_whenNotExists_returns404() {
        when(peliculasService.findById(999L)).thenThrow(new PeliculaNotFoundException(999L));

        var result = mockMvcTester.get()
            .uri(ENDPOINT + "/999")
            .exchange();

        assertThat(result).hasStatus4xxClientError();
        verify(peliculasService).findById(999L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void post_whenValid_returns201() {
        PeliculaCreateDto createDto = PeliculaCreateDto.builder()
            .titulo("Nueva").genero("Drama").duracion(100).director("Yo").build();
        when(peliculasService.save(any())).thenReturn(dto1);

        var result = mockMvcTester.post()
            .uri(ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .body(createDto)
            .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        verify(peliculasService).save(any(PeliculaCreateDto.class));
    }
}
```

**Posibles cambios:**

| Si en el examen piden… | Qué hacer |
|------------------------|-----------|
| Ruta `/api/v2/reservas` | ENDPOINT = "/api/v2/reservas"; uri(ENDPOINT + "/1"), etc. |
| Probar filtros en GET | when(service.findAll(Optional.of("Matrix"), Optional.empty(), pageable)).thenReturn(page); mockMvcTester.get().uri(ENDPOINT + "?titulo=Matrix").exchange(); assert content. |
| Entidad Reserva | ReservaResponseDto, ReservaCreateDto, ReservasService, ReservaNotFoundException; mismo patrón. |
| Endpoint protegido por ADMIN | Añadir @WithMockUser(roles = "ADMIN") al test del POST/PUT/DELETE; sin rol o con USER puede devolver 403. |

---

## 6. Tabla general: si cambian entidades o atributos

Usa esta tabla para no perderte al adaptar todo el proyecto a una entidad nueva (ej. Reserva, ServicioExtra, Usuario).

| Capa / concepto | Pelicula (ejemplo) | Reserva (ejemplo) | ServicioExtra (ejemplo) |
|-----------------|--------------------|-------------------|--------------------------|
| Entidad | Pelicula (idPelicula, titulo, genero, duracion, ...) | Reserva (idReserva, fechaReserva, cantidadPersonas, confirmado, usuario) | ServicioExtra (id, nombre, precio, disponible) |
| Repositorio | PeliculasRepository | ReservasRepository | ServiciosExtraRepository |
| DTOs | PeliculaCreateDto, PeliculaResponseDto, PeliculaUpdateDto | ReservaCreateDto, ReservaResponseDto, ReservaUpdateDto | ServicioExtraCreateDto, ServicioExtraResponseDto |
| Mapper | PeliculaMapper | ReservaMapper | ServicioExtraMapper |
| Excepción 404 | PeliculaNotFoundException(id) | ReservaNotFoundException(id) | ServicioExtraNotFoundException(id) |
| Servicio | PeliculasService / PeliculasServiceImpl | ReservasService / ReservasServiceImpl | ServiciosExtraService / ServiciosExtraServiceImpl |
| Controlador REST | PeliculasRestController, `/api/v1/peliculas` | ReservasRestController, `/api/v2/reservas` | ServiciosExtraRestController, `/api/v2/servicios-extra` |
| Controlador MVC | PeliculasController, `/peliculas` | ReservasController, `/reservas` | ServiciosExtraController, `/servicios-extra` |
| Plantillas | peliculas/lista, peliculas/detalle | reservas/lista, reservas/detalle | servicios-extra/lista, servicios-extra/detalle |
| WebSocket | WebSocketHandler("Peliculas"), /ws/v1/peliculas | WebSocketHandler("Reservas"), /ws/v2/reservas | WebSocketHandler("ServiciosExtra"), /ws/v2/servicios-extra |
| Notificación DTO | PeliculaNotificationResponse | ReservaNotificationResponse | ServicioExtraNotificationResponse |
| Tests | PeliculasRepositoryTest, PeliculasServiceImplTest, PeliculasRestControllerTest | Mismo patrón con Reserva* | Mismo patrón con ServicioExtra* |

---

## 7. Orden recomendado en el examen

1. Definir **entidades** (atributos que pidan; si cambian nombres, usa esta guía para sustituir en todo).
2. **Repositorio** (JpaRepository + métodos derivados o Specification si hay filtros).
3. **DTOs** y **mapper** (Create, Response, Update; mapper entidad ↔ DTOs).
4. **Excepción** 404 y uso en servicio (orElseThrow).
5. **Servicio** (interface + impl: findAll con Pageable/filtros, findById, save, update, deleteById).
6. **Controlador REST** (GET listado con paginación/filtros/ordenación, GET por id, POST, PUT/PATCH, DELETE; PageResponse y cabecera Link si piden).
7. **WebSocket**: config + handler + onChange en servicio + DTO/mapper de notificación.
8. **JWT**: login + SecurityConfig + @PreAuthorize donde pidan.
9. **Pebble**: controlador MVC + plantillas lista y detalle.
10. **Tests**: repositorio, servicio, controlador (al menos un caso de éxito y uno de 404/no encontrado).

Con esta guía y la **GUIA_DESDE_CERO_EXAMEN.md** puedes repasar cada punto con ejemplos y adaptar rápido si cambian entidades, atributos, rutas o requisitos en el enunciado.
