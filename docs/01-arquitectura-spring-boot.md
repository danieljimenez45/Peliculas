# 1. Arquitectura general de un proyecto Spring Boot profesional

## 1.1 Estructura de carpetas

En ambos proyectos (Tarjetas/Titulares y Películas/Entradas) se sigue una estructura por **módulos o dominios**, no por tipo técnico. Cada recurso (entidad) tiene su propio paquete con controller, service, repository, model, dto, mappers y excepciones.

### Ejemplo DWES 25-26 (Tarjetas)

```
src/main/java/es/carlosgs/dwes2526/
├── Dwes2526Application.java          # Clase principal @SpringBootApplication
├── config/                           # Configuración global
│   └── websockets/
│       ├── WebSocketConfig.java
│       ├── WebSocketHandler.java
│       └── WebSocketSender.java
├── tarjetas/
│   ├── controllers/   TarjetasRestController.java
│   ├── dto/           TarjetaCreateDto, TarjetaResponseDto, TarjetaUpdateDto
│   ├── exceptions/    TarjetaNotFoundException, TarjetaBadRequestException...
│   ├── mappers/       TarjetaMapper.java
│   ├── models/        Tarjeta.java (entidad JPA)
│   ├── repositories/ TarjetasRepository.java
│   ├── services/      TarjetasService.java (interface), TarjetasServiceImpl.java
│   └── validators/    CreditCardNumber, CreditCardNumberValidator
├── titulares/
│   ├── controllers/   TitularesRestController.java
│   ├── dto/           TitularRequestDto
│   ├── exceptions/    TitularNotFoundException, TitularConflictException
│   ├── mappers/       TitularesMapper.java
│   ├── models/        Titular.java
│   ├── repositories/ TitularesRepository.java
│   └── services/      TitularesService, TitularesServiceImpl
├── utils/
│   └── pagination/    PageResponse.java, PaginationLinksUtils.java
└── websockets/notifications/
    ├── dto/           TarjetaNotificationResponse.java
    ├── mappers/        TarjetaNotificationMapper.java
    └── models/        Notificacion.java
```

### Ejemplo PELICULAS REPO

```
src/main/java/es/danieljr/peliculas/
├── PeliculasApplication.java
├── config/
│   ├── auth/          SecurityConfig, JwtAuthenticationFilter, LoginSuccessHandler
│   └── websockets/    WebSocketConfig, WebSocketHandler, WebSocketSender
├── rest/              # API REST
│   ├── auth/          controllers, dto, services (JWT, Authentication), repositories
│   ├── peliculas/     controllers, dto, exceptions, mappers, models, repositories, services
│   ├── entradas/      (misma estructura)
│   └── users/         (misma estructura)
├── web/               # MVC con vistas (Pebble)
│   ├── controllers/   PeliculasController, AdminController, LoginController, etc.
│   └── services/      I18nService
├── utils/pagination/   PageResponse, PaginationLinksUtils
└── websockets/notifications/   DTOs y mappers de notificaciones
```

### Resumen de capas por paquete

| Capa | Responsabilidad | Ejemplo |
|------|-----------------|--------|
| **Controller** | Recibe HTTP, valida entrada, delega al servicio, devuelve respuesta | `TarjetasRestController`, `PeliculasRestController` |
| **Service** | Lógica de negocio, orquesta repositorios y mappers, notificaciones | `TarjetasServiceImpl`, `PeliculasServiceImpl` |
| **Repository** | Acceso a datos (JPA), consultas | `TarjetasRepository`, `PeliculasRepository` |
| **Model / Entity** | Objeto persistido en BD | `Tarjeta`, `Pelicula`, `Titular`, `Entrada` |
| **DTO** | Objetos de entrada/salida de la API (Create, Update, Response) | `TarjetaCreateDto`, `PeliculaResponseDto` |
| **Mapper** | Convierte entre Entity y DTO | `TarjetaMapper`, `PeliculaMapper` |
| **Config** | Configuración de Spring (WebSocket, Security, CORS) | `WebSocketConfig`, `SecurityConfig` |
| **Exceptions** | Excepciones de negocio (404, 400, 409) | `TarjetaNotFoundException`, `PeliculaBadRequestException` |

---

## 1.2 Flujo completo de una petición HTTP

```
Cliente HTTP
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│  DispatcherServlet (Spring MVC)                                  │
│  - Enruta la petición al controlador según @RequestMapping      │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│  Filtros (opcionales)                                            │
│  - Ej: JwtAuthenticationFilter (Películas)                       │
│  - Validan token y rellenan SecurityContext                       │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│  @RestController / @Controller                                    │
│  - @GetMapping, @PostMapping, etc.                               │
│  - @Valid para validar el body (DTO)                              │
│  - Llama al Service y devuelve ResponseEntity o nombre de vista │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│  @Service                                                        │
│  - Lógica de negocio                                             │
│  - Usa Repository (findAll, findById, save, delete)               │
│  - Usa Mapper (Entity ↔ DTO)                                     │
│  - Puede enviar notificaciones WebSocket                         │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────┐
│  @Repository (JpaRepository)                                      │
│  - Acceso a BD vía JPA/Hibernate                                 │
│  - Métodos por nombre o @Query / Specification                   │
└─────────────────────────────────────────────────────────────────┘
    │
    ▼
  Base de datos
```

### Ejemplo concreto: GET /api/v1/tarjetas/1

1. **DispatcherServlet** recibe GET y envía a `TarjetasRestController.getById(1)`.
2. El **controller** llama a `tarjetasService.findById(1)`.
3. El **service** llama a `tarjetasRepository.findById(1)`, obtiene `Optional<Tarjeta>`, lanza `TarjetaNotFoundException` si está vacío, y usa `tarjetaMapper.toTarjetaResponseDto(tarjeta)`.
4. El **controller** devuelve `ResponseEntity.ok(dto)` → HTTP 200 + JSON.

Si en vez de REST fuera una vista (proyecto Películas): el controller devolvería `"peliculas/detalle"` y Spring resolvería la plantilla Pebble con el modelo.

---

## 1.3 Diagrama de dependencias entre capas

```
  Controller  ──►  Service  ──►  Repository
       │               │
       │               └──────►  Mapper  ──►  Entity / DTO
       │
       └────────────────────►  PaginationLinksUtils, etc.
```

- El **controller** no conoce entidades JPA ni repositorios; solo DTOs y servicios.
- El **service** conoce entidades, repositorios y mappers.
- El **repository** solo trabaja con entidades.

Esta separación facilita tests (puedes mockear el service en el controller) y cambios (por ejemplo, cambiar BD sin tocar la API).

---

## 1.4 Relación con los proyectos

- **DWES 25-26**: Solo API REST (tarjetas y titulares), WebSockets para notificaciones, sin seguridad ni vistas.
- **PELICULAS REPO**: API REST (películas, entradas, usuarios, auth) + seguridad JWT + vistas con Pebble (controladores en `web.controllers`). Misma idea de capas; la diferencia es el módulo `rest.auth` y los controladores web que devuelven nombres de plantillas.

En el examen, si piden “una API REST”, basta con controller + service + repository + model + DTOs + mapper, siguiendo esta arquitectura.
