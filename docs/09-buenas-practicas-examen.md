# 9. Buenas prácticas generales para el examen

Resumen de organización, patrones y checklist para el examen práctico de DWES (Entorno Servidor).

---

## 9.1 Organización del proyecto

- **Paquetes por dominio**: no pongas todos los controllers en un paquete y todos los services en otro; agrupa por recurso (tarjetas, peliculas, usuarios). Ejemplo: `es.xxx.tarjetas.controllers`, `es.xxx.tarjetas.services`, `es.xxx.tarjetas.repositories`, etc.
- **Nombre de la API versionado**: `api/v1/recursos` (usar propiedad `api.version` en `application.properties` y `${api.version}` en @RequestMapping).
- **Capas claras**: Controller → Service → Repository; el controller no accede al repositorio ni conoce entidades JPA; solo DTOs y servicios.
- **Excepciones de negocio**: crear excepciones propias (NotFoundException, BadRequestException, ConflictException) y lanzarlas desde el servicio; en el controller o en un @ControllerAdvice traducirlas a códigos HTTP (404, 400, 409).

---

## 9.2 Estructura mínima de un recurso REST

Para un CRUD de una entidad (ej. “Producto”):

1. **Entity** (JPA): `Producto.java` en `models` o `entities`.
2. **Repository**: `ProductosRepository extends JpaRepository<Producto, Long>` (y si hay filtros dinámicos, `JpaSpecificationExecutor<Producto>`).
3. **DTOs**: `ProductoCreateDto`, `ProductoUpdateDto`, `ProductoResponseDto` (en `dto`).
4. **Mapper**: `ProductoMapper` (toEntity, toResponseDto, toEntity from update).
5. **Service**: interfaz `ProductosService` e implementación `ProductosServiceImpl` que use repository y mapper.
6. **Controller**: `ProductosRestController` con GET (lista y por id), POST, PUT/PATCH, DELETE y devolución de `ResponseEntity<...>`.

Si piden **paginación**: Pageable en el GET de lista, PageResponse en el body y opcionalmente cabecera Link (PaginationLinksUtils). Si piden **filtros opcionales**: Specification en el repositorio y en el servicio.

---

## 9.3 Convenciones HTTP que debes respetar

| Acción   | Método   | Ruta         | Respuesta éxito   |
|----------|----------|--------------|-------------------|
| Listar   | GET      | /recursos    | 200 + body        |
| Ver uno  | GET      | /recursos/{id} | 200 + body      |
| Crear    | POST     | /recursos    | 201 + body        |
| Actualizar | PUT    | /recursos/{id} | 200 + body      |
| Actualización parcial | PATCH | /recursos/{id} | 200 + body |
| Borrar   | DELETE   | /recursos/{id} | 204 sin body    |

Errores: 400 (validación o petición incorrecta), 404 (no encontrado), 409 (conflicto, ej. duplicado), 401 (no autenticado), 403 (no autorizado).

---

## 9.4 Validación

- En los DTOs de entrada: `@NotNull`, `@NotBlank`, `@Size`, `@Min`/`@Max`, `@Email`, `@Pattern`, `@Future` (fechas), etc.
- En el controller: `@Valid @RequestBody Dto dto` (y opcionalmente `BindingResult` si quieres devolver errores personalizados).
- Manejador de `MethodArgumentNotValidException`: devolver 400 con un cuerpo que indique los campos y mensajes de error (por ejemplo con ProblemDetail y un mapa "errores").

---

## 9.5 Errores comunes a evitar

1. **Controller que usa el repositorio**: la lógica y el acceso a datos deben estar en el servicio.
2. **Devolver la entidad JPA en la API**: usar siempre DTOs (ResponseDto) para no exponer estructura interna ni provocar lazy loading o ciclos en JSON.
3. **Olvidar @Valid** en el body de POST/PUT: la validación no se ejecutará.
4. **Paginación sin comprobar parámetros**: si usas `sortBy`, asegurarte de que el campo existe en la entidad (o sanitizar) para no lanzar error en BD.
5. **Relaciones EAGER en todas partes**: preferir LAZY y cargar lo necesario en el servicio (join/fetch) para evitar N+1.
6. **No manejar Optional en findById**: usar `repository.findById(id).orElseThrow(() -> new NotFoundException(id))` en el servicio.
7. **Tests que dependen del orden**: que los tests no dependan unos de otros; cada uno debe preparar su estado (mocks o datos con @Sql).

---

## 9.6 Patrones recomendados

- **Interfaz del servicio**: permite mockear fácilmente en tests y cambiar implementación.
- **Mapper dedicado**: una clase (o interfaz) que convierte Entity ↔ DTO; el controller y el servicio no hacen conversiones a mano.
- **PageResponse** (record o clase) para respuestas paginadas: content, totalPages, totalElements, pageNumber, pageSize, first, last, sortBy, direction.
- **Specification** para filtros opcionales combinables en lugar de muchos métodos en el repositorio.
- **Excepciones por tipo de error**: una excepción por situación (NotFoundException, BadRequestException) y un manejador global o por controlador que devuelva el código HTTP y mensaje adecuados.

---

## 9.7 Checklist mental antes de entregar

- [ ] ¿La API está bajo una ruta versionada (api/v1/...)?
- [ ] ¿Los métodos POST/PUT reciben DTOs con @Valid y devuelven DTOs (nunca entidades)?
- [ ] ¿Los códigos HTTP son correctos (201 para POST, 204 para DELETE, 404 cuando no existe)?
- [ ] ¿Hay manejo de validación (400) y de recurso no encontrado (404)?
- [ ] ¿El servicio usa el repositorio y el mapper y no hay lógica de acceso a datos en el controller?
- [ ] ¿Las entidades tienen @NoArgsConstructor (y si usas Lombok, @AllArgsConstructor para Builder)?
- [ ] ¿Las relaciones JPA tienen mappedBy/JoinColumn correctos y evitas ciclos en JSON (@JsonIgnoreProperties o @JsonIgnore)?
- [ ] Si hay paginación: ¿Pageable en el GET, PageResponse en el body y (si lo piden) cabecera Link?
- [ ] Si hay filtros: ¿Specification + JpaSpecificationExecutor o métodos por nombre bien definidos?
- [ ] Si hay seguridad: ¿Filtro JWT antes del controller y @PreAuthorize donde corresponda?
- [ ] ¿Al menos un par de tests de controller (por ejemplo GET por id y POST) para demostrar que sabes usar MockMvc y mocks?

---

## 9.8 Relación rápida con los proyectos

- **DWES 25-26**: Referencia para estructura de capas, paginación con Specification, PageResponse, PaginationLinksUtils, WebSockets con TextWebSocketHandler, DTOs y mappers, validación y manejo de excepciones en el controller.
- **PELICULAS REPO**: Misma base más seguridad JWT (filtro, JwtService, signin/signup), @PreAuthorize, frontend Pebble (layouts, fragmentos, macros, formularios con CSRF), GlobalControllerAdvice y tests con @WithMockUser.

Si sigues la estructura de estos proyectos y este checklist, tendrás una base sólida para el examen práctico.
