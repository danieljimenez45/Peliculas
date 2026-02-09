package es.danieljr.peliculas.rest.entradas.controllers;

import es.danieljr.peliculas.rest.entradas.dto.EntradaCreateDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaResponseDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaUpdateDto;
import es.danieljr.peliculas.rest.entradas.exceptions.EntradaNotFoundException;
import es.danieljr.peliculas.rest.entradas.services.EntradasService;
import es.danieljr.peliculas.utils.pagination.PageResponse;
import es.danieljr.peliculas.utils.pagination.PaginationLinksUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador de entradas del tipo RestController
 * Fijamos la ruta de acceso a este controlador
 * Usamos el servicio de entradas y lo inyectamos en el constructor con @RequiredArgsConstructor
 *
 * @RequiredArgsConstructor es una anotación Lombok que nos permite inyectar dependencias basadas
 * en las anotaciones @Controller, @Service, @Component, etc.
 * y que se encuentren en nuestro contenedor de Spring
 * con solo declarar las dependencias como final ya que el constructor lo genera Lombok
 */
@Slf4j
@RequiredArgsConstructor
@RestController // Es un controlador Rest
@RequestMapping("api/${api.version}/entradas") // Es la ruta del controlador
public class EntradasRestController {
    // Servicio de entradas
    private final EntradasService entradasService;
    private final PaginationLinksUtils paginationLinksUtils;

    /**
     * Obtiene todas las entradas
     *
     * @param peliculaId    ID de la película para filtrar
     * @param fechaDesde    Fecha desde para filtrar
     * @param fechaHasta    Fecha hasta para filtrar
     * @param precioMin     Precio mínimo para filtrar
     * @param precioMax     Precio máximo para filtrar
     * @param metodoPago    Método de pago para filtrar
     * @return Lista paginada de entradas
     */
    @GetMapping()
    public ResponseEntity<PageResponse<EntradaResponseDto>> getAll(
            @RequestParam(required = false) Optional<Long> peliculaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> fechaHasta,
            @RequestParam(required = false) Optional<Double> precioMin,
            @RequestParam(required = false) Optional<Double> precioMax,
            @RequestParam(required = false) Optional<String> metodoPago,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idEntrada") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest request) {
        log.info("Buscando entradas con peliculaId={}, fechaDesde={}, fechaHasta={}, precioMin={}, precioMax={}, metodoPago={}", 
            peliculaId, fechaDesde, fechaHasta, precioMin, precioMax, metodoPago);
        // Creamos el objeto de ordenación
        Sort sort = direction.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        // Creamos cómo va a ser la paginación
        Pageable pageable = PageRequest.of(page, size, sort);
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(request.getRequestURL().toString());
        Page<EntradaResponseDto> pageResult = entradasService.findAll(peliculaId, fechaDesde, fechaHasta, precioMin, precioMax, metodoPago, pageable);
        return ResponseEntity.ok()
                .header("link", paginationLinksUtils.createLinkHeader(pageResult, uriBuilder))
                .body(PageResponse.of(pageResult, sortBy, direction));
    }

    /// Obtiene una entrada por su id
    ///
    /// @param id de la entrada, se pasa como parámetro de la URL /{id}
    /// @return EntradaResponseDto si existe
    /// @throws EntradaNotFoundException si no existe la entrada (404)
    @GetMapping("/{id}")
    public ResponseEntity<EntradaResponseDto> getById(@PathVariable Long id) {
        log.info("Buscando entrada por id={}", id);
        return ResponseEntity.ok(entradasService.findById(id));
    }

    /**
     * Crear una entrada
     *
     * @param entradaCreateDto a crear
     * @return EntradaResponseDto creada
     */
    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')") // Solo los administradores pueden crear entradas
    public ResponseEntity<EntradaResponseDto> create(@Valid @RequestBody EntradaCreateDto entradaCreateDto) {
        log.info("Creando entrada : {}", entradaCreateDto);
        var saved = entradasService.save(entradaCreateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Actualiza una entrada
     *
     * @param id      de la entrada a actualizar
     * @param entradaUpdateDto con los datos a actualizar
     * @return EntradaResponseDto actualizada
     * @throws EntradaNotFoundException si no existe la entrada (404)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Solo los administradores pueden actualizar entradas
    public ResponseEntity<EntradaResponseDto> update(@PathVariable Long id, @Valid @RequestBody EntradaUpdateDto entradaUpdateDto) {
        log.info("Actualizando entrada id={} con entrada={}", id, entradaUpdateDto);
        return ResponseEntity.ok(entradasService.update(id, entradaUpdateDto));
    }

    /**
     * Actualiza parcialmente una entrada
     *
     * @param id      de la entrada a actualizar
     * @param entradaUpdateDto con los datos a actualizar
     * @return Entrada actualizada
     * @throws EntradaNotFoundException si no existe la entrada (404)
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Solo los administradores pueden actualizar entradas
    public ResponseEntity<EntradaResponseDto> updatePartial(@PathVariable Long id, @Valid @RequestBody EntradaUpdateDto entradaUpdateDto) {
        log.info("Actualizando parcialmente entrada con id={} con entrada={}", id, entradaUpdateDto);
        return ResponseEntity.ok(entradasService.update(id, entradaUpdateDto));
    }

    /**
     * Borra una entrada por su id
     *
     * @param id de la entrada a borrar
     * @return ResponseEntity con status 204 No Content si se ha conseguido borrar
     * @throws EntradaNotFoundException si no existe la entrada (404)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // Solo los administradores pueden borrar entradas
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Borrando entrada por id: {}", id);
        entradasService.deleteById(id);
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








