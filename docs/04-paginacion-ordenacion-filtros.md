# 4. Paginación, ordenación y criterios de selección

## 4.1 Pageable y PageRequest

Spring Data ofrece **Pageable** para paginación y **Sort** para ordenación. El repositorio puede recibir un `Pageable` y devolver un **Page\<T\>**.

### En el controlador (Tarjetas — DWES 25-26)

```java
@GetMapping()
public ResponseEntity<PageResponse<TarjetaResponseDto>> getAll(
    @RequestParam(required = false) Optional<String> numero,
    @RequestParam(required = false) Optional<String> titular,
    @RequestParam(required = false) Optional<Boolean> isDeleted,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "asc") String direction,
    HttpServletRequest request) {

    Sort sort = direction.equalsIgnoreCase(Sort.Direction.ASC.name())
        ? Sort.by(sortBy).ascending()
        : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(request.getRequestURL().toString());
    Page<TarjetaResponseDto> pageResult = tarjetasService.findAll(numero, titular, isDeleted, pageable);

    return ResponseEntity.ok()
        .header("link", paginationLinksUtils.createLinkHeader(pageResult, uriBuilder))
        .body(PageResponse.of(pageResult, sortBy, direction));
}
```

- **page**: número de página (0-based).
- **size**: tamaño de página.
- **sortBy**: campo por el que ordenar (debe existir en la entidad).
- **direction**: `asc` o `desc`.
- **PageRequest.of(page, size, sort)** construye el `Pageable`.

### En el servicio (Tarjetas)

```java
@Override
public Page<TarjetaResponseDto> findAll(Optional<String> numero, Optional<String> titular,
                                        Optional<Boolean> isDeleted, Pageable pageable) {
    Specification<Tarjeta> specNumero = ...;
    Specification<Tarjeta> specTitular = ...;
    Specification<Tarjeta> specIsDeleted = ...;
    Specification<Tarjeta> criterio = Specification.allOf(specNumero, specTitular, specIsDeleted);

    return tarjetasRepository.findAll(criterio, pageable)
        .map(tarjetaMapper::toTarjetaResponseDto);
}
```

El repositorio debe extender **JpaSpecificationExecutor\<Tarjeta\>** para usar `findAll(Specification, Pageable)`.

---

## 4.2 PageResponse (DTO de paginación)

Ambos proyectos usan un DTO para devolver la página en JSON de forma clara:

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

Así el cliente recibe algo como:

```json
{
  "content": [ ... ],
  "totalPages": 2,
  "totalElements": 15,
  "pageSize": 10,
  "pageNumber": 0,
  "totalPageElements": 10,
  "empty": false,
  "first": true,
  "last": false,
  "sortBy": "id",
  "direction": "asc"
}
```

---

## 4.3 Cabecera "Link" (paginación HATEOAS-style)

En ambos proyectos se genera una cabecera `Link` con URIs para primera, anterior, siguiente y última página:

```java
@Component
public class PaginationLinksUtils {
    public String createLinkHeader(Page<?> page, UriComponentsBuilder uriBuilder) {
        StringBuilder linkHeader = new StringBuilder();
        if (page.hasNext()) {
            String uri = constructUri(page.getNumber() + 1, page.getSize(), uriBuilder);
            linkHeader.append(buildLinkHeader(uri, "next"));
        }
        if (page.hasPrevious()) {
            String uri = constructUri(page.getNumber() - 1, page.getSize(), uriBuilder);
            appendCommaIfNecessary(linkHeader);
            linkHeader.append(buildLinkHeader(uri, "prev"));
        }
        if (!page.isFirst()) {
            String uri = constructUri(0, page.getSize(), uriBuilder);
            appendCommaIfNecessary(linkHeader);
            linkHeader.append(buildLinkHeader(uri, "first"));
        }
        if (!page.isLast()) {
            String uri = constructUri(page.getTotalPages() - 1, page.getSize(), uriBuilder);
            appendCommaIfNecessary(linkHeader);
            linkHeader.append(buildLinkHeader(uri, "last"));
        }
        return linkHeader.toString();
    }

    private String constructUri(int newPageNumber, int size, UriComponentsBuilder uriBuilder) {
        return uriBuilder
            .replaceQueryParam("page", newPageNumber)
            .replaceQueryParam("size", size)
            .build().encode().toUriString();
    }

    private String buildLinkHeader(String uri, String rel) {
        return "<" + uri + ">; rel=\"" + rel + "\"";
    }
}
```

Ejemplo de cabecera:  
`Link: <http://localhost:3000/api/v1/tarjetas?page=1&size=10>; rel="next", <...>; rel="last"`

---

## 4.4 Filtros dinámicos con Specification (JPA)

Cuando los filtros son opcionales (número de tarjeta, titular, isDeleted, título, género…), usar **Specification\<T\>** permite construir la cláusula WHERE dinámicamente.

### Repositorio

```java
@Repository
public interface TarjetasRepository extends JpaRepository<Tarjeta, Long>, JpaSpecificationExecutor<Tarjeta> {
    Optional<Tarjeta> findByUuid(UUID uuid);
    // ...
}
```

### Servicio (Tarjetas — filtro por número, titular e isDeleted)

```java
Specification<Tarjeta> specNumero = (root, query, cb) ->
    numero.map(n -> cb.like(cb.lower(root.get("numero")), "%" + n.toLowerCase() + "%"))
        .orElseGet(() -> cb.isTrue(cb.literal(true)));

Specification<Tarjeta> specTitular = (root, query, cb) ->
    titular.map(t -> {
        Join<Tarjeta, Titular> join = root.join("titular");
        return cb.like(cb.lower(join.get("nombre")), "%" + t.toLowerCase() + "%");
    }).orElseGet(() -> cb.isTrue(cb.literal(true)));

Specification<Tarjeta> specIsDeleted = (root, query, cb) ->
    isDeleted.map(d -> cb.equal(root.get("isDeleted"), d))
        .orElseGet(() -> cb.isTrue(cb.literal(true)));

Specification<Tarjeta> criterio = Specification.allOf(specNumero, specTitular, specIsDeleted);

return tarjetasRepository.findAll(criterio, pageable).map(tarjetaMapper::toTarjetaResponseDto);
```

- **root**: entidad principal (Tarjeta).
- **query**: CriteriaQuery (no suele usarse en casos simples).
- **cb**: CriteriaBuilder (like, equal, and, or, etc.).
- Si el parámetro está vacío, devolvemos “siempre true” para no filtrar por ese criterio.
- **root.join("titular")**: join con Titular para filtrar por nombre.

### Servicio Películas (título y género)

```java
Specification<Pelicula> specTitulo = (root, query, cb) -> titulo
    .map(t -> cb.like(cb.lower(root.get("titulo")), "%" + t.toLowerCase() + "%"))
    .orElseGet(() -> cb.isTrue(cb.literal(true)));

Specification<Pelicula> specGenero = (root, query, cb) -> genero
    .map(g -> cb.equal(cb.lower(root.get("genero")), g.toLowerCase()))
    .orElseGet(() -> cb.isTrue(cb.literal(true)));

Specification<Pelicula> criterio = Specification.allOf(specTitulo, specGenero);

return peliculasRepository.findAll(criterio, pageable).map(peliculaMapper::toPeliculaResponseDto);
```

---

## 4.5 Queries por nombre (sin Specification)

Si los filtros son fijos, puedes usar métodos del repositorio por nombre:

```java
Page<Pelicula> findByTitulo(String titulo, Pageable pageable);
Page<Pelicula> findByGenero(String genero, Pageable pageable);
Page<Pelicula> findByTituloAndGenero(String titulo, String genero, Pageable pageable);
```

Para filtros opcionales y combinables, Specification es más flexible.

---

## 4.6 Ordenación por varios campos

```java
Sort sort = Sort.by("apellidos").ascending().and(Sort.by("nombre").ascending());
Pageable pageable = PageRequest.of(page, size, sort);
```

---

## 4.7 Resumen para el examen

1. **Controlador**: parámetros `page`, `size`, `sortBy`, `direction` y opcionales de filtro; construir `Sort` y `PageRequest`; llamar al servicio y devolver `PageResponse` + cabecera `Link` si lo piden.
2. **Servicio**: construir `Specification` por cada filtro opcional, combinarlos con `Specification.allOf`, llamar a `repository.findAll(spec, pageable)` y mapear a DTO.
3. **Repositorio**: extender `JpaRepository<Entity, Id>` y `JpaSpecificationExecutor<Entity>`.
4. **PageResponse**: record o clase con content, totalPages, totalElements, etc., y un método estático `of(Page, sortBy, direction)`.

Con esto tienes paginación, ordenación y filtros dinámicos tal como en los proyectos del profesor y Películas.
