# Guía: API REST con Spring Boot desde cero (para el examen)

Esta guía explica **qué hace cada pieza** y **en qué orden montar un proyecto similar** para que puedas hacerlo en el examen sin perderte.

---

## 1. La idea general: ¿qué estamos construyendo?

Imagina que tu aplicación es un **restaurante**:

| Capa | Qué es | En el restaurante |
|------|--------|-------------------|
| **Cliente** (Postman, navegador, app móvil) | Quien pide cosas | El cliente que pide el menú |
| **Controller** | Recibe la petición y decide qué hacer | El camarero: recibe el pedido y lo lleva a cocina |
| **Service** | La lógica de negocio (reglas, validaciones) | El cocinero: prepara el plato según las reglas |
| **Repository** | Habla con la base de datos | La despensa: guarda y saca ingredientes (datos) |
| **Entity (Model)** | Cómo se guarda un “objeto” en la BD | La receta de un plato (estructura de datos) |
| **DTO** | Lo que enviamos/recibimos por la API (sin exponer la entidad) | El menú que ve el cliente (versión “limpia”) |

**Flujo de una petición** (ejemplo: “dame la película con id 5”):

1. Llega `GET /api/v1/peliculas/5` al **Controller**.
2. El Controller llama al **Service**: `peliculasService.findById(5)`.
3. El Service llama al **Repository**: `peliculasRepository.findById(5)`.
4. El Repository devuelve la **Entity** (o vacío).
5. El Service convierte la Entity a **DTO** con el **Mapper** y la devuelve (o lanza excepción 404).
6. El Controller devuelve el DTO al cliente con status 200 (o 404 si no existe).

No hace falta memorizar todo: con que entiendas **Controller → Service → Repository** y **Entity / DTO / Mapper** tienes el 80 %.

---

## 2. Orden para montar el proyecto (y para el examen)

Sigue este orden. Cada paso depende del anterior.

```
1. Entidad (modelo JPA)
2. Repository (interface que extiende JpaRepository)
3. DTOs (request + response) y Mapper
4. Excepción "no encontrado" (404)
5. Service (interface + implementación)
6. Controller (REST)
7. (Opcional) Manejo de validación 400 y excepciones globales
```

---

## 3. Paso 1: La entidad (modelo)

**Qué es:** La clase que representa **una fila de una tabla** en la base de datos. JPA/Hibernate la usa para crear la tabla y mapear filas a objetos.

**Qué poner:**

- `@Entity` y `@Table(name = "nombre_tabla")`
- `@Id` y `@GeneratedValue(strategy = GenerationType.IDENTITY)` en el id
- `@Column(nullable = false)` (o sin nullable) en los campos
- Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`
- Relaciones si las hay: `@OneToMany`, `@ManyToOne`, `@ManyToMany`

**Ejemplo (resumido) de tu proyecto – `Pelicula.java`:**

```java
@Entity
@Table(name = "peliculas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pelicula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPelicula;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private Integer duracion;

    // ... más campos
}
```

**En el examen:** Define primero la entidad con todos los campos que pidan (id, nombre, fecha, etc.) y las relaciones. Sin entidad no hay tabla ni repositorio.

---

## 4. Paso 2: El repositorio

**Qué es:** Una **interface** que extiende `JpaRepository<Entidad, TipoDelId>`. Spring implementa por ti los métodos `findById`, `save`, `findAll`, `delete`, etc. Tú solo declaras métodos extra si los necesitas (por nombre o con `@Query`).

**Qué poner:**

- `extends JpaRepository<Pelicula, Long>`
- Métodos por “nombre derivado”: por ejemplo `findByTitulo(String titulo)`, `findByGenero(String genero)`, `Page<Pelicula> findAll(Pageable pageable)` para paginación.

**Ejemplo – `PeliculasRepository.java`:**

```java
@Repository
public interface PeliculasRepository extends JpaRepository<Pelicula, Long> {
    List<Pelicula> findByTitulo(String titulo);
    Page<Pelicula> findAll(Pageable pageable);  // para paginación
}
```

**En el examen:** Crea el repositorio justo después de la entidad. Si piden “consultas derivadas”, usa nombres como `findByCampo` o `findByCampoAndOtroCampo`.

---

## 5. Paso 3: DTOs y Mapper

**Por qué:** No exponemos la entidad directamente en la API (seguridad, control de campos, evitar lazy loading en JSON). Enviamos y recibimos **DTOs** (objetos planos).

- **DTO de petición (Create/Update):** lo que el cliente envía en el body (sin id, con validaciones).
- **DTO de respuesta (Response):** lo que devolvemos (con id, fechas, etc.).
- **Mapper:** clase con métodos que convierten Entity ↔ DTO.

**DTO de creación (ejemplo) – `PeliculaCreateDto.java`:**

- Campos que piden en el enunciado.
- Validaciones: `@NotBlank`, `@NotNull`, `@Size`, `@Positive`, etc.

**DTO de respuesta (ejemplo) – `PeliculaResponseDto.java`:**

- Los campos que quieres devolver (id, título, género, etc.). Nada sensible.

**Mapper (ejemplo) – `PeliculaMapper.java`:**

- `toPelicula(PeliculaCreateDto dto)` → crea una **nueva** entidad (id = null).
- `toPelicula(PeliculaUpdateDto dto, Pelicula existente)` → actualiza la entidad existente.
- `toPeliculaResponseDto(Pelicula entity)` → entidad a DTO de respuesta.

**En el examen:** Piden “al menos un DTO de petición y uno de respuesta” y “mapper con conversiones”. Haz estos tres: CreateDto, ResponseDto y Mapper.

---

## 6. Paso 4: Excepción “no encontrado” (404)

**Qué es:** Una excepción que lanzas cuando no existe el recurso (por ejemplo película con id X). Spring la convierte en respuesta HTTP 404 si la anotas.

**Qué poner:**

- Clase que extiende `RuntimeException` (o una base como `PeliculaException`).
- `@ResponseStatus(HttpStatus.NOT_FOUND)` en la clase.

**Ejemplo – `PeliculaNotFoundException.java`:**

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PeliculaNotFoundException extends RuntimeException {
    public PeliculaNotFoundException(Long id) {
        super("Película con id " + id + " no encontrada");
    }
}
```

**Uso en el servicio:**  
`peliculasRepository.findById(id).orElseThrow(() -> new PeliculaNotFoundException(id));`

**En el examen:** Si piden “recurso no encontrado → 404”, crea una excepción así y lánzala en el servicio cuando `findById` no tenga resultado.

---

## 7. Paso 5: El servicio

**Qué es:** La capa de **lógica de negocio**. El controlador no habla con el repositorio; habla con el servicio. El servicio usa repositorio + mapper y lanza excepciones cuando toque.

**Qué poner:**

- **Interface** con los métodos que necesites: `findAll`, `findById`, `save`, `update`, `deleteById`.
- **Clase** `@Service` que implementa la interface, con `@RequiredArgsConstructor` e inyección del Repository y del Mapper.

**Lógica típica:**

- **findById(id):** `repository.findById(id).orElseThrow(() -> new PeliculaNotFoundException(id))` y luego convertir a DTO con el mapper.
- **save(dto):** convertir DTO a entidad con mapper, `repository.save(entidad)`, convertir a DTO de respuesta y devolverla.
- **update(id, dto):** buscar entidad (o lanzar 404), actualizar con mapper, `repository.save`, devolver DTO.
- **deleteById(id):** comprobar que existe (findById y lanzar 404 si no), luego `repository.deleteById(id)`.

**En el examen:** El servicio es el “cerebro”: aquí va la regla “si no existe → 404” y las conversiones Entity ↔ DTO.

---

## 8. Paso 6: El controlador REST

**Qué es:** La clase que **recibe las peticiones HTTP** y devuelve respuestas. Cada método es un endpoint.

**Anotaciones importantes:**

- Clase: `@RestController`, `@RequestMapping("api/v1/peliculas")` (o la ruta que pidan), `@RequiredArgsConstructor`.
- Métodos:
  - `@GetMapping()` → listar
  - `@GetMapping("/{id}")` → buscar por id
  - `@PostMapping()` → crear (body = DTO de creación)
  - `@PutMapping("/{id}")` → actualizar completo
  - `@PatchMapping("/{id}")` → actualizar parcial
  - `@DeleteMapping("/{id}")` → borrar

**Qué hace cada método (patrón):**

- Recibir parámetros (`@PathVariable Long id`, `@RequestBody PeliculaCreateDto dto`, etc.).
- Llamar al servicio (una sola línea en muchos casos).
- Devolver `ResponseEntity.ok(...)` (200), `ResponseEntity.status(HttpStatus.CREATED).body(...)` (201), o `ResponseEntity.noContent().build()` (204).

**Ejemplo GET por id:**

```java
@GetMapping("/{id}")
public ResponseEntity<PeliculaResponseDto> getById(@PathVariable Long id) {
    return ResponseEntity.ok(peliculasService.findById(id));
}
```

**Ejemplo POST (crear):**

```java
@PostMapping()
public ResponseEntity<PeliculaResponseDto> create(@Valid @RequestBody PeliculaCreateDto dto) {
    PeliculaResponseDto saved = peliculasService.save(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
}
```

**Validación:** Usa `@Valid` en el body para que se validen las anotaciones del DTO. Si falla, Spring devuelve 400 (y puedes manejar `MethodArgumentNotValidException` para dar un mensaje claro).

**En el examen:** Define la ruta base y cada verbo (GET, POST, PUT/PATCH, DELETE) según el enunciado. Que el controlador sea fino: recibe, delega en el servicio, devuelve.

---

## 9. Paso 7 (opcional): Manejo de errores 400 y global

**400 Bad Request** cuando el body no es válido (validación del DTO):

- En el controlador (o en un `@RestControllerAdvice`) puedes poner un método con `@ExceptionHandler(MethodArgumentNotValidException.class)` que devuelva un `ProblemDetail` o un mapa con los errores de validación (`BindingResult`).

**404** ya lo tienes con la excepción `@ResponseStatus(NOT_FOUND)`.

**En el examen:** Si piden “recurso no encontrado → 404” y “petición errónea → 400”, haz la excepción 404 y, si da tiempo, un manejador para validación que devuelva 400.

---

## 10. Resumen: checklist “desde cero”

Para no perderte, sigue este orden y comprueba cada punto:

1. [ ] **Entidad:** `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, campos y Lombok.
2. [ ] **Repository:** interface `extends JpaRepository<Entidad, Long>` y métodos extra si los piden.
3. [ ] **DTOs:** CreateDto (con validaciones) y ResponseDto.
4. [ ] **Mapper:** toEntity(createDto), toEntity(updateDto, entidad), toResponseDto(entidad).
5. [ ] **Excepción 404:** clase con `@ResponseStatus(NOT_FOUND)`, lanzarla en servicio cuando no exista.
6. [ ] **Service:** interface + clase con findById, save, update, deleteById usando repository y mapper.
7. [ ] **Controller:** `@RestController`, rutas GET/POST/PUT o PATCH/DELETE que llamen al servicio y devuelvan ResponseEntity.
8. [ ] (Opcional) **Validación:** `@Valid` en el body y manejador de `MethodArgumentNotValidException` para 400.

---

## 11. Los “extras” del examen (notificaciones, paginación, JWT, Pebble, tests)

Estos temas suelen entrar todos en el examen de 2ª evaluación. Aquí tienes cada uno explicado para que sepas **qué es**, **qué hacer** y **dónde está en tu proyecto**.

---

### 11.1 Notificaciones con WebSockets

**Qué es:** Un canal de comunicación **en tiempo real** entre el servidor y los clientes (navegador, app). Cuando algo cambia en el servidor (por ejemplo se crea o actualiza una película), el servidor **envía un mensaje** a todos los clientes conectados sin que ellos tengan que refrescar.

**Qué tienes que montar (en orden):**

1. **Configuración (`WebSocketConfig`):**
   - Clase `@Configuration` con `@EnableWebSocket`.
   - Implementar `WebSocketConfigurer` y en `registerWebSocketHandlers` registrar un handler y una **ruta** (ej. `/ws/v1/peliculas`).
   - Un `@Bean` por cada tipo de notificación que devuelva un `WebSocketHandler("NombreEntidad")`.

2. **Handler (`WebSocketHandler`):**
   - Extender `TextWebSocketHandler` e implementar `WebSocketSender` (interfaz con `sendMessage(String message)`).
   - Mantener un `Set<WebSocketSession> sessions` para los clientes conectados.
   - En `afterConnectionEstablished`: añadir la sesión al set y opcionalmente enviar un mensaje de bienvenida.
   - En `afterConnectionClosed`: quitar la sesión del set.
   - En `sendMessage(String message)`: recorrer todas las sesiones y enviar `new TextMessage(message)` a cada una (solo si `session.isOpen()`).

3. **En el servicio (cuando creas/actualizas/borras):**
   - Después de `repository.save(...)` o antes de `repository.deleteById(...)`, llamar a un método tipo `onChange(Tipo.CREATE, entidad)`.
   - En `onChange`: construir un objeto de notificación (entidad, tipo CREATE/UPDATE/DELETE, fecha); convertirlo a JSON con `ObjectMapper.writeValueAsString(...)`; obtener el `WebSocketHandler` (inyectando `WebSocketConfig` y llamando a su método que devuelve el handler) y llamar a `webSocketHandler.sendMessage(json)`.
   - Para no bloquear la petición HTTP, puedes lanzar el envío en un hilo: `new Thread(() -> webSocketHandler.sendMessage(json)).start()`.

4. **DTO de notificación:** Un DTO ligero (solo los campos que quieras enviar) y un mapper Entidad → DTO de notificación. El mensaje que envías por WebSocket es un JSON con algo tipo: `{ "entity": "PELICULAS", "type": "CREATE", "data": { ... }, "createdAt": "..." }`.

**Dónde está en tu proyecto:**
- Config: `config/websockets/WebSocketConfig.java`
- Handler: `config/websockets/WebSocketHandler.java`
- Interfaz: `config/websockets/WebSocketSender.java`
- Uso en servicio: en `PeliculasServiceImpl` el método `onChange` y las llamadas tras `save`, `update`, `deleteById`
- Modelo notificación: `websockets/notifications/models/Notificacion.java`
- Mapper y DTO: `websockets/notifications/` (PeliculaNotificationMapper, PeliculaNotificationResponse)

**Qué te pueden pedir en el examen:** Configurar WebSocket con una ruta dada; enviar notificación al crear/actualizar/borrar un recurso; estructura del mensaje (entidad, tipo, datos, fecha). No suelen pedir el cliente en HTML/JS, solo el servidor.

---

### 11.2 Paginación, ordenación y criterios de selección (filtros)

**Qué es:**
- **Paginación:** Devolver los resultados por “páginas” (ej. página 0 con 10 elementos) en lugar de toda la lista.
- **Ordenación:** Ordenar por un campo (ej. `titulo`) y dirección `asc` o `desc`.
- **Criterios de selección (filtros):** Restringir resultados por parámetros (ej. `titulo=Matrix`, `genero=Drama`).

**Qué tienes que hacer:**

1. **Repositorio:**
   - Métodos que reciban `Pageable`: por ejemplo `Page<Entidad> findAll(Pageable pageable)`.
   - Si hay filtros, usar **consultas derivadas** (`findByTituloAndGenero(...)`) o **Specification** (objeto que construyes con criterios opcionales y pasas a `findAll(Specification, Pageable)`).

2. **Servicio:**
   - Recibir `Pageable` y los filtros (por ejemplo `Optional<String> titulo`, `Optional<String> genero`).
   - Si usas Specification: construir un `Specification` por cada filtro (si está presente) y combinarlos con `Specification.allOf(...)`; luego `repository.findAll(spec, pageable)`.
   - El resultado es un `Page<Entidad>`; mapear cada elemento a DTO y devolver `Page<DTO>` (con `page.map(mapper::toResponseDto)`).

3. **Controlador:**
   - Parámetros: `@RequestParam(defaultValue = "0") int page`, `@RequestParam(defaultValue = "10") int size`, `@RequestParam(defaultValue = "id") String sortBy`, `@RequestParam(defaultValue = "asc") String direction`, y los filtros como `@RequestParam(required = false) Optional<String> titulo`, etc.
   - Construir `Sort`: `Sort.by(sortBy).ascending()` o `.descending()` según `direction`.
   - Construir `Pageable`: `PageRequest.of(page, size, sort)`.
   - Llamar al servicio y recibir `Page<ResponseDto>`.
   - Devolver un **objeto de respuesta de página** (ej. `PageResponse`) que incluya: `content`, `totalPages`, `totalElements`, `pageNumber`, `size`, `first`, `last`, `sortBy`, `direction`.
   - **Cabecera Link (opcional pero típico en examen):** Construir enlaces `next`, `prev`, `first`, `last` con la misma URL base y distintos `page`/`size`, y ponerlos en la cabecera `Link` (con una utilidad tipo `PaginationLinksUtils`).

**Dónde está en tu proyecto:**
- Utilidades: `utils/pagination/PageResponse.java`, `utils/pagination/PaginationLinksUtils.java`
- Controlador: `PeliculasRestController.getAll` (parámetros page, size, sortBy, direction, titulo, genero)
- Servicio: `PeliculasServiceImpl.findAll` (Specification + Pageable)
- Repositorio: `PeliculasRepository` con `Page<...> findAll(Pageable)` y métodos con Specification

**Qué te pueden pedir:** GET con `page`, `size`, `sortBy`, `direction` y filtros opcionales; respuesta paginada (lista + metadatos); cabecera Link con next/prev/first/last; consultas derivadas o JPQL para filtros.

---

### 11.3 Seguridad con JWT

**Qué es:** El cliente se autentica una vez (login con usuario y contraseña); el servidor devuelve un **token JWT**. En las siguientes peticiones el cliente envía ese token (normalmente en la cabecera `Authorization: Bearer <token>`). El servidor valida el token y sabe “quién” es el usuario y qué roles tiene (USER, ADMIN).

**Qué tienes que montar (resumido):**

1. **Endpoint de login (ej. POST `/api/v1/auth/signin`):**
   - Recibir DTO con usuario/email y password.
   - Validar credenciales (con `AuthenticationManager` o tu `UserDetailsService` y `PasswordEncoder`).
   - Si son correctas: generar un JWT (incluyendo en el token el username y el rol) y devolverlo en un DTO (ej. `{ "token": "...", "type": "Bearer" }`). Status 200.
   - Si no: devolver 401.

2. **Filtro JWT (`JwtAuthenticationFilter`):**
   - Se ejecuta antes del controlador. Lee la cabecera `Authorization`, extrae el token, lo valida (firmatura, no expirado) y obtiene el usuario; luego pone en el contexto de seguridad un `UsernamePasswordAuthenticationToken` con ese usuario y sus roles. Así Spring Security “sabe” quién está autenticado.

3. **Configuración de seguridad (`SecurityConfig`):**
   - Para las rutas de la API: sesión stateless, desactivar CSRF, añadir el filtro JWT antes de `UsernamePasswordAuthenticationFilter`.
   - Rutas públicas: por ejemplo `/api/v1/auth/**`, `/ws/**`, opcionalmente GET de listados.
   - Rutas protegidas: el resto de la API (o solo POST/PUT/DELETE). Con `@PreAuthorize("hasRole('ADMIN')")` en un método del controlador, solo los usuarios con rol ADMIN pueden acceder.

**Dónde está en tu proyecto:**
- Login: `rest/auth/controllers/AuthenticationRestController.java` (signin/signup)
- Filtro: `config/auth/JwtAuthenticationFilter.java`
- Config: `config/auth/SecurityConfig.java`
- Servicio JWT (generar/validar token): `rest/auth/services/jwt/`
- Uso en controlador: `@PreAuthorize("hasRole('ADMIN')")` en PeliculasRestController en create/update/delete

**Qué te pueden pedir:** Endpoint de login que devuelva JWT; proteger ciertos endpoints (solo autenticados o solo ADMIN); 401 si no hay token o es inválido; 403 si no tiene permiso.

---

### 11.4 Frontend con motor de plantillas Pebble

**Qué es:** Generar **HTML en el servidor** con plantillas. El controlador no devuelve JSON sino el nombre de una vista; Spring resuelve la plantilla (Pebble), le pasa un modelo (datos) y devuelve HTML al navegador.

**Qué tienes que hacer:**

1. **Dependencia:** Pebble (por ejemplo `pebble-spring-boot-starter` o la que use tu proyecto) y configuración en `application.properties` (sufijo `.peb.html`, etc.).

2. **Controlador MVC (no REST):**
   - Clase con `@Controller` (no `@RestController`) y `@RequestMapping` para las rutas web (ej. `/peliculas`).
   - Métodos que devuelven `String`: el nombre de la vista (ej. `"peliculas/lista"` → plantilla `templates/peliculas/lista.peb.html`).
   - Usar `Model model` y `model.addAttribute("nombre", valor)` para pasar datos a la plantilla (lista de entidades, una entidad, página, etc.).

3. **Plantillas Pebble:**
   - Sintaxis: `{{ variable }}` para mostrar, `{% if %}`, `{% for %}`, `{% extends "layout" %}`, `{% block body %}`, `{% include "fragmento" %}`.
   - Ejemplo listado: recibir `page` o `peliculas`, hacer `{% for p in peliculas %} ... {{ p.titulo }} ... {% endfor %}` y enlaces a detalle `/peliculas/{{ p.id }}`.
   - Ejemplo detalle: recibir `pelicula` y mostrar `{{ pelicula.titulo }}`, `{{ pelicula.genero }}`, etc.
   - Formato fechas: filtro `date` si está disponible, ej. `{{ pelicula.createdAt | date("dd/MM/yyyy HH:mm") }}`.

4. **Reutilizar lógica:** El mismo servicio que usa el REST (por ejemplo `peliculasService.findById(id)`, `peliculasService.findAll(...)`). Convertir a DTO si ya tienes DTOs; la plantilla usa los getters (ej. `pelicula.titulo`).

**Dónde está en tu proyecto:**
- Controlador MVC: `web/controllers/PeliculasController.java` (lista, detalle, formularios)
- Plantillas: `resources/templates/peliculas/lista.peb.html`, `detalle.peb.html`, `form.peb.html`
- Fragmentos: `templates/fragments/layout.peb.html`, `pager.peb.html`, etc.

**Qué te pueden pedir:** Ruta MVC que muestre listado en HTML; ruta que muestre detalle de un recurso por id; plantilla que use variables del modelo (listas, objeto); enlaces entre listado y detalle.

---

### 11.5 Tests (repositorio, servicio, controlador)

**Tests de repositorio:**
- Anotación: `@DataJpaTest` (arranca solo la capa JPA y la BD en memoria).
- Inyectar el `Repository` y opcionalmente `TestEntityManager` para persistir datos de prueba.
- En cada test: preparar datos (persistir entidades), llamar al método del repositorio (findById, findByXxx, findAll con Pageable), hacer assertions (assertNotNull, assertEquals, assertTrue(optional.isPresent()), etc.).
- Si usas datos iniciales: `@Sql` para ejecutar un script antes del test.

**Tests de servicio:**
- Anotación: `@ExtendWith(MockitoExtension.class)` (o `@MockitoBean` en SpringBootTest si necesitas contexto).
- `@Mock` para el repositorio y dependencias (mapper, WebSocket, etc.); `@InjectMocks` para el servicio.
- En cada test: `when(repository.findById(1L)).thenReturn(Optional.of(entidad))`; llamar al método del servicio; `assertThat(resultado).isNotNull()` o similar; `verify(repository, times(1)).findById(1L)`.
- Casos: éxito (findById devuelve algo, save devuelve DTO); no encontrado (when(...).thenReturn(Optional.empty()), assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(NotFoundException.class)).

**Tests de controlador:**
- Anotación: `@SpringBootTest` + `@AutoConfigureMockMvc` (o usar solo MockMvc).
- Inyectar `MockMvcTester` (o `MockMvc`) y hacer `@MockitoBean` del **servicio** (no del repositorio), para simular la capa de negocio.
- Ejemplo GET por id: `when(peliculasService.findById(1L)).thenReturn(peliculaResponseDto)`; luego `mockMvcTester.get().uri("/api/v1/peliculas/1").exchange()`; `assertThat(result).hasStatusOk().bodyJson()...`.
- Ejemplo POST: body JSON del DTO, `when(peliculasService.save(any())).thenReturn(responseDto)`; `mockMvcTester.post().uri("/api/v1/peliculas").contentType(APPLICATION_JSON).body(requestDto).exchange()`; assert status 201 y cuerpo.
- Ejemplo 404: `when(peliculasService.findById(999L)).thenThrow(new PeliculaNotFoundException(999L))`; GET a `/api/v1/peliculas/999`; assert status 404.
- Si el endpoint requiere autenticación: `@WithMockUser` o `@WithMockUser(roles = "ADMIN")`.

**Dónde está en tu proyecto:**
- Repositorio: `test/.../repositories/PeliculasRepositoryTest.java` (@DataJpaTest, TestEntityManager)
- Servicio: `test/.../services/PeliculasServiceImplTest.java` (@ExtendWith(MockitoExtension.class), @Mock, @InjectMocks)
- Controlador: `test/.../controllers/PeliculasRestControllerTest.java` (MockMvcTester, @MockitoBean PeliculasService, AssertJ)

**Qué te pueden pedir:** Test de un método del repositorio (findByXxx, findAll con Pageable); test del servicio (éxito y no encontrado); test del controlador (GET 200, GET 404, POST 201, POST 400) con MockMvcTester/MockMvc y AssertJ; simular la capa de servicio (mock).

---

Si tienes claro el orden **Entidad → Repository → DTOs + Mapper → Excepción 404 → Service → Controller**, puedes montar un CRUD completo y luego añadir estos extras en el orden que pida el examen (normalmente paginación/filtros en el listado, luego WebSocket, JWT y Pebble, y por último tests).

---

## 12. Dónde está cada cosa en tu proyecto Películas

Para que puedas mirar ejemplos reales mientras estudias:

| Qué | Ruta en el proyecto |
|-----|----------------------|
| Entidad Película | `rest/peliculas/models/Pelicula.java` |
| Repositorio | `rest/peliculas/repositories/PeliculasRepository.java` |
| DTOs | `rest/peliculas/dto/` (PeliculaCreateDto, PeliculaResponseDto, PeliculaUpdateDto) |
| Mapper | `rest/peliculas/mappers/PeliculaMapper.java` |
| Excepción 404 | `rest/peliculas/exceptions/PeliculaNotFoundException.java` |
| Servicio | `rest/peliculas/services/PeliculasService.java` + `PeliculasServiceImpl.java` |
| Controlador REST | `rest/peliculas/controllers/PeliculasRestController.java` |
| Paginación | `utils/pagination/PageResponse.java`, `PaginationLinksUtils.java` |

Con esta guía y tu código como referencia puedes repasar cada capa en orden y practicar montando un CRUD de otra entidad (por ejemplo “Producto” o “Reserva”) desde cero. **Guía complementaria:** En **`GUIA_COMPLETA_EXTRAS_CON_EJEMPLOS.md`** tienes código completo de cada extra (WebSocket, paginación/filtros, JWT, Pebble, tests), ejemplos para entidades como Reserva o ServicioExtra, y tablas "si en el examen piden X, haz Y".

¡Mucho ánimo para mañana!
