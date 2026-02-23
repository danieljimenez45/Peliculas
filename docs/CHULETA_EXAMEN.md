# CHULETA EXAMEN — DWES 2º DAW (repaso rápido)

---

## 1. API REST con Spring Boot desde cero

### Orden de creación
1. **pom.xml**: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `h2`.
2. **application.properties**: `server.port`, `api.version`, `spring.datasource.url=jdbc:h2:mem:nombrebd`, `spring.jpa.hibernate.ddl-auto=create-drop`, `spring.sql.init.mode=always`, `spring.jpa.defer-datasource-initialization=true`.
3. **Entity**: `@Entity`, `@Table`, `@Id`, `@GeneratedValue(IDENTITY)`, `@Column`, relaciones (`@ManyToOne` / `@OneToMany` con `mappedBy`).
4. **Repository**: `extends JpaRepository<Entity, Long>` (+ `JpaSpecificationExecutor<Entity>` si hay filtros).
5. **DTOs**: CreateDto, UpdateDto, ResponseDto (+ validaciones: `@NotBlank`, `@NotNull`, etc.).
6. **Mapper**: `toEntity(createDto)`, `toResponseDto(entity)`, `toEntity(updateDto, entity)`.
7. **Service** (interface + impl): usa repository + mapper; `findById(id).orElseThrow(() -> new NotFoundException(id))`.
8. **RestController**: `@RequestMapping("api/${api.version}/recursos")`, GET/POST/PUT/PATCH/DELETE, `@Valid @RequestBody`, `ResponseEntity` (201 POST, 204 DELETE, 200 resto).
9. **Excepciones**: NotFoundException → 404; manejo de `MethodArgumentNotValidException` → 400 con errores.

### Códigos HTTP
| Método  | Ruta        | Respuesta   |
|---------|-------------|-------------|
| GET     | /recursos   | 200 + body  |
| GET     | /recursos/{id} | 200 + body |
| POST    | /recursos   | **201** + body |
| PUT     | /recursos/{id} | 200 + body |
| DELETE  | /recursos/{id} | **204** sin body |

### Capas (no saltarse)
```
Controller → Service → Repository
     ↓           ↓
   DTOs      Entity + Mapper
```

---

## 2. JWT — Esquema mental

```
┌─────────────────────────────────────────────────────────────────┐
│  LOGIN (una vez)                                                │
│  POST /api/v1/auth/signin  { "username", "password" }            │
│       → AuthenticationManager.authenticate()                    │
│       → JwtService.generateToken(user)                           │
│       → Respuesta: { "token": "eyJ..." }   (200)                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  PETICIONES CON TOKEN                                            │
│  Header: Authorization: Bearer eyJ...                            │
│       → JwtAuthenticationFilter (OncePerRequestFilter)            │
│           1. Si no hay "Bearer " → filterChain.doFilter (sigue)   │
│           2. jwt = header.substring(7)                            │
│           3. userName = JwtService.extractUserName(jwt)           │
│           4. userDetails = AuthUsersService.loadUserByUsername()  │
│           5. JwtService.isTokenValid(jwt, userDetails)            │
│           6. SecurityContextHolder.setAuthentication(authToken)   │
│       → Controller (+ @PreAuthorize("hasRole('ADMIN')") si aplica)│
└─────────────────────────────────────────────────────────────────┘
```

### Piezas necesarias
- **application.properties**: `jwt.secret`, `jwt.expiration`.
- **JwtService**: `generateToken(UserDetails)`, `extractUserName(token)`, `isTokenValid(token, userDetails)`.
- **JwtAuthenticationFilter**: leer `Authorization`, extraer token, validar, rellenar `SecurityContext`; `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`.
- **SecurityConfig**: cadena API con `sessionManagement(STATELESS)`, `addFilterBefore(jwtFilter, ...)`.
- **User** implementa **UserDetails**; `getAuthorities()` devuelve `ROLE_USER` / `ROLE_ADMIN`.

### Resumen en una línea
Login devuelve token → cliente envía `Authorization: Bearer <token>` → filtro valida token y pone usuario en SecurityContext → los endpoints pueden usar @PreAuthorize.

---

## 3. WebSockets y notificaciones en tiempo real

### Pasos rápidos
1. **Dependencia**: `spring-boot-starter-websocket` (no STOMP).
2. **WebSocketConfig** (`@Configuration`, `@EnableWebSocket`, `WebSocketConfigurer`):
   - `registerWebSocketHandlers(registry)`: `registry.addHandler(handler(), "/ws/" + apiVersion + "/tarjetas")`.
   - `@Bean` del handler por entidad (tarjetas, películas, etc.).
3. **WebSocketHandler** (extiende `TextWebSocketHandler`, implementa `WebSocketSender`):
   - `Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>()`.
   - `afterConnectionEstablished`: `sessions.add(session)`.
   - `afterConnectionClosed`: `sessions.remove(session)`.
   - `sendMessage(String msg)`: recorrer sesiones y `session.sendMessage(new TextMessage(msg))`.
4. **En el Service** (tras save/update/delete):
   - Obtener handler (vía `WebSocketConfig.xxxHandler()` o inyección si está disponible).
   - Construir notificación (entidad, tipo CREATE/UPDATE/DELETE, timestamp) → JSON.
   - Llamar `handler.sendMessage(json)` (opcional en hilo para no bloquear).

### Flujo
```
Cliente REST: POST /api/v1/tarjetas → Controller → Service → Repository (guarda)
                                                    ↓
Service: onChange(CREATE, tarjeta) → JSON → handler.sendMessage(json)
                                                    ↓
Cliente WS (conectado a ws://.../ws/v1/tarjetas) recibe JSON → actualiza UI
```

### Recordar
- Ruta conexión cliente: `ws://localhost:PUERTO/ws/v1/tarjetas`.
- Un handler por “canal” (tarjetas, películas, entradas).
- No usar STOMP en este esquema; es TextWebSocketHandler + mensaje en texto (JSON).

---

## 4. Checklist final — examen práctico

### Estructura y capas
- [ ] API bajo `api/v1/...` (`api.version` en properties, `${api.version}` en @RequestMapping).
- [ ] Controller **nunca** usa Repository; solo Service.
- [ ] Respuestas en DTOs (ResponseDto), **nunca** entidad JPA.
- [ ] Service: `findById(id).orElseThrow(() -> new NotFoundException(id))`.

### HTTP y validación
- [ ] POST → 201 + body; DELETE → 204 sin body.
- [ ] POST/PUT: `@Valid @RequestBody Dto`.
- [ ] Manejo de `MethodArgumentNotValidException` → 400 (ProblemDetail + mapa errores).
- [ ] Recurso no encontrado → 404 (NotFoundException).

### JPA
- [ ] Entidad: `@NoArgsConstructor` (y `@AllArgsConstructor` si usas @Builder).
- [ ] Relaciones: `@ManyToOne` + `@JoinColumn` en el lado N; `@OneToMany(mappedBy="...")` en el lado 1.
- [ ] Evitar ciclos en JSON: `@JsonIgnoreProperties("campo")` o `@JsonIgnore` donde haga falta.
- [ ] Preferir **LAZY** en relaciones; no EAGER por defecto.

### Paginación (si la piden)
- [ ] GET lista: `Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending()/descending())`.
- [ ] Service: `repository.findAll(spec, pageable).map(mapper::toResponseDto)`.
- [ ] Respuesta: `PageResponse.of(page, sortBy, direction)`; opcional cabecera `Link` (PaginationLinksUtils).
- [ ] Filtros opcionales: **Specification** + `JpaSpecificationExecutor`.

### Seguridad JWT (si la piden)
- [ ] Login: POST signin → token en respuesta.
- [ ] Filtro JWT antes de UsernamePasswordAuthenticationFilter; rellenar SecurityContext si token válido.
- [ ] Endpoints sensibles: `@PreAuthorize("hasRole('ADMIN')")`; `@EnableMethodSecurity`.

### WebSockets (si los piden)
- [ ] @EnableWebSocket + WebSocketConfigurer; handler registrado en `/ws/v1/...`.
- [ ] Handler con Set de sesiones; sendMessage recorre y envía TextMessage.
- [ ] Servicio llama a sendMessage(JSON) tras create/update/delete.

### Tests (recomendado)
- [ ] Al menos: GET por id (200), GET por id inexistente (404), POST (201), DELETE (204).
- [ ] Mock del Service en el controller; `when(service.findById(1L)).thenReturn(dto)`; `verify(service, times(1)).findById(1L)`.
- [ ] Si hay @PreAuthorize: `@WithMockUser(roles = "ADMIN")` para el test que requiere rol.

### Errores típicos a no cometer
- [ ] No devolver entidad en la API (solo DTOs).
- [ ] No olvidar @Valid en body de POST/PUT.
- [ ] No usar repositorio en el controller.
- [ ] No olvidar 201 en POST ni 204 en DELETE.

---

*Repaso último día — DWES 2º DAW. Detalle en el resto de apuntes .md.*
