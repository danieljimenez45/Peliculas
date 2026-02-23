# 2. Creación de una API REST desde cero

## 2.1 Paso a paso resumido

1. Crear proyecto (Spring Initializr o IDE) con: Web, Data JPA, Validation, (H2 si quieres en memoria).
2. Configurar `application.properties` o `application.yml` (puerto, BD, versión API).
3. Crear la **entidad** JPA y el **repositorio** (interface que extiende `JpaRepository`).
4. Crear **DTOs** (Create, Update, Response) y **Mapper** (Entity ↔ DTO).
5. Crear **interfaz del servicio** y **implementación** (usa repositorio + mapper).
6. Crear **RestController** con los endpoints (GET, POST, PUT, PATCH, DELETE) que llaman al servicio.
7. Opcional: excepciones propias y manejo global o en controller (404, 400).

A continuación se detalla cada parte con ejemplos de ambos proyectos.

---

## 2.2 Configuración inicial y dependencias

### pom.xml (Maven) — Mínimo para API REST

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.x</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

- **spring-boot-starter-web**: REST y MVC.
- **spring-boot-starter-data-jpa**: JPA y repositorios.
- **spring-boot-starter-validation**: `@Valid` en DTOs.
- **h2**: BD en memoria (ideal para desarrollo y exámenes).

---

## 2.3 application.properties / application.yml

### Ejemplo DWES 25-26 (application.properties)

```properties
spring.application.name=DWES25-26
server.port=${PORT:3000}
api.version=${API_VERSION:v1}

# Base de datos H2 en memoria
spring.datasource.url=jdbc:h2:mem:tarjetasapirest
spring.datasource.username=sa
spring.h2.console.enabled=true

# JPA: crear esquema y cargar data.sql al arrancar
spring.jpa.defer-datasource-initialization=true
spring.jpa.hibernate.ddl-auto=create-drop
spring.sql.init.mode=always

# SQL legible en logs
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Logging
logging.level.es.carlosgs.dwes2526.tarjetas=DEBUG
```

### Ejemplo Películas (fragmento)

```properties
spring.application.name=peliculas
server.port=${PORT:3000}
api.version=${API_VERSION:v1}

spring.datasource.url=jdbc:h2:mem:peliculasdb
spring.datasource.username=sa
spring.h2.console.enabled=true

spring.jpa.defer-datasource-initialization=true
spring.jpa.hibernate.ddl-auto=create-drop
spring.sql.init.mode=always
spring.jpa.show-sql=true

# JWT (solo si usas seguridad)
jwt.secret=tuClaveSecreta
jwt.expiration=86400
```

### Uso de la versión de API en el controlador

En ambos proyectos la ruta base del API se construye con la propiedad:

```java
@RequestMapping("api/${api.version}/tarjetas")  // DWES 25-26
@RequestMapping("api/${api.version}/peliculas") // Películas
```

Así puedes cambiar `api.version` (por ejemplo a `v2`) sin tocar código.

---

## 2.4 Conexión con base de datos

- **H2 en memoria**: no hay que instalar nada; al arrancar Spring crea la BD.
- **Consola H2**: con `spring.h2.console.enabled=true` se accede a `http://localhost:3000/h2-console` (o el puerto que uses). JDBC URL suele ser `jdbc:h2:mem:nombrebd`.
- **Datos iniciales**: con `spring.sql.init.mode=always` y `spring.jpa.defer-datasource-initialization=true`, Spring ejecuta `data.sql` (en `src/main/resources`) después de crear las tablas. Útil para exámenes.

Ejemplo de `data.sql` (idea):

```sql
INSERT INTO TITULARES (id, nombre, created_at, updated_at, is_deleted) VALUES
(1, 'Juan Pérez', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false);
INSERT INTO TARJETAS (id, numero, cvc, fecha_caducidad, saldo, titular_id, ...) VALUES
(1, '1234-5678-9012-3456', '123', '2026-12-31', 100.0, 1, ...);
```

---

## 2.5 Estructura mínima de un endpoint REST

### Controller (ejemplo Tarjetas — DWES 25-26)

```java
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/${api.version}/tarjetas")
public class TarjetasRestController {
    private final TarjetasService tarjetasService;

    @GetMapping("/{id}")
    public ResponseEntity<TarjetaResponseDto> getById(@PathVariable Long id) {
        log.info("Buscando tarjeta por id={}", id);
        return ResponseEntity.ok(tarjetasService.findById(id));
    }

    @PostMapping()
    public ResponseEntity<TarjetaResponseDto> create(@Valid @RequestBody TarjetaCreateDto dto) {
        var saved = tarjetasService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TarjetaResponseDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody TarjetaUpdateDto dto) {
        return ResponseEntity.ok(tarjetasService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tarjetasService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
```

### Convenciones HTTP que debes conocer

| Método | Uso típico | Respuesta |
|--------|------------|-----------|
| GET    | Obtener uno o listado | 200 + body |
| POST   | Crear recurso         | 201 Created + body |
| PUT    | Reemplazar completo   | 200 + body |
| PATCH  | Actualización parcial | 200 + body |
| DELETE | Borrar                | 204 No Content (sin body) |

- **@Valid**: activa la validación sobre el DTO (jakarta.validation).
- **@RequestBody**: deserializa el JSON al DTO.
- **ResponseEntity**: permite fijar código HTTP y cabeceras.

---

## 2.6 Versión de API y documentación

- En los proyectos se usa `api.version` (v1) en la ruta. Para un examen suele bastar con algo como `api/v1/recursos`.
- En Películas se usa **SpringDoc OpenAPI** para Swagger; no es obligatorio para el examen, pero si lo piden, la dependencia es `springdoc-openapi-starter-webmvc-ui` y la UI queda en `/swagger-ui.html` o `/swagger-ui/index.html`.

Con esto tienes la base: propiedades, BD H2, y un CRUD REST con controller → service → repository + DTOs y mapper. En los siguientes temas se ven entidades JPA, paginación, WebSockets, JWT y tests.
