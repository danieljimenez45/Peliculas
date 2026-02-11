package es.danieljr.peliculas.config.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@RequiredArgsConstructor
@Configuration
//@EnableWebSecurity // No hace falta en proyectos Spring Boot
// Habilitamos la seguridad a nivel de método
// ahora prePostEnabled está a true por defecto y @Secured se considera desfasado
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LoginSuccessHandler loginSuccessHandler;

    @Value("${api.version}")
    private String apiVersion;

    // Este filtro permite el acceso a la API REST
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        String[] apiPaths = { "/api/**", "/error/**", "/ws/**", "/graphql", "/graphiql", "/graphiql/**" };
        http
                .securityMatcher(apiPaths)
                // Podemos decir que forzamos el uso de HTTPS, para algunas rutas de la API o todas
                // Requerimos HTTPS para todas las peticiones, pero ojo que devuelve 302 para los test
                // .requiresChannel(channel -> channel.anyRequest().requiresSecure())

                // Deshabilitamos CSRF
                .csrf(AbstractHttpConfigurer::disable)
                // Activamos CORS
                .cors(Customizer.withDefaults()) // CORS con opciones por defecto
                // CORS opciones definidas en Bean: no hace falta especificar en SpringBoot
                // porque detecta el Bean automáticamente
                //.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Sesiones
                .sessionManagement(
                        manager -> manager.sessionCreationPolicy(STATELESS))
                // Lo primero es decir a qué URLs queremos dar acceso libre
                // Lista blanca de comprobación

                .authorizeHttpRequests(request -> request
                        .requestMatchers("/error/**").permitAll()
                        // Websockets para notificaciones
                        .requestMatchers("/ws/**").permitAll()
                        // Otras rutas de la API podemos permitirlas o no....
                        .requestMatchers("/api/" + apiVersion + "/**").permitAll()
                        // graphql
                        .requestMatchers("/graphql", "/graphiql", "/graphiql/**").permitAll()
                        // Podríamos jugar con permisos, por ejemplo para una ruta concreta
                        //.requestMatchers("/" + apiVersion + "/auth/me").hasRole("ADMIN")
                        // O con un acción HTTP, POST, PUT, DELETE, etc.
                        //.requestMatchers(GET, "/" + apiVersion + "/auth/me").hasRole("ADMIN")
                        // O con un patrón de ruta
                        //.regexMatchers("/" + apiVersion + "/auth/me").hasRole("ADMIN")
                        // El resto de peticiones tienen que estar autenticadas
                        .anyRequest().authenticated())

                // Añadimos el filtro de autenticación
                .authenticationProvider(authenticationProvider()).addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Devolvemos la configuración
        return http.build();
    }

    // Este filtro permite el acceso a las rutas web públicas
    @Bean
    @Order(2)
    public SecurityFilterChain webPublicFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/public/**", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**", "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    // Este filtro permite el acceso a la documentación OpenAPI
    @Bean
    @Order(3)
    public SecurityFilterChain openapiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/swagger-ui/**")
                .securityMatcher("/v3/api-docs/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll());
        return http.build();
    }

    // Este filtro permite el acceso a la consola de H2. Quitar en producción
    @Bean
    @Order(4)
    @Profile("dev")
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PathRequest.toH2Console())
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(PathRequest.toH2Console()).permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers(PathRequest.toH2Console()))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));
        return http.build();
    }

    // Este filtro permite el acceso a las rutas web públicas y maneja el login
    @Bean
    @Order(5)
    public SecurityFilterChain formLoginFilterChain(HttpSecurity http) throws Exception {
      http
        // Dejamos habilitado CSRF cuando tengamos los formularios con csrfToken
        //.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
          .requestMatchers("/public", "/public/", "/public/**").permitAll()  // ← AÑADIR SIN /**
          .requestMatchers("/", "/auth/**", "/webjars/**", "/css/**", "/images/**").permitAll()
          .requestMatchers("/admin/**").hasRole("ADMIN")
          .anyRequest().authenticated())
        .formLogin(form -> form
          .loginPage("/auth/login")
          .successHandler(loginSuccessHandler)  // para gestionar la cookie de visitas
          .loginProcessingUrl("/auth/login-post")
          .permitAll())
        .logout(logout -> logout
          .logoutUrl("/auth/logout")
          .logoutSuccessUrl("/public")  // ← SIN /index
          .permitAll());
      return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.applyPermitDefaultValues();
        configuration.setAllowedOrigins(List.of("http://mifrontend.es"));
        configuration.setAllowedMethods(List.of("GET", "POST", "DELETE", "PUT", "PATCH"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
