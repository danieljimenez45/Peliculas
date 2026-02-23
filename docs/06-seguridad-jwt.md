# 6. Seguridad con JWT

El proyecto **PELICULAS REPO** usa autenticación JWT para la API y además login por formulario para la parte web. Aquí nos centramos en el flujo JWT (login por API, token en cabecera).

---

## 6.1 Flujo completo de autenticación JWT

1. **Cliente** envía POST a `/api/v1/auth/signin` con `username` y `password` (JSON).
2. **AuthenticationRestController** recibe el body y llama a `AuthenticationService.signIn(request)`.
3. **AuthenticationService** usa `AuthenticationManager.authenticate(...)` con username y password; si falla, lanza y se devuelve 401.
4. Si es correcto, obtiene el **User** (UserDetails) del repositorio y llama a **JwtService.generateToken(user)**.
5. Se devuelve **200** con un JSON tipo `{ "token": "eyJ..." }`.
6. En peticiones siguientes, el **cliente** envía la cabecera:  
   `Authorization: Bearer eyJ...`
7. El **JwtAuthenticationFilter** (OncePerRequestFilter) intercepta la petición:
   - Lee la cabecera `Authorization`; si no hay "Bearer " sale del filtro sin hacer nada.
   - Extrae el token y con **JwtService** obtiene el username (subject).
   - Carga **UserDetails** con **AuthUsersService.loadUserByUsername(username)**.
   - Comprueba **JwtService.isTokenValid(token, userDetails)** (mismo usuario y no expirado).
   - Crea **UsernamePasswordAuthenticationToken** con userDetails y autoridades y lo pone en **SecurityContextHolder**.
8. El **controller** puede usar `@PreAuthorize("hasRole('ADMIN')")` y Spring comprobará el contexto de seguridad.

Diagrama:

```
[Cliente]  POST /api/v1/auth/signin { username, password }
                ↓
    AuthenticationRestController.signIn
                ↓
    AuthenticationService.signIn
                ↓
    AuthenticationManager.authenticate  →  (fallo → 401)
                ↓ (éxito)
    JwtService.generateToken(user)  →  token
                ↓
    Respuesta: { "token": "eyJ..." }

--- Peticiones posteriores ---

[Cliente]  GET /api/v1/peliculas  Header: Authorization: Bearer eyJ...
                ↓
    JwtAuthenticationFilter
        - Extrae token
        - JwtService.extractUserName(token)
        - AuthUsersService.loadUserByUsername(username)
        - JwtService.isTokenValid(token, userDetails)
        - SecurityContextHolder.setAuthentication(authToken)
                ↓
    Controller (opcional @PreAuthorize)  →  Service  →  Respuesta
```

---

## 6.2 Dependencias (Películas)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.5.0</version>
</dependency>
```

---

## 6.3 Configuración JWT en application.properties

```properties
jwt.secret=TuClaveSecretaMuyLargaParaFirmar
jwt.expiration=86400
```

- **jwt.secret**: clave para firmar y verificar el token (en producción usar variable de entorno).
- **jwt.expiration**: tiempo de vida del token en segundos (86400 = 24 horas).

---

## 6.4 JwtService (generar y validar token)

El servicio usa **Auth0 java-jwt** (JWT.create(), JWT.decode(), Algorithm.HMAC512). Resumen de métodos:

- **extractUserName(String token)**: devuelve el subject (username) del token.
- **generateToken(UserDetails userDetails)**: crea un JWT con subject = username, issuedAt, expiresAt y lo firma con HMAC512 en base64 de jwt.secret.
- **isTokenValid(String token, UserDetails userDetails)**: comprueba que el subject coincida con el username y que la fecha de expiración sea posterior a ahora.

Ejemplo de generación (idea, según JwtServiceImpl de Películas):

```java
Algorithm algorithm = Algorithm.HMAC512(getSigningKey());
Date now = new Date();
Date expirationDate = new Date(now.getTime() + (1000 * jwtExpiration));

return JWT.create()
    .withSubject(userDetails.getUsername())
    .withIssuedAt(now)
    .withExpiresAt(expirationDate)
    .sign(algorithm);
```

La clave de firma suele ser `Base64.getEncoder().encode(jwtSigningKey.getBytes())` para cumplir tamaño esperado por HMAC512.

---

## 6.5 JwtAuthenticationFilter

Extiende **OncePerRequestFilter** para que se ejecute una vez por petición:

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AuthUsersService authUsersService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !StringUtils.startsWithIgnoreCase(authHeader, "Bearer ")) {
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
            UserDetails userDetails = authUsersService.loadUserByUsername(userName);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                context.setAuthentication(authToken);
                SecurityContextHolder.setContext(context);
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

- Si no hay token o no es Bearer, se ignora y se sigue la cadena (la ruta puede ser pública).
- Si el token es inválido o el usuario no existe, se devuelve 401 y no se continúa.
- Si es válido, se rellena el **SecurityContext** para que el resto de la aplicación vea al usuario como autenticado.

---

## 6.6 SecurityConfig: cadena para la API (JWT)

En Películas hay varias **SecurityFilterChain** con `@Order`. La que aplica a la API (y a `/ws/**`, `/graphql`, etc.) es la de orden 1:

```java
@Bean
@Order(1)
public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    String[] apiPaths = { "/api/**", "/error/**", "/ws/**", "/graphql", "/graphiql", "/graphiql/**" };
    http
        .securityMatcher(apiPaths)
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(manager -> manager.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(request -> request
            .requestMatchers("/error/**").permitAll()
            .requestMatchers("/ws/**").permitAll()
            .requestMatchers("/api/" + apiVersion + "/**").permitAll()  // o .authenticated() si quieres exigir JWT
            .requestMatchers("/graphql", "/graphiql", "/graphiql/**").permitAll()
            .anyRequest().authenticated())
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

- **STATELESS**: no se usan sesiones HTTP; la “sesión” es el token en cada petición.
- **addFilterBefore(jwtAuthenticationFilter, ...)**: el filtro JWT se ejecuta antes del filtro de login por formulario, de modo que si viene un Bearer token se autentica antes de llegar al controller.

Si quieres que **solo** algunas rutas exijan autenticación, puedes poner:

- `.requestMatchers("/api/" + apiVersion + "/auth/signin", "/api/" + apiVersion + "/auth/signup").permitAll()`
- `.requestMatchers("/api/" + apiVersion + "/**").authenticated()`

y el resto igual. En el proyecto actual la API está en permitAll para simplificar; los endpoints sensibles se protegen con **@PreAuthorize**.

---

## 6.7 Roles y permisos (@PreAuthorize)

El **User** implementa **UserDetails** y en **getAuthorities()** devuelve roles como `ROLE_USER`, `ROLE_ADMIN` (por ejemplo desde un enum `Role` y una tabla user_roles).

En los controladores:

```java
@PostMapping()
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<PeliculaResponseDto> create(@Valid @RequestBody PeliculaCreateDto dto) {
    ...
}

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> delete(@PathVariable Long id) {
    ...
}
```

Para que `@PreAuthorize` funcione hay que tener en la configuración de seguridad (en Películas ya está):

```java
@EnableMethodSecurity(jsr250Enabled = true)
```

Así, aunque la API esté “permitAll” a nivel de URL, los métodos con `@PreAuthorize("hasRole('ADMIN')")` solo se ejecutan si el SecurityContext tiene un usuario con rol ADMIN (que en JWT viene de los authorities del UserDetails al validar el token).

---

## 6.8 Login (signin) y registro (signup) — endpoints

**AuthenticationRestController** (Películas):

```java
@RestController
@RequestMapping("api/${api.version}/auth")
public class AuthenticationRestController {
    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public ResponseEntity<JwtAuthResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        return ResponseEntity.ok(authenticationService.signUp(request));
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtAuthResponse> signIn(@Valid @RequestBody UserSignInRequest request) {
        return ResponseEntity.ok(authenticationService.signIn(request));
    }
}
```

**AuthenticationServiceImpl** (resumen):

- **signIn**: `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password))`; si OK, busca el usuario y devuelve `JwtAuthResponse.builder().token(jwtService.generateToken(user)).build()`.
- **signUp**: comprueba que password y repetición coincidan, codifica la contraseña con **PasswordEncoder**, crea el User (con rol USER), lo guarda y devuelve el token; si el username/email ya existe lanza excepción (409).

**PasswordEncoder** en SecurityConfig suele ser un bean **BCryptPasswordEncoder** y se usa en el **DaoAuthenticationProvider** para la API (y para el login por formulario en otra cadena).

---

## 6.9 Resumen para el examen

1. **Login**: POST a `/auth/signin` con username/password → si es correcto, respuesta con `{ "token": "..." }`.
2. **Uso del token**: cabecera `Authorization: Bearer <token>` en cada petición.
3. **Filtro JWT**: OncePerRequestFilter que lee el Bearer token, valida con JwtService, carga UserDetails y rellena SecurityContext.
4. **Seguridad por método**: `@PreAuthorize("hasRole('ADMIN')")` en los endpoints que solo deban usar admins.
5. **STATELESS**: sesión no guardada en servidor; toda la información de “quién es” está en el token y en el UserDetails cargado a partir del subject del token.

Con esto tienes el flujo completo de autenticación JWT tal como en PELICULAS REPO.
