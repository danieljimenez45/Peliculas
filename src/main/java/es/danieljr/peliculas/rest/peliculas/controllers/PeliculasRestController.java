package es.danieljr.peliculas.rest.peliculas.controllers;

import es.danieljr.peliculas.rest.peliculas.dto.PeliculaCreateDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaUpdateDto;
import es.danieljr.peliculas.rest.peliculas.exceptions.PeliculaNotFoundException;
import es.danieljr.peliculas.rest.peliculas.services.PeliculasService;
import es.danieljr.peliculas.utils.pagination.PageResponse;
import es.danieljr.peliculas.utils.pagination.PaginationLinksUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador de películas del tipo RestController.
 * Fijamos la ruta de acceso a este controlador.
 * Usamos el servicio de películas inyectado en el constructor
 * con @RequiredArgsConstructor.
 */
@Tag(name = "Peliculas", description = "Endpoint de Películas de nuestra API")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/${api.version}/peliculas")
public class PeliculasRestController {
    // Servicio de películas
    private final PeliculasService peliculasService;
    private final PaginationLinksUtils paginationLinksUtils;

    /**
     * Obtiene todas las películas
     *
     * @param titulo Título de la película
     * @param genero Género de la película
     * @return Lista paginada de películas
     */
    @Operation(summary = "Obtiene todas las películas", description = "Obtiene una lista paginada de películas")
    @Parameters({
            @Parameter(name = "titulo", description = "Título de la película (filtro)", example = ""),
            @Parameter(name = "genero", description = "Género de la película (filtro)", example = ""),
            @Parameter(name = "page", description = "Número de página", example = "0"),
            @Parameter(name = "size", description = "Tamaño de la página", example = "10"),
            @Parameter(name = "sortBy", description = "Campo de ordenación", example = "idPelicula"),
            @Parameter(name = "direction", description = "Dirección de ordenación", example = "asc")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de películas"),
    })
    @GetMapping()
    public ResponseEntity<PageResponse<PeliculaResponseDto>> getAll(
            @RequestParam(required = false) Optional<String> titulo,
            @RequestParam(required = false) Optional<String> genero,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idPelicula") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest request) {
        log.info("Buscando películas por titulo={}, genero={}", titulo, genero);
        // Creamos el objeto de ordenación
        Sort sort = direction.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        // Creamos cómo va a ser la paginación
        Pageable pageable = PageRequest.of(page, size, sort);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(request.getRequestURL().toString());
        Page<PeliculaResponseDto> pageResult = peliculasService.findAll(titulo, genero, pageable);
        return ResponseEntity.ok()
                .header("link", paginationLinksUtils.createLinkHeader(pageResult, uriBuilder))
                .body(PageResponse.of(pageResult, sortBy, direction));
    }

    /**
     * Obtiene una película por su id
     *
     * @param id de la película, se pasa como parámetro de la URL /{id}
     * @return PeliculaResponseDto si existe
     * @throws PeliculaNotFoundException si no existe la película (404)
     */
    @Operation(summary = "Obtiene una película por su id", description = "Obtiene una película por su id")
    @Parameters({
            @Parameter(name = "id", description = "Identificador de la película", example = "1", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Película"),
            @ApiResponse(responseCode = "404", description = "Película no encontrada"),
    })
    @GetMapping("/{id}")
    public ResponseEntity<PeliculaResponseDto> getById(@PathVariable Long id) {
        log.info("Buscando película por id={}", id);
        return ResponseEntity.ok(peliculasService.findById(id));
    }

    /**
     * Crear una película
     *
     * @param peliculaCreateDto a crear
     * @return PeliculaResponseDto creada
     */
    @Operation(summary = "Crea una película", description = "Crea una película")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Película a crear", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Película creada"),
            @ApiResponse(responseCode = "400", description = "Película no válida"),
    })
    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PeliculaResponseDto> create(@Valid @RequestBody PeliculaCreateDto peliculaCreateDto) {
        log.info("Creando película : {}", peliculaCreateDto);
        var saved = peliculasService.save(peliculaCreateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Actualiza una película
     *
     * @param id                de la película a actualizar
     * @param peliculaUpdateDto con los datos a actualizar
     * @return PeliculaResponseDto actualizada
     * @throws PeliculaNotFoundException si no existe la película (404)
     */
    @Operation(summary = "Actualiza una película", description = "Actualiza una película")
    @Parameters({
            @Parameter(name = "id", description = "Identificador de la película", example = "1", required = true)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Película a actualizar", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Película actualizada"),
            @ApiResponse(responseCode = "400", description = "Película no válida"),
            @ApiResponse(responseCode = "404", description = "Película no encontrada"),
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PeliculaResponseDto> update(@PathVariable Long id,
            @Valid @RequestBody PeliculaUpdateDto peliculaUpdateDto) {
        log.info("Actualizando película id={} con película={}", id, peliculaUpdateDto);
        return ResponseEntity.ok(peliculasService.update(id, peliculaUpdateDto));
    }

    /**
     * Actualiza parcialmente una película
     *
     * @param id                de la película a actualizar
     * @param peliculaUpdateDto con los datos a actualizar
     * @return Película actualizada
     * @throws PeliculaNotFoundException si no existe la película (404)
     */
    @Operation(summary = "Actualiza parcialmente una película", description = "Actualiza parcialmente una película")
    @Parameters({
            @Parameter(name = "id", description = "Identificador de la película", example = "1", required = true)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Película a actualizar", required = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Película actualizada"),
            @ApiResponse(responseCode = "400", description = "Película no válida"),
            @ApiResponse(responseCode = "404", description = "Película no encontrada"),
    })
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PeliculaResponseDto> updatePartial(@PathVariable Long id,
            @Valid @RequestBody PeliculaUpdateDto peliculaUpdateDto) {
        log.info("Actualizando parcialmente película con id={} con película={}", id, peliculaUpdateDto);
        return ResponseEntity.ok(peliculasService.update(id, peliculaUpdateDto));
    }

    /**
     * Borra una película por su id
     *
     * @param id de la película a borrar
     * @return ResponseEntity con status 204 No Content si se ha conseguido borrar
     * @throws PeliculaNotFoundException si no existe la película (404)
     */
    @Operation(summary = "Borra una película", description = "Borra una película")
    @Parameters({
            @Parameter(name = "id", description = "Identificador de la película", example = "1", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Película borrada"),
            @ApiResponse(responseCode = "404", description = "Película no encontrada"),
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Borrando película por id: {}", id);
        peliculasService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Manejador de excepciones de Validación: 400 Bad Request
     *
     * @param ex excepción
     * @return Mapa de errores de validación con el campo y el mensaje
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        BindingResult result = ex.getBindingResult();
        problemDetail.setDetail("Falló la validación para el objeto='" + result.getObjectName()
                + "'. " + "Núm. errores: " + result.getErrorCount());

        Map<String, String> errores = new HashMap<>();
        result.getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errores.put(fieldName, errorMessage);
        });

        problemDetail.setProperty("errores", errores);
        return problemDetail;
    }
}
