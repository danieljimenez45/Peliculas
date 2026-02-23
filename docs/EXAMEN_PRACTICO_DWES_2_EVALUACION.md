# Examen 2ª Evaluación. Desarrollo Web Entorno Servidor

**Solo parte práctica — Ejercicios de implementación**

---

## Descripción general de la aplicación

Se desarrollará una aplicación de **gestión de reservas** (por ejemplo, de restaurante o eventos) con tres entidades principales y sus relaciones.

- **Usuario**: un usuario puede tener varias reservas; una reserva pertenece a un único usuario.
- **Reserva**: una reserva puede incluir varios servicios extra; un servicio extra puede estar en varias reservas (relación muchos a muchos entre Reserva y ServicioExtra).
- **ServicioExtra**: entidad catálogo (ej. menú degustación, parking, etc.) con lista de reservas asociadas.

**Entidad Usuario** (atributos):
- Identificador único
- Nombre
- Email
- Password
- Fecha de registro
- Rol (por ejemplo USER, ADMIN)
- Activo (boolean)

**Entidad Reserva** (atributos):
- Identificador único
- Fecha de la reserva
- Cantidad de personas
- Confirmado (estado)
- Usuario (relación)
- Lista de servicios extra u opciones (relación muchos a muchos con ServicioExtra)

**Entidad ServicioExtra** (atributos):
- Identificador único
- Nombre
- Precio
- Disponible (boolean)
- Lista de reservas asociadas (relación muchos a muchos con Reserva)

---

## Funcionalidades — Listado resumen

| # | Funcionalidad | Ruta / Descripción | Puntos |
|---|---------------|--------------------|--------|
| 1 | Definición del modelo | Entidades Usuario, Reserva, ServicioExtra | 1,5 |
| 2 | Carga inicial de datos | data.sql, H2 | 0,5 |
| 3 | Manejo de DTOs y mappers | DTOs de petición/respuesta y mappers | 1,5 |
| 4 | Gestión de excepciones | 404, 400, etc. | 1 |
| 5 | CRUD Usuarios (API REST) | `/api/v2/usuarios` | 2 |
| 6 | CRUD Reservas (API REST) | `/api/v2/reservas` | 2,5 |
| 7 | CRUD Servicios extra (API REST) | `/api/v2/servicios-extra` | 2 |
| 8 | Listado paginado con filtros y ordenación | Reservas: `GET /api/v2/reservas?page=0&size=10&sortBy=...` | 2 |
| 9 | Autenticación y autorización JWT | Login, protección de endpoints | 1,5 |
| 10 | WebSocket notificaciones | Notificación al crear/actualizar reserva o servicio | 1 |
| 11 | Vistas MVC con Pebble | Listados y detalle en HTML | 1,5 |
| 12 | Tests (repositorio, servicio, controlador) | JUnit, Mockito, MockMvcTester, AssertJ | 2 |

**Total: 19 puntos** (los decimales permiten ajustar el total a 20 si se redondea o se añade algún extra).

---

## Detalle de las funcionalidades

### 1. Definición del modelo (1,5 puntos)

- Definir las tres entidades (**Usuario**, **Reserva**, **ServicioExtra**) con sus anotaciones JPA.
- Las anotaciones de validación solo tendrán que ver con la comunicación con la base de datos y, donde corresponda, con la capa de servicio/controlador (p. ej. en DTOs).
- **Relaciones**:
  - Usuario 1:N Reserva (un usuario tiene muchas reservas).
  - Reserva N:M ServicioExtra (tabla intermedia; una reserva tiene varios servicios extra y un servicio extra puede estar en varias reservas).
- **Restricciones del modelo**:
  - Todos los campos obligatorios según el diseño (no nulos donde se indique).
  - Usar anotaciones de Lombok recomendadas para entidades (@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder, etc.).
  - La generación de identificadores la hará la base de datos.
  - Usuario: nombre y email obligatorios; password con longitud mínima; fecha de registro y actualización; rol como enum o equivalente; activo por defecto true.
  - Reserva: fecha de reserva obligatoria (posterior a “hoy” al dar de alta); cantidad de personas mínima 1; confirmado por defecto false; solo se podrán cambiar ciertos estados una vez dada de alta (por ejemplo confirmado/cancelado), no la fecha ni el usuario.
  - ServicioExtra: nombre y precio obligatorios; disponible por defecto true.
- Indicar nombres de tablas y columnas cuando sea necesario para claridad (nombres en español o inglés de forma coherente).

---

### 2. Carga inicial de datos (0,5 puntos)

- Configurar el proyecto para usar base de datos **H2** (en memoria o fichero, según se indique en clase).
- La carga de datos inicial se hará mediante un recurso **data.sql**.
- Cargar:
  - Al menos 3 usuarios (incluyendo al menos un ADMIN y uno USER).
  - Al menos 4 servicios extra.
  - Al menos 5 reservas; al menos 2 reservas del mismo usuario y al menos 2 reservas que incluyan más de un servicio extra.

---

### 3. Manejo de DTOs y mappers (1,5 puntos)

- Definir DTOs para la comunicación entre cliente y controladores REST (y entre controlador y servicio cuando se use DTO).
- Debe haber **al menos un DTO de petición y uno de respuesta** por cada entidad principal (Usuario, Reserva, ServicioExtra) que se exponga en la API.
- Los DTOs de petición deben incluir anotaciones de validación (Jakarta Validation) donde corresponda.
- Implementar una **clase mapper** (o un mapper por entidad) con métodos para convertir:
  - Entidad → DTO de respuesta
  - DTO de petición → entidad (para crear/actualizar)
- No exponer entidades JPA directamente en los endpoints.

---

### 4. Gestión de excepciones (1 punto)

- Contemplar **recurso no encontrado**: devolver HTTP **404** (Not Found) cuando se consulte por id un usuario, reserva o servicio extra que no exista.
- Contemplar **petición errónea**: devolver HTTP **400** (Bad Request) cuando los datos del body no sean válidos (validación de DTOs) o no cumplan las reglas de negocio (por ejemplo fecha de reserva en el pasado).
- Usar **ProblemDetail** (o equivalente) para respuestas de error con detalle (por ejemplo campos de validación).
- Las excepciones deben manejarse de forma centralizada (p. ej. @ControllerAdvice / @RestControllerAdvice) o en el controlador según se indique.

---

### 5. CRUD Usuarios — API REST (2 puntos)

- **Ruta base**: `GET/POST /api/v2/usuarios` y `GET/PUT/PATCH/DELETE /api/v2/usuarios/{id}`.
- **GET /api/v2/usuarios**: listar todos los usuarios; devolver lista de DTOs de respuesta (sin password).
- **GET /api/v2/usuarios/{id}**: devolver un usuario por id en DTO de respuesta; si no existe, 404.
- **POST /api/v2/usuarios**: crear usuario; body JSON con DTO de petición; validaciones; en éxito 201 y cuerpo con DTO de respuesta.
- **PUT /api/v2/usuarios/{id}**: actualizar usuario completo por id; 200 y DTO de respuesta; 404 si no existe; 400 si datos inválidos.
- **PATCH /api/v2/usuarios/{id}**: actualización parcial (por ejemplo activo, rol); mismo criterio que PUT.
- **DELETE /api/v2/usuarios/{id}**: borrar por id; 204 si existe; 404 si no existe.
- Implementar repositorio (JPA), servicio y controlador REST; usar DTOs y mappers en todas las respuestas/peticiones.

---

### 6. CRUD Reservas — API REST (2,5 puntos)

- **Ruta base**: `GET/POST /api/v2/reservas` y `GET/PUT/PATCH/DELETE /api/v2/reservas/{id}`.
- **GET /api/v2/reservas**: listar todas las reservas; devolver lista de DTOs de respuesta (incluyendo datos del usuario y lista de servicios extra según el diseño del DTO).
- **GET /api/v2/reservas/{id}**: devolver una reserva por id; 404 si no existe.
- **POST /api/v2/reservas**: crear reserva; body con DTO de petición (usuario id, fecha, cantidad de personas, lista de ids de servicios extra, etc.); validar fecha futura y cantidad ≥ 1; 201 y DTO de respuesta; 400 si no válido.
- **PUT /api/v2/reservas/{id}**: actualizar reserva (solo los campos permitidos según el modelo); 200 y DTO; 404/400 según corresponda.
- **PATCH /api/v2/reservas/{id}**: actualizar solo estado (por ejemplo confirmado); body con el campo a actualizar; 200 y DTO; 404 si no existe; 400 si datos no válidos.
- **DELETE /api/v2/reservas/{id}**: borrar por id; 204; 404 si no existe.
- Persistencia usando **consultas derivadas** o **JPQL** cuando sea necesario (por ejemplo filtros por usuario o por confirmado).

---

### 7. CRUD Servicios extra — API REST (2 puntos)

- **Ruta base**: `GET/POST /api/v2/servicios-extra` y `GET/PUT/PATCH/DELETE /api/v2/servicios-extra/{id}`.
- **GET /api/v2/servicios-extra**: listar todos; devolver lista de DTOs de respuesta.
- **GET /api/v2/servicios-extra/{id}**: uno por id; 404 si no existe.
- **POST /api/v2/servicios-extra**: crear; body con DTO de petición; 201 y DTO de respuesta; 400 si no válido.
- **PUT /api/v2/servicios-extra/{id}**: actualizar; 200 y DTO; 404/400 según corresponda.
- **PATCH /api/v2/servicios-extra/{id}**: actualización parcial (por ejemplo disponible, precio); 200 y DTO; 404/400 según corresponda.
- **DELETE /api/v2/servicios-extra/{id}**: borrar por id; 204; 404 si no existe.
- Implementar repositorio, servicio y controlador REST usando DTOs y mappers.

---

### 8. Listado paginado con filtros y ordenación (2 puntos)

- **GET /api/v2/reservas** (sobre el mismo endpoint del listado o una variante documentada):
  - Parámetros de paginación: `page` (default 0), `size` (default 10).
  - Parámetros de ordenación: `sortBy` (nombre del campo, p. ej. fechaReserva, id), `direction` (asc/desc).
  - Filtros opcionales: por ejemplo `usuarioId`, `confirmado` (true/false).
- La respuesta debe ser un **objeto de paginación** (p. ej. PageResponse) que incluya: lista de elementos (DTOs), total de páginas, total de elementos, página actual, tamaño, si es primera/última página, etc.
- Incluir cabecera **Link** con enlaces `next`, `prev`, `first`, `last` (usando una utilidad tipo PaginationLinksUtils).
- Implementar el repositorio con **Pageable** y, si se usan filtros, con **consultas derivadas** o **JPQL**.

---

### 9. Autenticación y autorización JWT (1,5 puntos)

- **(a) (0,75 puntos) Login y generación de token**
  - Endpoint **POST /api/v2/auth/login** (o similar) que reciba usuario/email y password.
  - Si las credenciales son correctas, devolver un **JWT** (en el body o en una cabecera, según diseño estándar del curso).
  - Si son incorrectas, devolver 401.
  - El proyecto debe estar configurado para validar JWT en las peticiones a la API (filtro o integración con Spring Security).

- **(b) (0,75 puntos) Protección de endpoints**
  - Proteger con **JWT** los endpoints de creación, actualización y borrado de reservas y de servicios extra (y opcionalmente los de usuarios).
  - Solo usuarios autenticados (y, si se indica, solo rol ADMIN para ciertos endpoints) puedan ejecutar POST/PUT/PATCH/DELETE.
  - Los listados GET pueden ser públicos o requerir autenticación según se indique en el enunciado de clase.
  - Devolver 401 si no hay token o es inválido; 403 si no tiene permiso.

---

### 10. WebSocket — Notificaciones (1 punto)

- Configurar un **WebSocket** (por ejemplo con Spring WebSocket) que permita suscribirse a un canal de notificaciones (p. ej. `/ws/reservas` o `/ws/notificaciones`).
- Cuando se **cree** o se **actualice** una reserva desde la API REST, enviar un mensaje por WebSocket con la información relevante (por ejemplo id de reserva, usuario, confirmado) para que los clientes conectados reciban la notificación.
- Opcionalmente, cuando se actualice un **ServicioExtra** (por ejemplo disponible o precio), enviar también una notificación por otro canal o por el mismo con un tipo de mensaje distinto.
- El mensaje enviado debe ser un JSON (DTO) coherente con el resto de la API.
- No es obligatorio implementar el cliente (front); solo el envío desde el servidor tras crear/actualizar reserva (y opcionalmente servicio extra).

---

### 11. Vistas MVC con Pebble (1,5 puntos)

- Configurar el proyecto para usar **Pebble** como motor de plantillas (dependencia y configuración).
- **(a) (0,75 puntos) Listado**
  - Ruta MVC (p. ej. `GET /reservas` o `GET /web/reservas`) que muestre un **listado de reservas** en HTML usando una plantilla Pebble.
  - La plantilla debe recibir la lista de reservas (o una página) y mostrar al menos: id, fecha, usuario, confirmado, enlaces o botones hacia el detalle.
  - Estilo y maquetación mínimos (tabla o cards) para que se entienda la estructura.

- **(b) (0,75 puntos) Detalle**
  - Ruta MVC (p. ej. `GET /reservas/{id}` o `GET /web/reservas/{id}`) que muestre el **detalle de una reserva** en HTML (plantilla Pebble): todos los campos relevantes, usuario, lista de servicios extra.
  - Si el id no existe, redirigir a listado o mostrar página de error según se indique.

- Los controladores MVC pueden usar los mismos servicios que la API REST y convertir entidades a DTOs o a un modelo simple para la vista.

---

### 12. Tests (2 puntos)

- **(a) (0,5 puntos) Tests de repositorio**
  - Con **JUnit** (y Spring Boot Test si se usa @DataJpaTest), probar al menos:
    - Un método del repositorio de reservas (por ejemplo búsqueda por usuario o por confirmado, o findAll con Pageable).
    - Un método del repositorio de servicios extra o de usuarios (por ejemplo findById o un filtro).
  - Cobertura: que existan datos y que se cumplan las condiciones de la consulta.

- **(b) (0,75 puntos) Tests de servicio**
  - Con **JUnit** y **Mockito**, probar al menos un método del servicio de reservas (por ejemplo crear reserva o buscar por id).
  - Casos: éxito; reserva no encontrada (id inexistente); datos inválidos si aplica.
  - Simular el repositorio y, si hay WebSocket u otros servicios, mockearlos.

- **(c) (0,75 puntos) Tests de controlador**
  - Con **MockMvcTester** y **AssertJ**, probar al menos un controlador REST (por ejemplo Reservas o Servicios extra):
    - GET por id: 200 con cuerpo correcto; 404 si no existe.
    - POST: 201 con cuerpo correcto; 400 si body inválido.
    - PATCH: 200 con actualización correcta; 404 si no existe; 400 si body inválido.
  - Simular la capa de servicio (MockitoBean del servicio); no usar base de datos real en estos tests si se indica así.

---

## Puntos clave que entran en el examen

- **Entidades**: atributos variados (id, fechas, booleanos, enums, relaciones 1:N y N:M).
- **Validaciones**: en entidades (cuando aplique) y en DTOs (Jakarta Validation).
- **Relaciones**: Usuario 1:N Reserva; Reserva N:M ServicioExtra (tabla intermedia).
- **API REST**: CRUD completo para Usuario, Reserva y ServicioExtra; rutas claras y métodos HTTP correctos.
- **Paginación, filtros y ordenación**: en listado de reservas; cabecera Link.
- **Seguridad**: JWT (login y protección de endpoints).
- **WebSocket**: notificación al crear/actualizar reserva (y opcionalmente servicio extra).
- **Vistas MVC**: Pebble para listado y detalle de reservas.
- **DTOs y mappers**: en todos los endpoints REST.
- **Excepciones**: 404 y 400 con ProblemDetail.
- **Tests**: repositorio (JUnit), servicio (JUnit + Mockito), controlador (MockMvcTester + AssertJ).

---

*Examen 2ª Evaluación. Desarrollo Web Entorno Servidor — Solo práctica.*
