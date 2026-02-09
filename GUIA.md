# Guía Completa: Desarrollo de API REST Profesional con Spring Boot

## 📋 Tabla de Contenidos

1. [Introducción](#1-introducción)
2. [Análisis del Dominio](#2-análisis-del-dominio)
3. [Definición de Requerimientos](#3-definición-de-requerimientos)
4. [Diseño de la API REST](#4-diseño-de-la-api-rest)
5. [Arquitectura del Proyecto](#5-arquitectura-del-proyecto)
6. [Modelado de Datos con JPA](#6-modelado-de-datos-con-jpa)
7. [DTOs y Mapeo](#7-dtos-y-mapeo)
8. [Implementación Paso a Paso](#8-implementación-paso-a-paso)
9. [Persistencia](#9-persistencia)
10. [Manejo de Errores](#10-manejo-de-errores)
11. [Seguridad](#11-seguridad)
12. [Testing](#12-testing)
13. [Documentación](#13-documentación)
14. [Buenas Prácticas Profesionales](#14-buenas-prácticas-profesionales)
15. [Guía de Adaptación](#15-guía-de-adaptación)
16. [Conclusión](#16-conclusión)

---

## 1. Introducción

### 1.1 ¿Qué es una API REST?

Una **API REST (Representational State Transfer)** es un estilo arquitectónico que define un conjunto de restricciones para crear servicios web. Una API REST permite que diferentes aplicaciones se comuniquen entre sí mediante el protocolo HTTP, utilizando los métodos estándar (GET, POST, PUT, PATCH, DELETE) y devolviendo datos en formato JSON.

**Características principales:**
- **Stateless**: Cada petición contiene toda la información necesaria
- **Resource-based**: Los recursos se identifican mediante URLs
- **HTTP Methods**: Uso correcto de verbos HTTP (GET, POST, PUT, PATCH, DELETE)
- **JSON**: Formato estándar para intercambio de datos
- **Códigos de estado HTTP**: Uso apropiado de códigos de respuesta

### 1.2 ¿Qué significa una API profesional en Spring Boot?

Una API REST profesional en Spring Boot se caracteriza por:

1. **Arquitectura por capas**: Separación clara de responsabilidades (Controller, Service, Repository)
2. **Validación robusta**: Uso de Bean Validation para garantizar integridad de datos
3. **Manejo de excepciones centralizado**: Respuestas consistentes ante errores
4. **Seguridad implementada**: Autenticación y autorización con Spring Security y JWT
5. **Documentación completa**: Swagger/OpenAPI para facilitar el consumo
6. **Testing exhaustivo**: Tests unitarios e integración
7. **Paginación y filtrado**: Manejo eficiente de grandes volúmenes de datos
8. **Caché**: Optimización de rendimiento
9. **Logging apropiado**: Trazabilidad de operaciones
10. **Código limpio**: Principios SOLID y Clean Code

### 1.3 Objetivo del Documento

Este documento proporciona una guía **paso a paso** para desarrollar una API REST profesional con Spring Boot, basada en el análisis de proyectos reales. El contenido es:

- **Totalmente reutilizable**: Independiente del dominio de negocio
- **Adaptable**: Aplicable a cualquier conjunto de entidades
- **Basado en patrones reales**: Extraído de análisis de APIs profesionales
- **Didáctico**: Explica el *por qué* de cada decisión técnica

---

## 2. Análisis del Dominio

### 2.1 Identificación de Entidades

**Patrón observado**: En ambas APIs analizadas, las entidades representan conceptos del dominio de negocio que tienen persistencia en base de datos.

**Proceso de identificación:**

1. **Identificar conceptos principales**: ¿Qué "cosas" necesita gestionar tu sistema?
   - Ejemplo abstracto: `EntidadA`, `EntidadB`, `EntidadC`

2. **Determinar relaciones**:
   - **Uno a Muchos (OneToMany)**: Una `EntidadA` tiene muchas `EntidadB`
   - **Muchos a Uno (ManyToOne)**: Muchas `EntidadB` pertenecen a una `EntidadA`
   - **Uno a Uno (OneToOne)**: Una `EntidadA` tiene exactamente una `EntidadB`
   - **Muchos a Muchos (ManyToMany)**: Muchas `EntidadA` se relacionan con muchas `EntidadB`

3. **Ejemplo abstracto de relaciones**:
```
EntidadA (1) ────< (N) EntidadB
   │
   └─── (N) EntidadC
```

### 2.2 Definición de Atributos

**Patrón observado**: Cada entidad tiene:
- **Identificador único**: `id` (Long) con `@GeneratedValue`
- **Atributos de negocio**: Campos específicos del dominio
- **Auditoría**: `createdAt`, `updatedAt` (LocalDateTime)
- **Soft delete** (opcional): `isDeleted` (Boolean) para borrado lógico
- **UUID** (opcional): Identificador único alternativo para APIs públicas

**Ejemplo abstracto de atributos:**


// EntidadA
- id: Long (PK, autoincremental)
- nombre: String (obligatorio, max 100 caracteres)
- descripcion: String (opcional)
- activo: Boolean (por defecto true)
- createdAt: LocalDateTime (no modificable)
- updatedAt: LocalDateTime (actualizable)
- uuid: UUID (único, no modificable, opcional)


### 2.3 Relaciones entre Entidades

**Patrón observado**: Las relaciones JPA se definen con anotaciones específicas y configuraciones de cascada.

**Tipos de relaciones comunes:**

1. **@OneToMany**: Una entidad tiene muchas de otra
   - `mappedBy`: Nombre del campo en la entidad relacionada
   - `cascade`: Operaciones en cascada (ALL, PERSIST, MERGE, REMOVE)
   - `orphanRemoval`: Eliminar hijos huérfanos
   - `fetch`: Estrategia de carga (LAZY, EAGER)

2. **@ManyToOne**: Muchas entidades pertenecen a una
   - `@JoinColumn`: Nombre de la columna FK en BD
   - `fetch`: Generalmente LAZY

3. **@OneToOne**: Relación uno a uno
   - Similar a ManyToOne pero con unicidad

### 2.4 Ejemplo Abstracto Independiente del Dominio

**Entidad Principal (EntidadA):**
- Atributos básicos: `id`, `nombre`, `descripcion`, `estado`
- Relación con EntidadB: `@OneToMany List<EntidadB>`
- Auditoría: `createdAt`, `updatedAt`

**Entidad Relacionada (EntidadB):**
- Atributos básicos: `id`, `valor`, `fecha`
- Relación con EntidadA: `@ManyToOne EntidadA`
- Auditoría: `createdAt`, `updatedAt`

---

## 3. Definición de Requerimientos

### 3.1 Requerimientos Funcionales

**Patrón observado**: Las APIs profesionales implementan operaciones CRUD completas con funcionalidades adicionales.

**Checklist de requerimientos funcionales:**

- [ ] **CRUD completo**:
  - [ ] Crear entidad (POST)
  - [ ] Leer entidad por ID (GET /{id})
  - [ ] Leer todas las entidades con paginación (GET)
  - [ ] Actualizar entidad completa (PUT /{id})
  - [ ] Actualizar entidad parcial (PATCH /{id})
  - [ ] Eliminar entidad (DELETE /{id})

- [ ] **Filtrado y búsqueda**:
  - [ ] Búsqueda por atributos específicos
  - [ ] Búsqueda combinada (múltiples filtros)
  - [ ] Búsqueda case-insensitive

- [ ] **Paginación**:
  - [ ] Parámetros: `page`, `size`, `sortBy`, `direction`
  - [ ] Respuesta con metadatos: `totalElements`, `totalPages`, `content`
  - [ ] Headers de paginación (Link header)

- [ ] **Ordenación**:
  - [ ] Ordenación por cualquier campo
  - [ ] Ordenación ascendente/descendente

### 3.2 Requerimientos No Funcionales

**Patrón observado**: Las APIs profesionales priorizan seguridad, rendimiento, mantenibilidad y escalabilidad.

**Checklist de requerimientos no funcionales:**

- [ ] **Seguridad**:
  - [ ] Autenticación con JWT
  - [ ] Autorización por roles
  - [ ] Protección CSRF
  - [ ] CORS configurado

- [ ] **Rendimiento**:
  - [ ] Caché implementado
  - [ ] Consultas optimizadas
  - [ ] Paginación para grandes volúmenes

- [ ] **Mantenibilidad**:
  - [ ] Código limpio y documentado
  - [ ] Separación de responsabilidades
  - [ ] Tests unitarios e integración

- [ ] **Escalabilidad**:
  - [ ] Arquitectura por capas
  - [ ] Uso de interfaces
  - [ ] Inyección de dependencias

### 3.3 Casos de Uso

**Patrón observado**: Cada operación CRUD representa un caso de uso específico.

**Casos de uso genéricos:**

1. **CU-001**: Crear nueva entidad
   - Actor: Usuario autenticado con rol apropiado
   - Precondición: Datos válidos
   - Postcondición: Entidad creada y persistida

2. **CU-002**: Consultar entidad por ID
   - Actor: Usuario autenticado
   - Precondición: ID válido
   - Postcondición: Datos de la entidad devueltos

3. **CU-003**: Listar entidades con filtros
   - Actor: Usuario autenticado
   - Precondición: Parámetros de filtrado opcionales
   - Postcondición: Lista paginada de entidades

4. **CU-004**: Actualizar entidad
   - Actor: Usuario autenticado con permisos
   - Precondición: Entidad existe
   - Postcondición: Entidad actualizada

5. **CU-005**: Eliminar entidad
   - Actor: Usuario autenticado con permisos
   - Precondición: Entidad existe
   - Postcondición: Entidad eliminada

---

## 4. Diseño de la API REST

### 4.1 Convenciones REST en Spring Boot

**Patrón observado**: Ambas APIs siguen estrictamente las convenciones REST.

**Convenciones aplicadas:**

1. **URLs como recursos**:
   - Plural y en minúsculas: `/api/v1/entidades`
   - No verbos en URLs: ❌ `/api/v1/getEntidades`
   - Verbos en métodos HTTP: ✅ `GET /api/v1/entidades`

2. **Versionado**:
   - Prefijo `/api/v1/` o `/api/v2/`
   - Configurable mediante `application.properties`: `api.version=v1`

3. **Nombres consistentes**:
   - Controladores: `EntidadRestController`
   - Servicios: `EntidadService` / `EntidadServiceImpl`
   - Repositorios: `EntidadRepository`
   - DTOs: `EntidadCreateDto`, `EntidadUpdateDto`, `EntidadResponseDto`

### 4.2 Diseño de Endpoints

**Patrón observado**: Estructura estándar de endpoints REST.

**Endpoints genéricos:**

```
GET    /api/v1/entidades              → Listar todas (paginado)
GET    /api/v1/entidades/{id}          → Obtener por ID
POST   /api/v1/entidades               → Crear nueva
PUT    /api/v1/entidades/{id}          → Actualizar completa
PATCH  /api/v1/entidades/{id}          → Actualizar parcial
DELETE /api/v1/entidades/{id}         → Eliminar
```

**Parámetros de consulta comunes:**
- `page`: Número de página (default: 0)
- `size`: Tamaño de página (default: 10)
- `sortBy`: Campo de ordenación (default: "id")
- `direction`: Dirección (asc/desc, default: "asc")
- Filtros específicos: `?nombre=valor&estado=activo`

### 4.3 Uso Correcto de HTTP Verbs

**Patrón observado**: Uso semántico correcto de métodos HTTP.

| Método | Uso                         | Código de Respuesta                    |
|--------|-----------------------------|----------------------------------------|
| GET    | Consultar recursos          | 200 OK, 404 NOT FOUND                  |
| POST   | Crear nuevo recurso         | 201 CREATED, 400 BAD REQUEST           |
| PUT    | Actualizar recurso completo | 200 OK, 404 NOT FOUND, 400 BAD REQUEST |
| PATCH  | Actualizar recurso parcial  | 200 OK, 404 NOT FOUND, 400 BAD REQUEST |
| DELETE | Eliminar recurso            | 204 NO CONTENT, 404 NOT FOUND          |

### 4.4 Códigos de Estado HTTP

**Patrón observado**: Uso consistente y semántico de códigos HTTP.

**Códigos más utilizados:**

- **200 OK**: Operación exitosa (GET, PUT, PATCH)
- **201 CREATED**: Recurso creado exitosamente (POST)
- **204 NO CONTENT**: Recurso eliminado exitosamente (DELETE)
- **400 BAD REQUEST**: Error de validación o petición mal formada
- **401 UNAUTHORIZED**: No autenticado
- **403 FORBIDDEN**: Autenticado pero sin permisos
- **404 NOT FOUND**: Recurso no encontrado
- **409 CONFLICT**: Conflicto (ej: duplicado)
- **500 INTERNAL SERVER ERROR**: Error del servidor

### 4.5 Buenas Prácticas de Naming

**Patrón observado**: Nomenclatura consistente en todo el proyecto.

**Convenciones:**

- **Controladores**: `EntidadRestController` (sufijo `RestController`)
- **Servicios**: `EntidadService` (interfaz), `EntidadServiceImpl` (implementación)
- **Repositorios**: `EntidadRepository` (interfaz)
- **Entidades**: `Entidad` (singular, sin sufijo)
- **DTOs**: `EntidadCreateDto`, `EntidadUpdateDto`, `EntidadResponseDto`
- **Excepciones**: `EntidadNotFoundException`, `EntidadBadRequestException`
- **Mappers**: `EntidadMapper`

---

## 5. Arquitectura del Proyecto

### 5.1 Arquitectura por Capas Observada

**Patrón observado**: Ambas APIs implementan arquitectura en capas con separación clara de responsabilidades.

**Estructura de paquetes:**

```
com.tuempresa.tuproyecto/
├── config/
│   ├── auth/
│   │   ├── SecurityConfig.java
│   │   └── JwtAuthenticationFilter.java
│   ├── swagger/
│   │   └── SwaggerConfig.java
│   ├── i18n/
│   │   └── I18nConfig.java
│   └── websockets/
│       └── WebSocketConfig.java
├── rest/
│   └── entidades/
│       ├── controllers/
│       │   └── EntidadRestController.java
│       ├── services/
│       │   ├── EntidadService.java
│       │   └── EntidadServiceImpl.java
│       ├── repositories/
│       │   └── EntidadRepository.java
│       ├── models/
│       │   └── Entidad.java
│       ├── dto/
│       │   ├── EntidadCreateDto.java
│       │   ├── EntidadUpdateDto.java
│       │   └── EntidadResponseDto.java
│       ├── mappers/
│       │   └── EntidadMapper.java
│       └── exceptions/
│           ├── EntidadException.java
│           ├── EntidadNotFoundException.java
│           └── EntidadBadRequestException.java
├── utils/
│   └── pagination/
│       ├── PageResponse.java
│       └── PaginationLinksUtils.java
└── web/
    └── controllers/
        └── GlobalControllerAdvice.java
```

### 5.2 Responsabilidad de Cada Capa

**1. Capa de Controladores (Controllers)**
- **Responsabilidad**: Recibir peticiones HTTP, validar entrada, delegar a servicios, formatear respuestas
- **No debe**: Contener lógica de negocio, acceder directamente a repositorios
- **Anotaciones**: `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc.

**2. Capa de Servicios (Services)**
- **Responsabilidad**: Lógica de negocio, orquestación, transformaciones, validaciones complejas
- **No debe**: Acceder directamente a HTTP, conocer detalles de persistencia
- **Anotaciones**: `@Service`, `@Transactional` (opcional)

**3. Capa de Repositorios (Repositories)**
- **Responsabilidad**: Acceso a datos, consultas, persistencia
- **No debe**: Contener lógica de negocio
- **Anotaciones**: `@Repository` (opcional, Spring lo detecta automáticamente)

**4. Capa de Modelos (Models/Entities)**
- **Responsabilidad**: Representar estructura de datos en BD
- **Anotaciones**: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, relaciones JPA

**5. Capa de DTOs**
- **Responsabilidad**: Transferir datos entre capas, ocultar estructura interna
- **Separación**: DTOs de entrada (Create/Update) y salida (Response)

### 5.3 Flujo de una Request HTTP

**Patrón observado**: Flujo estándar de petición HTTP.

```
1. Cliente → HTTP Request
   ↓
2. SecurityFilterChain (JWT, CORS, CSRF)
   ↓
3. Controller (@RestController)
   - Valida entrada con @Valid
   - Extrae parámetros (@PathVariable, @RequestParam)
   ↓
4. Service (lógica de negocio)
   - Valida reglas de negocio
   - Usa Mapper para convertir DTO → Entity
   - Llama a Repository
   ↓
5. Repository (acceso a datos)
   - Ejecuta consulta JPA
   - Retorna Entity
   ↓
6. Service
   - Usa Mapper para convertir Entity → DTO
   - Aplica caché si corresponde
   ↓
7. Controller
   - Formatea respuesta HTTP
   - Código de estado apropiado
   ↓
8. Cliente ← HTTP Response (JSON)
```

### 5.4 Beneficios en Mantenibilidad y Escalabilidad

**Patrón observado**: La separación de capas facilita:

1. **Mantenibilidad**:
   - Cambios localizados (modificar servicio no afecta controller)
   - Fácil localización de bugs
   - Código más legible

2. **Testabilidad**:
   - Mock de dependencias fácil
   - Tests unitarios por capa
   - Tests de integración controlados

3. **Escalabilidad**:
   - Fácil añadir nuevas funcionalidades
   - Reutilización de servicios
   - Cambio de implementación sin afectar otras capas

4. **Trabajo en equipo**:
   - Diferentes desarrolladores en diferentes capas
   - Menos conflictos en Git
   - Especialización por capa

---

## 6. Modelado de Datos con JPA

### 6.1 Conversión de Entidades a Clases JPA

**Patrón observado**: Las entidades JPA siguen un patrón consistente.

**Estructura base de una entidad:**

```java
@Entity
@Table(name = "entidades")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Entidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Atributos de negocio
    @Column(nullable = false)
    private String nombre;
    
    // Relaciones
    @OneToMany(mappedBy = "entidad", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EntidadRelacionada> relacionadas = new ArrayList<>();
    
    // Auditoría
    @Column(name = "created_at", updatable = false, nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

### 6.2 Uso de Anotaciones JPA

**@Entity**: Indica que la clase es una entidad JPA
- Debe tener constructor sin argumentos
- Debe tener un campo `@Id`

**@Table**: Personaliza el nombre de la tabla
- `name`: Nombre de la tabla en BD

**@Id**: Marca el campo como clave primaria

**@GeneratedValue**: Estrategia de generación de ID
- `GenerationType.IDENTITY`: Auto-incremental (MySQL, PostgreSQL)
- `GenerationType.SEQUENCE`: Secuencia (Oracle)
- `GenerationType.AUTO`: Deja que JPA elija

**@Column**: Personaliza la columna
- `nullable`: Si puede ser NULL
- `length`: Longitud máxima (String)
- `name`: Nombre de la columna en BD
- `unique`: Si debe ser único

### 6.3 Relaciones JPA

**@OneToMany**: Una entidad tiene muchas de otra

```java
@OneToMany(mappedBy = "entidad", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
@Builder.Default
private List<EntidadRelacionada> relacionadas = new ArrayList<>();
```

- `mappedBy`: Campo en la entidad relacionada que mantiene la FK
- `cascade`: Operaciones en cascada (ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH)
- `orphanRemoval`: Eliminar hijos cuando se elimina la relación
- `fetch`: LAZY (carga bajo demanda) o EAGER (carga inmediata)

**@ManyToOne**: Muchas entidades pertenecen a una

```java
@ManyToOne
@JoinColumn(name = "entidad_id")
private Entidad entidad;
```

- `@JoinColumn`: Nombre de la columna FK en BD
- Generalmente `fetch = FetchType.LAZY`

**@OneToOne**: Relación uno a uno

```java
@OneToOne
@JoinColumn(name = "entidad_relacionada_id")
private EntidadRelacionada relacionada;
```

### 6.4 Validaciones

**Patrón observado**: Validaciones a nivel de entidad usando Bean Validation.

**Anotaciones comunes:**

- `@NotNull`: No puede ser null
- `@NotBlank`: No puede ser null, vacío o solo espacios (String)
- `@NotEmpty`: No puede ser null o vacío (colecciones, String)
- `@Size(min=, max=)`: Tamaño de String o colección
- `@Min(value)`: Valor mínimo (números)
- `@Max(value)`: Valor máximo (números)
- `@Positive`: Debe ser positivo
- `@Email`: Formato de email válido
- `@Pattern(regexp=)`: Expresión regular
- `@Future`: Fecha futura
- `@Past`: Fecha pasada

**Ejemplo:**

```java
@NotBlank(message = "El nombre es obligatorio")
@Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
@Column(nullable = false, length = 100)
private String nombre;
```

### 6.5 Ejemplo Abstracto Completo

```java
@Entity
@Table(name = "entidades_a")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EntidadA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(length = 500)
    private String descripcion;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
    
    @OneToMany(mappedBy = "entidadA", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EntidadB> entidadesB = new ArrayList<>();
    
    @Column(name = "created_at", updatable = false, nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

---

## 7. DTOs y Mapeo

### 7.1 ¿Por qué usar DTOs?

**Patrón observado**: Separación estricta entre entidades de dominio y DTOs de API.

**Razones:**

1. **Seguridad**: No exponer estructura interna de la BD
2. **Flexibilidad**: Cambiar entidad sin afectar API
3. **Optimización**: Enviar solo datos necesarios
4. **Validación**: Validaciones específicas por operación
5. **Versionado**: Diferentes DTOs para diferentes versiones de API

### 7.2 Separación Entidad / API

**Patrón observado**: Tres tipos de DTOs por entidad.

**Tipos de DTOs:**

1. **CreateDto**: Datos para crear nueva entidad
   - No incluye `id`, `createdAt`, `updatedAt`
   - Solo campos editables por el usuario

2. **UpdateDto**: Datos para actualizar entidad
   - Todos los campos opcionales (para PATCH)
   - No incluye `id`, relaciones complejas

3. **ResponseDto**: Datos devueltos al cliente
   - Incluye `id`, `createdAt`, `updatedAt`
   - Puede incluir datos calculados o relacionados

### 7.3 Estrategias de Mapeo

**Patrón observado**: Mapeo manual con clases Mapper dedicadas.

**Ventajas del mapeo manual:**
- Control total sobre la transformación
- Fácil de depurar
- Sin dependencias adicionales

**Alternativa: MapStruct**
- Generación automática de código
- Mejor rendimiento
- Menos código boilerplate

**Ejemplo de Mapper manual:**

```java
@Component
public class EntidadMapper {
    
    public Entidad toEntidad(EntidadCreateDto dto) {
        return Entidad.builder()
            .id(null)
            .nombre(dto.getNombre())
            .descripcion(dto.getDescripcion())
            .activo(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    public Entidad toEntidad(EntidadUpdateDto dto, Entidad entidad) {
        return Entidad.builder()
            .id(entidad.getId())
            .nombre(dto.getNombre() != null ? dto.getNombre() : entidad.getNombre())
            .descripcion(dto.getDescripcion() != null ? dto.getDescripcion() : entidad.getDescripcion())
            .activo(dto.getActivo() != null ? dto.getActivo() : entidad.getActivo())
            .createdAt(entidad.getCreatedAt())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    public EntidadResponseDto toResponseDto(Entidad entidad) {
        return EntidadResponseDto.builder()
            .id(entidad.getId())
            .nombre(entidad.getNombre())
            .descripcion(entidad.getDescripcion())
            .activo(entidad.getActivo())
            .createdAt(entidad.getCreatedAt())
            .updatedAt(entidad.getUpdatedAt())
            .build();
    }
}
```

### 7.4 Ejemplo Genérico Completo

**EntidadCreateDto:**
```java
@Builder
@Data
public class EntidadCreateDto {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 1, max = 100)
    private final String nombre;
    
    @Size(max = 500)
    private final String descripcion;
}
```

**EntidadUpdateDto:**
```java
@Builder
@Data
public class EntidadUpdateDto {
    @Size(min = 1, max = 100)
    private final String nombre;
    
    @Size(max = 500)
    private final String descripcion;
    
    private final Boolean activo;
}
```

**EntidadResponseDto:**
```java
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntidadResponseDto {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 8. Implementación Paso a Paso - GUÍA COMPLETA Y DETALLADA

Esta sección te guiará paso a paso para crear una API REST profesional desde cero. Sigue el orden indicado y copia el código exacto proporcionado.

---

## 📦 FASE 1: CREACIÓN DEL PROYECTO Y CONFIGURACIÓN INICIAL

### PASO 1.1: Crear el Proyecto con Spring Initializr

**🔗 URL:** https://start.spring.io/

**Configuración exacta:**

1. **Project:** Maven
2. **Language:** Java
3. **Spring Boot:** 3.5.9 (o la última versión estable)
4. **Project Metadata:**
   - **Group:** `com.tuempresa` (o tu dominio)
   - **Artifact:** `tu-api-rest` (nombre de tu proyecto)
   - **Name:** `TuApiRest`
   - **Package name:** `com.tuempresa.tuapirest`
   - **Packaging:** Jar
   - **Java:** 21 o superior (recomendado 21)

5. **Dependencias a seleccionar (OBLIGATORIAS):**
   - ✅ Spring Web
   - ✅ Spring Data JPA
   - ✅ Spring Security
   - ✅ Validation
   - ✅ H2 Database
   - ✅ Lombok
   - ✅ Spring Boot DevTools

6. **Dependencias adicionales (RECOMENDADAS):**
   - ✅ Springdoc OpenAPI (Swagger UI)
   - ✅ Spring Boot Actuator (monitoreo)

7. **Clic en "Generate"** y descarga el ZIP

8. **Extrae el ZIP** en tu directorio de trabajo

9. **Abre el proyecto** en tu IDE (IntelliJ IDEA, Eclipse, VS Code)

---

### PASO 1.2: Configurar el archivo `pom.xml`

**📄 Archivo:** `pom.xml` (raíz del proyecto)

**🔎 Propósito:** Define todas las dependencias del proyecto Maven.

**🧠 Explicación:** El `pom.xml` es el archivo de configuración de Maven que gestiona dependencias, versiones y plugins. Debe incluir todas las librerías necesarias para la API.

**💻 Código completo:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.9</version>
        <relativePath/>
    </parent>
    
    <groupId>com.tuempresa</groupId>
    <artifactId>tu-api-rest</artifactId>
    <version>1.0.0</version>
    <name>TuApiRest</name>
    <description>API REST Profesional con Spring Boot</description>
    
    <properties>
        <java.version>21</java.version>
        <springdoc.version>2.6.0</springdoc.version>
        <jwt.version>4.4.0</jwt.version>
    </properties>
    
    <dependencies>
        <!-- Spring Web - Para crear REST Controllers -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Data JPA - Para persistencia -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <!-- Spring Security - Para autenticación y autorización -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <!-- Validation - Para validar DTOs -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- H2 Database - Base de datos en memoria para desarrollo -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Lombok - Reduce código boilerplate -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Spring Boot DevTools - Recarga automática en desarrollo -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        
        <!-- Springdoc OpenAPI (Swagger) - Documentación automática -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        
        <!-- JWT (Auth0) - Para tokens JWT -->
        <dependency>
            <groupId>com.auth0</groupId>
            <artifactId>java-jwt</artifactId>
            <version>${jwt.version}</version>
        </dependency>
        
        <!-- Spring Boot Actuator - Monitoreo y métricas -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**⚠️ Errores comunes:**
- **Error:** "Dependency not found" → Verifica que las versiones sean correctas
- **Error:** "Lombok not working" → Asegúrate de tener el plugin de Lombok instalado en tu IDE
- **Solución:** Ejecuta `mvn clean install` en la terminal para descargar dependencias

---

### PASO 1.3: Configurar `application.properties`

**📄 Archivo:** `src/main/resources/application.properties`

**📍 Ruta:** `src/main/resources/application.properties`

**🔎 Propósito:** Configuración centralizada de la aplicación (BD, puerto, JWT, etc.)

**🧠 Explicación:** Este archivo contiene todas las propiedades de configuración. Spring Boot las lee automáticamente al iniciar.

**💻 Código completo:**

```properties
# ============================================
# CONFIGURACIÓN GENERAL DE LA APLICACIÓN
# ============================================
spring.application.name=tu-api-rest
application.title=Tu API REST Spring Boot
application.version=1.0.0
server.port=${PORT:3000}

# ============================================
# VERSIONADO DE API
# ============================================
api.version=${API_VERSION:v1}

# ============================================
# BASE DE DATOS H2 (DESARROLLO)
# ============================================
# URL de conexión a H2 (memoria)
spring.datasource.url=jdbc:h2:mem:tuapirest
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver

# Habilitar consola H2 (accesible en /h2-console)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.h2.console.settings.web-allow-others=false

# ============================================
# CONFIGURACIÓN JPA / HIBERNATE
# ============================================
# Estrategia de creación de tablas
# create-drop: Crea al inicio, elimina al final (solo desarrollo)
# update: Actualiza el esquema si hay cambios
# validate: Solo valida, no modifica
spring.jpa.hibernate.ddl-auto=create-drop

# Mostrar SQL en consola (útil para debugging)
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Dialecto de Hibernate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

# Inicializar datos desde data.sql
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
spring.sql.init.encoding=UTF-8

# ============================================
# JWT (JSON Web Tokens)
# ============================================
# Clave secreta para firmar tokens (¡CAMBIA ESTO EN PRODUCCIÓN!)
jwt.secret=TuClaveSecretaMuyLargaYSeguraParaFirmarTokensJWT_DeberiaSerAlMenos256Bits
# Tiempo de expiración en segundos (86400 = 24 horas)
jwt.expiration=86400

# ============================================
# SWAGGER / OPENAPI
# ============================================
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.operations-sorter=method
springdoc.swagger-ui.tags-sorter=alpha

# ============================================
# INTERNACIONALIZACIÓN (i18n)
# ============================================
spring.messages.basename=messages
spring.messages.encoding=UTF-8
spring.web.locale=es
spring.web.locale-resolver=fixed

# ============================================
# LOGGING
# ============================================
logging.level.root=INFO
logging.level.com.tuempresa.tuapirest=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n

# ============================================
# ACTUATOR (MONITOREO)
# ============================================
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
```

**⚠️ Errores comunes:**
- **Error:** "Port 3000 already in use" → Cambia `server.port=3001`
- **Error:** "H2 console not accessible" → Verifica que `spring.h2.console.enabled=true`
- **Error:** "JWT secret too short" → Usa una clave de al menos 256 bits

---

### PASO 1.4: Crear archivo de datos inicial `data.sql`

**📄 Archivo:** `src/main/resources/data.sql`

**📍 Ruta:** `src/main/resources/data.sql`

**🔎 Propósito:** Script SQL que se ejecuta al iniciar la aplicación para poblar la BD con datos de prueba.

**🧠 Explicación:** Spring Boot ejecuta este archivo automáticamente si `spring.sql.init.mode=always`. Útil para tener datos de prueba desde el inicio.

**💻 Código completo (ejemplo genérico):**

```sql
-- ============================================
-- DATOS INICIALES PARA DESARROLLO
-- ============================================

-- Insertar entidades de ejemplo
INSERT INTO entidades (nombre, descripcion, activo, created_at, updated_at) VALUES
('Entidad Ejemplo 1', 'Descripción de la entidad 1', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Entidad Ejemplo 2', 'Descripción de la entidad 2', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Entidad Ejemplo 3', 'Descripción de la entidad 3', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insertar usuarios de ejemplo (si tienes tabla de usuarios)
-- INSERT INTO usuarios (username, password, email, enabled) VALUES
-- ('admin', '$2a$10$...', 'admin@example.com', true);
```

**⚠️ Errores comunes:**
- **Error:** "Table not found" → Asegúrate de que la tabla existe (JPA la crea automáticamente)
- **Error:** "Data not inserted" → Verifica que `spring.sql.init.mode=always`
- **Solución:** Los nombres de tabla deben coincidir con `@Table(name = "...")` en la entidad

---

## 🏗️ FASE 2: CREAR ESTRUCTURA DE PAQUETES Y CLASES BASE

### PASO 2.1: Crear estructura de paquetes

**📁 Estructura de carpetas a crear:**

```
src/main/java/com/tuempresa/tuapirest/
├── TuApiRestApplication.java (ya existe, clase principal)
├── config/
│   ├── auth/
│   ├── swagger/
│   └── i18n/
├── rest/
│   └── entidades/
│       ├── controllers/
│       ├── services/
│       ├── repositories/
│       ├── models/
│       ├── dto/
│       ├── mappers/
│       └── exceptions/
├── utils/
│   └── pagination/
└── web/
    └── controllers/
```

**🔧 Cómo crear los paquetes:**

1. En IntelliJ: Click derecho en `com.tuempresa.tuapirest` → New → Package
2. Crea cada paquete uno por uno siguiendo la estructura anterior
3. En Eclipse: Click derecho → New → Package

---

### PASO 2.2: Crear clase principal (verificar que existe)

**📄 Archivo:** `TuApiRestApplication.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/TuApiRestApplication.java`

**🔎 Propósito:** Clase principal que inicia la aplicación Spring Boot.

**🧠 Explicación:** Esta clase contiene el método `main()` y la anotación `@SpringBootApplication` que activa la auto-configuración de Spring.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TuApiRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TuApiRestApplication.class, args);
    }
}
```

**✅ Verificación:** Esta clase debería existir automáticamente. Si no existe, créala.

---

## 📊 FASE 3: CREAR ENTIDADES JPA

### PASO 3.1: Crear la Entidad

**📄 Archivo:** `Entidad.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/models/Entidad.java`

**🔎 Propósito:** Representa una tabla en la base de datos. Mapea objetos Java a registros SQL.

**🧠 Explicación:** Las entidades JPA son clases Java anotadas con `@Entity` que representan tablas. JPA las convierte automáticamente en SQL.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entidades")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Entidad {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    @Column(nullable = false, length = 100, unique = false)
    private String nombre;
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    @Column(length = 500)
    private String descripcion;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
    
    // Relaciones (ejemplo: si tienes entidades relacionadas)
    // @OneToMany(mappedBy = "entidad", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    // @Builder.Default
    // private List<EntidadRelacionada> relacionadas = new ArrayList<>();
    
    // Auditoría: fecha de creación
    @Column(name = "created_at", updatable = false, nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Auditoría: fecha de actualización
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Método para actualizar la fecha de modificación
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

**⚠️ Errores comunes:**
- **Error:** "No identifier specified" → Asegúrate de tener un campo `@Id`
- **Error:** "Table 'entidades' already exists" → Cambia `ddl-auto` a `update` o elimina la BD
- **Error:** "Lombok annotations not working" → Instala el plugin de Lombok en tu IDE

**🧪 Cómo probar:**
1. Inicia la aplicación: `mvn spring-boot:run`
2. Abre el navegador: `http://localhost:3000/h2-console`
3. JDBC URL: `jdbc:h2:mem:tuapirest`
4. Usuario: `sa`, Password: (vacío)
5. Ejecuta: `SELECT * FROM entidades;`
6. Deberías ver la tabla creada

---

## 📦 FASE 4: CREAR REPOSITORIOS

### PASO 4.1: Crear el Repository

**📄 Archivo:** `EntidadRepository.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/repositories/EntidadRepository.java`

**🔎 Propósito:** Interfaz que extiende JpaRepository para operaciones CRUD y consultas personalizadas.

**🧠 Explicación:** Spring Data JPA implementa automáticamente esta interfaz. Solo defines la interfaz y Spring crea la implementación.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.repositories;

import com.tuempresa.tuapirest.rest.entidades.models.Entidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntidadRepository extends 
        JpaRepository<Entidad, Long>, 
        JpaSpecificationExecutor<Entidad> {
    
    // Queries derivadas (Spring las genera automáticamente)
    Optional<Entidad> findByNombre(String nombre);
    
    Optional<Entidad> findByNombreEqualsIgnoreCase(String nombre);
    
    // Puedes añadir más métodos aquí
    // Spring generará las queries automáticamente basándose en el nombre del método
}
```

**⚠️ Errores comunes:**
- **Error:** "Repository not found" → Asegúrate de que el paquete esté dentro del paquete raíz escaneado por `@SpringBootApplication`
- **Error:** "Method not supported" → Verifica la sintaxis del nombre del método (debe seguir convenciones de Spring Data)

**🧪 Cómo probar:**
1. Crea un test simple o usa la consola H2 para verificar que la tabla existe
2. La aplicación debería iniciar sin errores

---

## 📝 FASE 5: CREAR DTOs (Data Transfer Objects)

### PASO 5.1: Crear `EntidadCreateDto`

**📄 Archivo:** `EntidadCreateDto.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/dto/EntidadCreateDto.java`

**🔎 Propósito:** DTO para recibir datos al crear una nueva entidad. No incluye `id`, `createdAt`, `updatedAt`.

**🧠 Explicación:** Los DTOs separan la estructura interna (entidad) de la API externa. El cliente solo envía los campos editables.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EntidadCreateDto {
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    private final String nombre;
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private final String descripcion;
    
    // Nota: 'activo' no está aquí porque se establece por defecto en la entidad
    // Si quieres que el usuario pueda establecerlo, añádelo aquí
}
```

---

### PASO 5.2: Crear `EntidadUpdateDto`

**📄 Archivo:** `EntidadUpdateDto.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/dto/EntidadUpdateDto.java`

**🔎 Propósito:** DTO para actualizar una entidad existente. Todos los campos son opcionales (para PATCH).

**🧠 Explicación:** Para actualizaciones parciales (PATCH), todos los campos deben ser opcionales. El servicio decide qué campos actualizar.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EntidadUpdateDto {
    
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    private final String nombre;
    
    @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
    private final String descripcion;
    
    private final Boolean activo;
    
    // Nota: Todos los campos son opcionales para permitir actualizaciones parciales
}
```

---

### PASO 5.3: Crear `EntidadResponseDto`

**📄 Archivo:** `EntidadResponseDto.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/dto/EntidadResponseDto.java`

**🔎 Propósito:** DTO que se devuelve al cliente. Incluye `id`, `createdAt`, `updatedAt` y todos los datos de la entidad.

**🧠 Explicación:** Este DTO representa la respuesta completa que el cliente recibe. Incluye metadatos como fechas de creación.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntidadResponseDto {
    
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Puedes añadir campos calculados o relacionados aquí
    // Ejemplo: private Integer totalRelacionadas;
}
```

**⚠️ Errores comunes:**
- **Error:** "Cannot deserialize" → Asegúrate de tener `@NoArgsConstructor` y `@AllArgsConstructor`
- **Error:** "Validation not working" → Verifica que tengas `@Valid` en el controller

---

## 🔄 FASE 6: CREAR MAPPER

### PASO 6.1: Crear `EntidadMapper`

**📄 Archivo:** `EntidadMapper.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/mappers/EntidadMapper.java`

**🔎 Propósito:** Convierte entre Entidades y DTOs. Separa la lógica de transformación.

**🧠 Explicación:** El mapper centraliza todas las conversiones. Facilita el mantenimiento y evita código duplicado.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.mappers;

import com.tuempresa.tuapirest.rest.entidades.dto.EntidadCreateDto;
import com.tuempresa.tuapirest.rest.entidades.dto.EntidadResponseDto;
import com.tuempresa.tuapirest.rest.entidades.dto.EntidadUpdateDto;
import com.tuempresa.tuapirest.rest.entidades.models.Entidad;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EntidadMapper {
    
    /**
     * Convierte EntidadCreateDto a Entidad
     * Se usa al crear una nueva entidad
     */
    public Entidad toEntidad(EntidadCreateDto dto) {
        return Entidad.builder()
                .id(null) // El ID se genera automáticamente
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .activo(true) // Valor por defecto
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * Convierte EntidadUpdateDto a Entidad (actualización parcial)
     * Solo actualiza los campos que no son null en el DTO
     */
    public Entidad toEntidad(EntidadUpdateDto dto, Entidad entidadExistente) {
        return Entidad.builder()
                .id(entidadExistente.getId()) // Mantener el ID original
                .nombre(dto.getNombre() != null ? dto.getNombre() : entidadExistente.getNombre())
                .descripcion(dto.getDescripcion() != null ? dto.getDescripcion() : entidadExistente.getDescripcion())
                .activo(dto.getActivo() != null ? dto.getActivo() : entidadExistente.getActivo())
                .createdAt(entidadExistente.getCreatedAt()) // No se modifica
                .updatedAt(LocalDateTime.now()) // Actualizar fecha de modificación
                .build();
    }
    
    /**
     * Convierte Entidad a EntidadResponseDto
     * Se usa para devolver datos al cliente
     */
    public EntidadResponseDto toResponseDto(Entidad entidad) {
        return EntidadResponseDto.builder()
                .id(entidad.getId())
                .nombre(entidad.getNombre())
                .descripcion(entidad.getDescripcion())
                .activo(entidad.getActivo())
                .createdAt(entidad.getCreatedAt())
                .updatedAt(entidad.getUpdatedAt())
                .build();
    }
}
```

**⚠️ Errores comunes:**
- **Error:** "Mapper not found" → Asegúrate de tener `@Component` para que Spring lo detecte
- **Error:** "NullPointerException" → Verifica que los campos no sean null antes de acceder

---

## 🎯 FASE 7: CREAR EXCEPCIONES PERSONALIZADAS

### PASO 7.1: Crear excepción base

**📄 Archivo:** `EntidadException.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/exceptions/EntidadException.java`

**🔎 Propósito:** Excepción base para todas las excepciones relacionadas con Entidad.

**🧠 Explicación:** Jerarquía de excepciones facilita el manejo centralizado.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.exceptions;

public class EntidadException extends RuntimeException {
    public EntidadException(String message) {
        super(message);
    }
    
    public EntidadException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

### PASO 7.2: Crear `EntidadNotFoundException`

**📄 Archivo:** `EntidadNotFoundException.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/exceptions/EntidadNotFoundException.java`

**🔎 Propósito:** Se lanza cuando no se encuentra una entidad por ID.

**🧠 Explicación:** Excepción específica permite manejar este caso de forma diferenciada (404).

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntidadNotFoundException extends EntidadException {
    public EntidadNotFoundException(Long id) {
        super("Entidad con id " + id + " no encontrada");
    }
}
```

---

### PASO 7.3: Crear `EntidadBadRequestException`

**📄 Archivo:** `EntidadBadRequestException.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/exceptions/EntidadBadRequestException.java`

**🔎 Propósito:** Se lanza cuando la petición es incorrecta (validación de negocio).

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EntidadBadRequestException extends EntidadException {
    public EntidadBadRequestException(String message) {
        super(message);
    }
}
```

---

## 🛠️ FASE 8: CREAR SERVICIOS

### PASO 8.1: Crear interfaz `EntidadService`

**📄 Archivo:** `EntidadService.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/services/EntidadService.java`

**🔎 Propósito:** Define el contrato (métodos) que debe implementar el servicio.

**🧠 Explicación:** Separar interfaz de implementación permite cambiar la lógica sin afectar a los controladores.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.services;

import com.tuempresa.tuapirest.rest.entidades.dto.EntidadCreateDto;
import com.tuempresa.tuapirest.rest.entidades.dto.EntidadResponseDto;
import com.tuempresa.tuapirest.rest.entidades.dto.EntidadUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface EntidadService {
    
    /**
     * Obtiene todas las entidades con paginación y filtros opcionales
     */
    Page<EntidadResponseDto> findAll(Optional<String> nombre, Pageable pageable);
    
    /**
     * Obtiene una entidad por su ID
     * @throw  si no existe
     */
    EntidadResponseDto findById(Long id);
    
    /**
     * Crea una nueva entidad
     */
    EntidadResponseDto save(EntidadCreateDto dto);
    
    /**
     * Actualiza una entidad existente (actualización parcial)
     * @throw EntidadNotFoundException si no existe
     */
    EntidadResponseDto update(Long id, EntidadUpdateDto dto);
    
    /**
     * Elimina una entidad por ID
     * @throw EntidadNotFoundException si no existe
     */
    void deleteById(Long id);
}
```

---

### PASO 8.2: Crear implementación `EntidadServiceImpl`

**📄 Archivo:** `EntidadServiceImpl.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/services/EntidadServiceImpl.java`

**🔎 Propósito:** Implementa la lógica de negocio y orquesta las operaciones con el repositorio.

**🧠 Explicación:** El servicio contiene la lógica de negocio, validaciones complejas y orquesta las llamadas al repositorio y mapper.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.services;

import com.tuempresa.tuapirest.rest.entidades.dto.EntidadCreateDto;
import com.tuempresa.tuapirest.rest.entidades.dto.EntidadResponseDto;
import com.tuempresa.tuapirest.rest.entidades.dto.EntidadUpdateDto;
import com.tuempresa.tuapirest.rest.entidades.exceptions.EntidadNotFoundException;
import com.tuempresa.tuapirest.rest.entidades.mappers.EntidadMapper;
import com.tuempresa.tuapirest.rest.entidades.models.Entidad;
import com.tuempresa.tuapirest.rest.entidades.repositories.EntidadRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntidadServiceImpl implements EntidadService {
    
    private final EntidadRepository repository;
    private final EntidadMapper mapper;
    
    @Override
    @Transactional(readOnly = true)
    public Page<EntidadResponseDto> findAll(Optional<String> nombre, Pageable pageable) {
        log.debug("Buscando entidades con filtros: nombre={}, pageable={}", nombre, pageable);
        
        // Crear Specification para filtros dinámicos
        Specification<Entidad> spec = (root, query, cb) -> {
            if (nombre.isPresent() && !nombre.get().isBlank()) {
                // Búsqueda case-insensitive y parcial
                return cb.like(
                    cb.lower(root.get("nombre")), 
                    "%" + nombre.get().toLowerCase() + "%"
                );
            }
            // Si no hay filtro, devolver todas
            return cb.isTrue(cb.literal(true));
        };
        
        return repository.findAll(spec, pageable)
                .map(mapper::toResponseDto);
    }
    
    @Override
    @Transactional(readOnly = true)
    public EntidadResponseDto findById(Long id) {
        log.debug("Buscando entidad con id: {}", id);
        
        Entidad entidad = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Entidad con id {} no encontrada", id);
                    return new EntidadNotFoundException(id);
                });
        
        return mapper.toResponseDto(entidad);
    }
    
    @Override
    @Transactional
    public EntidadResponseDto save(EntidadCreateDto dto) {
        log.debug("Creando nueva entidad: {}", dto);
        
        Entidad entidad = mapper.toEntidad(dto);
        Entidad saved = repository.save(entidad);
        
        log.info("Entidad creada con id: {}", saved.getId());
        return mapper.toResponseDto(saved);
    }
    
    @Override
    @Transactional
    public EntidadResponseDto update(Long id, EntidadUpdateDto dto) {
        log.debug("Actualizando entidad con id: {}, datos: {}", id, dto);
        
        Entidad existente = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Entidad con id {} no encontrada para actualizar", id);
                    return new EntidadNotFoundException(id);
                });
        
        Entidad actualizada = mapper.toEntidad(dto, existente);
        Entidad saved = repository.save(actualizada);
        
        log.info("Entidad con id {} actualizada", id);
        return mapper.toResponseDto(saved);
    }
    
    @Override
    @Transactional
    public void deleteById(Long id) {
        log.debug("Eliminando entidad con id: {}", id);
        
        if (!repository.existsById(id)) {
            log.warn("Intento de eliminar entidad con id {} que no existe", id);
            throw new EntidadNotFoundException(id);
        }
        
        repository.deleteById(id);
        log.info("Entidad con id {} eliminada", id);
    }
}
```

**⚠️ Errores comunes:**
- **Error:** "Transaction required" → Añade `@Transactional` a métodos que modifican datos
- **Error:** "LazyInitializationException" → Usa `@Transactional(readOnly = true)` en métodos de lectura

---

## 🌐 FASE 9: CREAR UTILIDADES DE PAGINACIÓN

### PASO 9.1: Crear `PageResponse`

**📄 Archivo:** `PageResponse.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/utils/pagination/PageResponse.java`

**🔎 Propósito:** Wrapper para respuestas paginadas que incluye metadatos.

**🧠 Explicación:** Facilita la respuesta paginada con información adicional (total, páginas, etc.)

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.utils.pagination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private String sortBy;
    private String direction;
    
    public static <T> PageResponse<T> of(Page<T> page, String sortBy, String direction) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sortBy(sortBy)
                .direction(direction)
                .build();
    }
}
```

---

### PASO 9.2: Crear `PaginationLinksUtils`

**📄 Archivo:** `PaginationLinksUtils.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/utils/pagination/PaginationLinksUtils.java`

**🔎 Propósito:** Genera headers HTTP `Link` para navegación de paginación (RFC 5988).

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.utils.pagination;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PaginationLinksUtils {
    
    public String createLinkHeader(Page<?> page, UriComponentsBuilder uriBuilder) {
        StringBuilder linkHeader = new StringBuilder();
        
        if (page.hasNext()) {
            String uri = uriBuilder
                    .replaceQueryParam("page", page.getNumber() + 1)
                    .replaceQueryParam("size", page.getSize())
                    .build()
                    .encode()
                    .toUriString();
            linkHeader.append("<").append(uri).append(">; rel=\"next\"");
        }
        
        if (page.hasPrevious()) {
            if (linkHeader.length() > 0) {
                linkHeader.append(", ");
            }
            String uri = uriBuilder
                    .replaceQueryParam("page", page.getNumber() - 1)
                    .replaceQueryParam("size", page.getSize())
                    .build()
                    .encode()
                    .toUriString();
            linkHeader.append("<").append(uri).append(">; rel=\"prev\"");
        }
        
        if (!page.isFirst()) {
            if (linkHeader.length() > 0) {
                linkHeader.append(", ");
            }
            String uri = uriBuilder
                    .replaceQueryParam("page", 0)
                    .replaceQueryParam("size", page.getSize())
                    .build()
                    .encode()
                    .toUriString();
            linkHeader.append("<").append(uri).append(">; rel=\"first\"");
        }
        
        if (!page.isLast()) {
            if (linkHeader.length() > 0) {
                linkHeader.append(", ");
            }
            String uri = uriBuilder
                    .replaceQueryParam("page", page.getTotalPages() - 1)
                    .replaceQueryParam("size", page.getSize())
                    .build()
                    .encode()
                    .toUriString();
            linkHeader.append("<").append(uri).append(">; rel=\"last\"");
        }
        
        return linkHeader.toString();
    }
}
```

---

## 🎮 FASE 10: CREAR CONTROLADOR REST

### PASO 10.1: Crear `EntidadRestController`

**📄 Archivo:** `EntidadRestController.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/rest/entidades/controllers/EntidadRestController.java`

**🔎 Propósito:** Recibe peticiones HTTP y delega a los servicios. Formatea respuestas JSON.

**🧠 Explicación:** El controlador es la capa más externa. Solo maneja HTTP, no contiene lógica de negocio.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.rest.entidades.controllers;

import com.tuempresa.tuapirest.rest.entidades.dto.EntidadCreateDto;
import com.tuempresa.tuapirest.rest.entidades.dto.EntidadResponseDto;
import com.tuempresa.tuapirest.rest.entidades.dto.EntidadUpdateDto;
import com.tuempresa.tuapirest.rest.entidades.services.EntidadService;
import com.tuempresa.tuapirest.utils.pagination.PageResponse;
import com.tuempresa.tuapirest.utils.pagination.PaginationLinksUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api/${api.version}/entidades")
public class EntidadRestController {
    
    private final EntidadService service;
    private final PaginationLinksUtils paginationLinksUtils;
    
    @Value("${api.version}")
    private String apiVersion;
    
    /**
     * GET /api/v1/entidades
     * Obtiene todas las entidades con paginación y filtros opcionales
     */
    @GetMapping
    public ResponseEntity<PageResponse<EntidadResponseDto>> getAll(
            @RequestParam(required = false) Optional<String> nombre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest request) {
        
        log.debug("GET /api/{}/entidades - page={}, size={}, sortBy={}, direction={}", 
                apiVersion, page, size, sortBy, direction);
        
        // Crear objeto Sort
        Sort sort = direction.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Llamar al servicio
        Page<EntidadResponseDto> pageResult = service.findAll(nombre, pageable);
        
        // Construir URI para los links de paginación
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(request.getRequestURL().toString())
                .queryParamIfPresent("nombre", nombre)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sortBy", sortBy)
                .queryParam("direction", direction);
        
        // Crear respuesta con headers de paginación
        return ResponseEntity.ok()
                .header("link", paginationLinksUtils.createLinkHeader(pageResult, uriBuilder))
                .body(PageResponse.of(pageResult, sortBy, direction));
    }
    
    /**
     * GET /api/v1/entidades/{id}
     * Obtiene una entidad por su ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntidadResponseDto> getById(@PathVariable Long id) {
        log.debug("GET /api/{}/entidades/{}", apiVersion, id);
        return ResponseEntity.ok(service.findById(id));
    }
    
    /**
     * POST /api/v1/entidades
     * Crea una nueva entidad
     */
    @PostMapping
    public ResponseEntity<EntidadResponseDto> create(
            @Valid @RequestBody EntidadCreateDto dto) {
        log.debug("POST /api/{}/entidades - Creando: {}", apiVersion, dto);
        
        EntidadResponseDto created = service.save(dto);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(created);
    }
    
    /**
     * PUT /api/v1/entidades/{id}
     * Actualiza una entidad completa
     */
    @PutMapping("/{id}")
    public ResponseEntity<EntidadResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody EntidadUpdateDto dto) {
        log.debug("PUT /api/{}/entidades/{} - Actualizando: {}", apiVersion, id, dto);
        return ResponseEntity.ok(service.update(id, dto));
    }
    
    /**
     * PATCH /api/v1/entidades/{id}
     * Actualiza una entidad parcialmente
     */
    @PatchMapping("/{id}")
    public ResponseEntity<EntidadResponseDto> updatePartial(
            @PathVariable Long id,
            @Valid @RequestBody EntidadUpdateDto dto) {
        log.debug("PATCH /api/{}/entidades/{} - Actualizando parcialmente: {}", 
                apiVersion, id, dto);
        return ResponseEntity.ok(service.update(id, dto));
    }
    
    /**
     * DELETE /api/v1/entidades/{id}
     * Elimina una entidad
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("DELETE /api/{}/entidades/{}", apiVersion, id);
        service.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
```

**⚠️ Errores comunes:**
- **Error:** "404 Not Found" → Verifica que `@RequestMapping` tenga la ruta correcta
- **Error:** "415 Unsupported Media Type" → Añade `Content-Type: application/json` en la petición
- **Error:** "400 Bad Request" → Verifica que el JSON sea válido y cumpla las validaciones

---

## 🛡️ FASE 11: CREAR MANEJO GLOBAL DE EXCEPCIONES

### PASO 11.1: Crear `GlobalExceptionHandler`

**📄 Archivo:** `GlobalExceptionHandler.java`

**📍 Ruta:** `src/main/java/com/tuempresa/tuapirest/web/controllers/GlobalExceptionHandler.java`

**🔎 Propósito:** Maneja todas las excepciones de forma centralizada y devuelve respuestas consistentes.

**🧠 Explicación:** `@RestControllerAdvice` intercepta todas las excepciones lanzadas por los controladores y las convierte en respuestas HTTP apropiadas.

**💻 Código completo:**

```java
package com.tuempresa.tuapirest.web.controllers;

import com.tuempresa.tuapirest.rest.entidades.exceptions.EntidadBadRequestException;
import com.tuempresa.tuapirest.rest.entidades.exceptions.EntidadNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Maneja excepciones de entidad no encontrada (404)
     */
    @ExceptionHandler(EntidadNotFoundException.class)
    public ProblemDetail handleEntidadNotFound(
            EntidadNotFoundException ex, 
            HttpServletRequest request) {
        
        log.warn("Entidad no encontrada: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Entidad no encontrada");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(request.getRequestURI());
        
        return problemDetail;
    }
    
    /**
     * Maneja excepciones de petición incorrecta (400)
     */
    @ExceptionHandler(EntidadBadRequestException.class)
    public ProblemDetail handleEntidadBadRequest(
            EntidadBadRequestException ex,
            HttpServletRequest request) {
        
        log.warn("Petición incorrecta: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Petición incorrecta");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(request.getRequestURI());
        
        return problemDetail;
    }
    
    /**
     * Maneja errores de validación (400)
     * Se activa cuando @Valid falla en un DTO
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        log.warn("Error de validación: {}", ex.getMessage());
        
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errores.put(fieldName, errorMessage);
        });
        
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Error de validación");
        problemDetail.setDetail("La petición contiene errores de validación. Revisa los campos indicados.");
        problemDetail.setProperty("errores", errores);
        problemDetail.setInstance(request.getRequestURI());
        
        return problemDetail;
    }
    
    /**
     * Maneja excepciones genéricas (500)
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Error inesperado: ", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setTitle("Error interno del servidor");
        problemDetail.setDetail("Ha ocurrido un error inesperado. Por favor, contacta con el administrador.");
        problemDetail.setInstance(request.getRequestURI());
        
        return problemDetail;
    }
}
```

**⚠️ Errores comunes:**
- **Error:** "Exception handler not working" → Verifica que `@RestControllerAdvice` esté en un paquete escaneado por Spring
- **Error:** "ProblemDetail not serializing" → Asegúrate de usar Spring Boot 3.x (ProblemDetail es de Spring 6+)

---

## 🧪 FASE 12: PROBAR LA API

### PASO 12.1: Iniciar la aplicación

**Comando:**
```bash
mvn spring-boot:run
```

**O desde el IDE:**
- IntelliJ: Click derecho en `TuApiRestApplication` → Run
- Eclipse: Click derecho → Run As → Spring Boot App

**✅ Verificación:**
- Deberías ver en la consola: "Started TuApiRestApplication"
- Sin errores de compilación

---

### PASO 12.2: Probar endpoints con cURL o Postman

**1. Crear una entidad (POST):**
```bash
curl -X POST http://localhost:3000/api/v1/entidades \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Mi Primera Entidad",
    "descripcion": "Esta es una descripción de prueba"
  }'
```

**Respuesta esperada (201 Created):**
```json
{
  "id": 1,
  "nombre": "Mi Primera Entidad",
  "descripcion": "Esta es una descripción de prueba",
  "activo": true,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

**2. Obtener todas las entidades (GET):**
```bash
curl http://localhost:3000/api/v1/entidades?page=0&size=10
```

**3. Obtener una entidad por ID (GET):**
```bash
curl http://localhost:3000/api/v1/entidades/1
```

**4. Actualizar una entidad (PUT):**
```bash
curl -X PUT http://localhost:3000/api/v1/entidades/1 \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Entidad Actualizada",
    "descripcion": "Nueva descripción",
    "activo": false
  }'
```

**5. Actualización parcial (PATCH):**
```bash
curl -X PATCH http://localhost:3000/api/v1/entidades/1 \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Solo cambio el nombre"
  }'
```

**6. Eliminar una entidad (DELETE):**
```bash
curl -X DELETE http://localhost:3000/api/v1/entidades/1
```

---

### PASO 12.3: Probar validaciones

**Petición con datos inválidos:**
```bash
curl -X POST http://localhost:3000/api/v1/entidades \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "",
    "descripcion": "Esta descripción es demasiado larga..." 
  }'
```

**Respuesta esperada (400 Bad Request):**
```json
{
  "type": "about:blank",
  "title": "Error de validación",
  "status": 400,
  "detail": "La petición contiene errores de validación...",
  "instance": "/api/v1/entidades",
  "errores": {
    "nombre": "El nombre es obligatorio"
  }
}
```

---

### PASO 12.4: Acceder a Swagger UI

**URL:** http://localhost:3000/swagger-ui.html

**Qué verás:**
- Interfaz gráfica para probar todos los endpoints
- Documentación automática de la API
- Posibilidad de ejecutar peticiones desde el navegador

---

## ✅ CHECKLIST FINAL

Marca cada elemento cuando lo completes:

- [ ] Proyecto creado con Spring Initializr
- [ ] `pom.xml` configurado con todas las dependencias
- [ ] `application.properties` configurado
- [ ] Estructura de paquetes creada
- [ ] Entidad JPA creada
- [ ] Repository creado
- [ ] DTOs creados (Create, Update, Response)
- [ ] Mapper creado
- [ ] Excepciones personalizadas creadas
- [ ] Interfaz Service creada
- [ ] Implementación Service creada
- [ ] Utilidades de paginación creadas
- [ ] Controller REST creado
- [ ] GlobalExceptionHandler creado
- [ ] Aplicación inicia sin errores
- [ ] Endpoints funcionan correctamente
- [ ] Validaciones funcionan
- [ ] Swagger UI accesible

---

## 🎯 PRÓXIMOS PASOS

Una vez completada esta fase básica, puedes continuar con:

1. **Seguridad (Sección 11):** Implementar JWT y Spring Security
2. **Testing (Sección 12):** Crear tests unitarios e integración
3. **Documentación (Sección 13):** Mejorar Swagger con anotaciones
4. **Optimizaciones:** Añadir caché, optimizar consultas, etc.

---

**⚠️ NOTA IMPORTANTE:** Esta guía te lleva hasta tener una API REST funcional básica. Las siguientes secciones (9-16) profundizan en persistencia, seguridad, testing, etc. Sigue el orden indicado y no avances hasta que cada paso funcione correctamente.

---

## 9. Persistencia

### 9.1 Uso de Spring Data JPA

**Patrón observado**: Repositorios que extienden `JpaRepository` y `JpaSpecificationExecutor`.

**Interfaz base:**
```java
@Repository
public interface EntidadRepository extends 
    JpaRepository<Entidad, Long>, 
    JpaSpecificationExecutor<Entidad> {
}
```

**Métodos heredados de JpaRepository:**
- `save(Entidad)`: Guardar o actualizar
- `findById(Long)`: Buscar por ID
- `findAll()`: Buscar todos
- `deleteById(Long)`: Eliminar por ID
- `existsById(Long)`: Verificar existencia

### 9.2 Queries Derivadas

**Patrón observado**: Nombres de métodos que generan queries automáticamente.

**Ejemplos:**
```java
// Buscar por nombre exacto
Optional<Entidad> findByNombre(String nombre);

// Buscar por nombre ignorando mayúsculas
Optional<Entidad> findByNombreEqualsIgnoreCase(String nombre);

// Buscar por nombre que contenga
List<Entidad> findByNombreContainingIgnoreCase(String nombre);

// Buscar con múltiples condiciones
List<Entidad> findByNombreAndActivo(String nombre, Boolean activo);

// Con paginación
Page<Entidad> findByNombre(String nombre, Pageable pageable);
```

### 9.3 Queries Personalizadas con @Query

**Patrón observado**: Queries JPQL o SQL nativas cuando es necesario.

**JPQL:**
```java
@Query("SELECT e FROM Entidad e WHERE e.nombre LIKE %:nombre%")
List<Entidad> buscarPorNombre(@Param("nombre") String nombre);
```

**SQL nativo:**
```java
@Query(value = "SELECT * FROM entidades WHERE nombre LIKE %:nombre%", nativeQuery = true)
List<Entidad> buscarPorNombreNativo(@Param("nombre") String nombre);
```

**Queries de actualización:**
```java
@Modifying
@Query("UPDATE Entidad e SET e.activo = false WHERE e.id = :id")
void desactivarPorId(@Param("id") Long id);
```

### 9.4 Specifications para Búsquedas Dinámicas

**Patrón observado**: Uso de `JpaSpecificationExecutor` para filtros dinámicos.

**Ejemplo:**
```java
@Override
public Page<EntidadResponseDto> findAll(Optional<String> nombre, Optional<Boolean> activo, Pageable pageable) {
    Specification<Entidad> specNombre = (root, query, cb) ->
        nombre.map(n -> cb.like(cb.lower(root.get("nombre")), "%" + n.toLowerCase() + "%"))
            .orElseGet(() -> cb.isTrue(cb.literal(true)));
    
    Specification<Entidad> specActivo = (root, query, cb) ->
        activo.map(a -> cb.equal(root.get("activo"), a))
            .orElseGet(() -> cb.isTrue(cb.literal(true)));
    
    Specification<Entidad> criterio = Specification.allOf(specNombre, specActivo);
    
    return repository.findAll(criterio, pageable)
        .map(mapper::toResponseDto);
}
```

### 9.5 Transacciones

**Patrón observado**: Transacciones automáticas en servicios.

**@Transactional:**
- Se aplica automáticamente en métodos `@Service`
- `readOnly = true` para operaciones de lectura
- Rollback automático en caso de excepción

**Ejemplo:**
```java
@Transactional
@Override
public EntidadResponseDto save(EntidadCreateDto dto) {
    // Operaciones que requieren transacción
    Entidad entidad = mapper.toEntidad(dto);
    return mapper.toResponseDto(repository.save(entidad));
}
```

### 9.6 Buenas Prácticas

1. **Usar LAZY loading** por defecto
2. **Evitar N+1 queries** con `@EntityGraph` o `JOIN FETCH`
3. **Paginación** para listas grandes
4. **Índices** en campos de búsqueda frecuente
5. **Validar existencia** antes de eliminar
6. **Soft delete** cuando sea necesario mantener historial

---

## 10. Manejo de Errores

### 10.1 Excepciones Personalizadas

**Patrón observado**: Jerarquía de excepciones específicas por dominio.

**Estructura:**
```java
// Excepción base
public class EntidadException extends RuntimeException {
    public EntidadException(String message) {
        super(message);
    }
}

// Excepciones específicas
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntidadNotFoundException extends EntidadException {
    public EntidadNotFoundException(Long id) {
        super("Entidad con id " + id + " no encontrada");
    }
}

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EntidadBadRequestException extends EntidadException {
    public EntidadBadRequestException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.CONFLICT)
public class EntidadConflictException extends EntidadException {
    public EntidadConflictException(String message) {
        super(message);
    }
}
```

### 10.2 @ControllerAdvice Global

**Patrón observado**: Manejo centralizado de excepciones.

**Implementación:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EntidadNotFoundException.class)
    public ProblemDetail handleEntidadNotFound(EntidadNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Entidad no encontrada");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }
    
    @ExceptionHandler(EntidadBadRequestException.class)
    public ProblemDetail handleEntidadBadRequest(EntidadBadRequestException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Petición incorrecta");
        problemDetail.setDetail(ex.getMessage());
        return problemDetail;
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        BindingResult result = ex.getBindingResult();
        
        Map<String, String> errores = new HashMap<>();
        result.getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errores.put(fieldName, errorMessage);
        });
        
        problemDetail.setDetail("Falló la validación. Núm. errores: " + result.getErrorCount());
        problemDetail.setProperty("errores", errores);
        return problemDetail;
    }
}
```

### 10.3 Respuestas Consistentes

**Patrón observado**: Uso de `ProblemDetail` (RFC 7807) para respuestas de error.

**Estructura estándar:**
```json
{
  "type": "about:blank",
  "title": "Entidad no encontrada",
  "status": 404,
  "detail": "Entidad con id 123 no encontrada",
  "instance": "/api/v1/entidades/123"
}
```

### 10.4 Patrones Observados

1. **Excepciones específicas** por tipo de error
2. **@ResponseStatus** en excepciones para código HTTP
3. **@ControllerAdvice** para manejo global
4. **ProblemDetail** para respuestas estándar
5. **Logging** de excepciones antes de lanzarlas
6. **Mensajes descriptivos** para el cliente

---

## 11. Seguridad

### 11.1 Autenticación vs Autorización

**Autenticación**: ¿Quién eres? (verificar identidad)
**Autorización**: ¿Qué puedes hacer? (verificar permisos)

### 11.2 Spring Security

**Patrón observado**: Configuración con múltiples `SecurityFilterChain` por orden de prioridad.

**Estructura básica:**
```java
@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${api.version}")
    private String apiVersion;
    
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**", "/error/**", "/ws/**", "/graphql", "/graphiql/**")
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(manager -> manager.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(request -> request
                .requestMatchers("/error/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/api/" + apiVersion + "/**").permitAll()
                .requestMatchers("/graphql", "/graphiql", "/graphiql/**").permitAll()
                .anyRequest().authenticated())
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
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
}
```

### 11.3 JWT (JSON Web Tokens)

**Patrón observado**: Autenticación stateless con JWT.

**Componentes necesarios:**

1. **JwtService**: Generar y validar tokens
2. **JwtAuthenticationFilter**: Filtrar peticiones y extraer token
3. **AuthenticationService**: Lógica de login

**Ejemplo de JwtService:**
```java
@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    public String generateToken(UserDetails userDetails) {
        return JWT.create()
            .withSubject(userDetails.getUsername())
            .withClaim("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
            .withExpiresAt(new Date(System.currentTimeMillis() + expiration * 1000))
            .sign(Algorithm.HMAC512(secret));
    }
    
    public String extractUsername(String token) {
        return JWT.require(Algorithm.HMAC512(secret))
            .build()
            .verify(token)
            .getSubject();
    }
}
```

### 11.4 Protección de Endpoints

**Patrón observado**: Uso de `@PreAuthorize` para control de acceso.

**Ejemplos:**
```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping
public ResponseEntity<EntidadResponseDto> create(@Valid @RequestBody EntidadCreateDto dto) {
    // Solo usuarios con rol ADMIN pueden crear
}

@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@GetMapping("/{id}")
public ResponseEntity<EntidadResponseDto> getById(@PathVariable Long id) {
    // ADMIN y USER pueden consultar
}
```

### 11.5 Patrones Reutilizables

1. **Múltiples SecurityFilterChain** con `@Order` para diferentes rutas
2. **JWT en header** `Authorization: Bearer <token>`
3. **CORS configurado** para frontend
4. **CSRF deshabilitado** para APIs stateless
5. **Sesiones stateless** (`STATELESS`)
6. **Roles y permisos** con `@PreAuthorize`

---

## 12. Testing

### 12.1 Tests Unitarios

**Patrón observado**: Tests unitarios para cada capa.

**Tests de Service:**
```java
@ExtendWith(MockitoExtension.class)
class EntidadServiceImplTest {
    @Mock
    private EntidadRepository repository;
    
    @Mock
    private EntidadMapper mapper;
    
    @InjectMocks
    private EntidadServiceImpl service;
    
    @Test
    void findById_shouldReturnEntidad_whenExists() {
        // Arrange
        Long id = 1L;
        Entidad entidad = Entidad.builder().id(id).nombre("Test").build();
        EntidadResponseDto dto = EntidadResponseDto.builder().id(id).nombre("Test").build();
        
        when(repository.findById(id)).thenReturn(Optional.of(entidad));
        when(mapper.toResponseDto(entidad)).thenReturn(dto);
        
        // Act
        EntidadResponseDto result = service.findById(id);
        
        // Assert
        assertThat(result).isEqualTo(dto);
        verify(repository).findById(id);
        verify(mapper).toResponseDto(entidad);
    }
    
    @Test
    void findById_shouldThrowException_whenNotExists() {
        // Arrange
        Long id = 999L;
        when(repository.findById(id)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> service.findById(id))
            .isInstanceOf(EntidadNotFoundException.class);
    }
}
```

### 12.2 Tests de Integración

**Patrón observado**: Tests de controladores con `@SpringBootTest` y `MockMvc`.

**Tests de Controller:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class EntidadRestControllerTest {
    @Autowired
    private MockMvcTester mockMvcTester;
    
    @MockitoBean
    private EntidadService service;
    
    private final String ENDPOINT = "/api/v1/entidades";
    
    @Test
    void getAll_shouldReturnPage_whenValidRequest() {
        // Arrange
        var page = new PageImpl<>(List.of(createResponseDto()));
        when(service.findAll(any(), any())).thenReturn(page);
        
        // Act
        var result = mockMvcTester.get()
            .uri(ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .exchange();
        
        // Assert
        assertThat(result)
            .hasStatusOk()
            .bodyJson()
            .hasPathSatisfying("$.content", content -> assertThat(content).isNotNull());
    }
    
    @Test
    void create_shouldReturnCreated_whenValidDto() {
        // Arrange
        String requestBody = """
            {
                "nombre": "Test"
            }
            """;
        when(service.save(any())).thenReturn(createResponseDto());
        
        // Act
        var result = mockMvcTester.post()
            .uri(ENDPOINT)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .exchange();
        
        // Assert
        assertThat(result).hasStatus(HttpStatus.CREATED);
    }
}
```

### 12.3 Uso de JUnit y Mockito

**Anotaciones comunes:**
- `@Test`: Método de test
- `@Mock`: Mock de dependencia
- `@InjectMocks`: Clase bajo test
- `@MockitoBean`: Mock en contexto Spring
- `@SpringBootTest`: Test de integración
- `@AutoConfigureMockMvc`: Configurar MockMvc

**Assertions:**
- `assertThat()`: AssertJ (fluent)
- `verify()`: Verificar llamadas a mocks
- `when().thenReturn()`: Configurar comportamiento de mocks

### 12.4 Qué Testear y Qué No

**Sí testear:**
- ✅ Lógica de negocio en servicios
- ✅ Transformaciones en mappers
- ✅ Endpoints de controladores
- ✅ Validaciones
- ✅ Manejo de excepciones
- ✅ Queries complejas

**No testear:**
- ❌ Framework (Spring, JPA)
- ❌ Getters/Setters simples
- ❌ Constructores sin lógica
- ❌ Configuraciones estándar

---

## 13. Documentación

### 13.1 Swagger / OpenAPI

**Patrón observado**: Documentación automática con Springdoc OpenAPI.

**Configuración:**
```java
@Configuration
class SwaggerConfig {
    @Value("${api.version}")
    private String apiVersion;
    
    @Bean
    OpenAPI apiInfo() {
        return new OpenAPI()
            .info(new Info()
                .title("Tu API REST Spring Boot")
                .version("1.0.0")
                .description("Documentación de la API")
                .contact(new Contact()
                    .name("Tu Nombre")
                    .email("tu@email.com")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .bearerFormat("JWT")
                        .scheme("bearer")));
    }
    
    @Bean
    GroupedOpenApi httpApi() {
        return GroupedOpenApi.builder()
            .group("http")
            .pathsToMatch("/api/" + apiVersion + "/**")
            .displayName("API Principal")
            .build();
    }
}
```

### 13.2 Documentación de Endpoints

**Patrón observado**: Anotaciones Swagger en controladores.

**Ejemplo:**
```java
@Tag(name = "Entidades", description = "Endpoint de Entidades")
@RestController
@RequestMapping("api/${api.version}/entidades")
public class EntidadRestController {
    
    @Operation(summary = "Obtiene todas las entidades", description = "Lista paginada de entidades")
    @Parameters({
        @Parameter(name = "nombre", description = "Filtro por nombre", example = "Test"),
        @Parameter(name = "page", description = "Número de página", example = "0"),
        @Parameter(name = "size", description = "Tamaño de página", example = "10")
    })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de entidades"),
        @ApiResponse(responseCode = "400", description = "Petición incorrecta")
    })
    @GetMapping
    public ResponseEntity<PageResponse<EntidadResponseDto>> getAll() {
        // ...
    }
}
```

### 13.3 Ejemplo Abstracto

**Acceso a documentación:**
- Swagger UI: `http://localhost:3000/swagger-ui.html`
- OpenAPI JSON: `http://localhost:3000/v3/api-docs`

---

## 14. Buenas Prácticas Profesionales

### 14.1 Clean Code

**Principios aplicados:**

1. **Nombres descriptivos**: Variables, métodos y clases con nombres claros
2. **Métodos pequeños**: Una responsabilidad por método
3. **Comentarios útiles**: Explicar el "por qué", no el "qué"
4. **Eliminar código muerto**: No dejar código comentado
5. **Formato consistente**: Usar formateador automático

### 14.2 SOLID Aplicado a Spring Boot

**Single Responsibility Principle (SRP):**
- Controller: Solo manejo HTTP
- Service: Solo lógica de negocio
- Repository: Solo acceso a datos

**Open/Closed Principle (OCP):**
- Interfaces para servicios (fácil extensión)
- Uso de abstracciones

**Liskov Substitution Principle (LSP):**
- Implementaciones intercambiables

**Interface Segregation Principle (ISP):**
- Interfaces específicas por responsabilidad

**Dependency Inversion Principle (DIP):**
- Depender de abstracciones (interfaces), no de implementaciones
- Inyección de dependencias con `@RequiredArgsConstructor`

### 14.3 Separación de Responsabilidades

**Cada capa tiene una responsabilidad única:**
- **Controller**: HTTP, validación entrada, formato salida
- **Service**: Lógica de negocio, orquestación
- **Repository**: Persistencia, consultas
- **Mapper**: Transformación de datos
- **DTO**: Transferencia de datos

### 14.4 Escalabilidad

**Patrones para escalar:**

1. **Caché**: Reducir carga en BD
2. **Paginación**: Manejar grandes volúmenes
3. **Lazy loading**: Cargar solo lo necesario
4. **Índices en BD**: Optimizar consultas
5. **Async processing**: Operaciones largas en background
6. **Microservicios**: Separar por dominio cuando crezca

---

## 15. Guía de Adaptación

### 15.1 Qué Partes Cambian

**Específico del dominio:**
- ✅ Nombres de entidades (EntidadA → TuEntidad)
- ✅ Atributos de entidades
- ✅ Relaciones entre entidades
- ✅ Validaciones específicas
- ✅ Lógica de negocio
- ✅ DTOs (campos específicos)

### 15.2 Qué Partes se Mantienen

**Infraestructura común:**
- ✅ Estructura de paquetes
- ✅ Configuración de seguridad
- ✅ Manejo de excepciones
- ✅ Paginación
- ✅ Swagger
- ✅ Testing (estructura)
- ✅ Mappers (patrón)

### 15.3 Checklist para Nuevos Proyectos

**Fase 1: Configuración inicial**
- [ ] Crear proyecto Spring Boot
- [ ] Configurar `application.properties`
- [ ] Configurar dependencias en `pom.xml`
- [ ] Configurar SecurityConfig
- [ ] Configurar SwaggerConfig

**Fase 2: Entidades y persistencia**
- [ ] Crear entidades JPA
- [ ] Definir relaciones
- [ ] Crear repositorios
- [ ] Configurar BD (H2 para desarrollo)

**Fase 3: DTOs y mappers**
- [ ] Crear CreateDto, UpdateDto, ResponseDto
- [ ] Implementar validaciones
- [ ] Crear mapper con métodos de transformación

**Fase 4: Servicios**
- [ ] Crear interfaz Service
- [ ] Implementar ServiceImpl
- [ ] Implementar lógica de negocio
- [ ] Añadir caché si es necesario

**Fase 5: Controladores**
- [ ] Crear RestController
- [ ] Implementar endpoints CRUD
- [ ] Añadir paginación y filtrado
- [ ] Documentar con Swagger

**Fase 6: Manejo de errores**
- [ ] Crear excepciones personalizadas
- [ ] Implementar GlobalExceptionHandler
- [ ] Probar respuestas de error

**Fase 7: Testing**
- [ ] Tests unitarios de servicios
- [ ] Tests unitarios de mappers
- [ ] Tests de integración de controladores
- [ ] Tests de repositorios

**Fase 8: Documentación y despliegue**
- [ ] Verificar Swagger
- [ ] Documentar endpoints
- [ ] Preparar para producción
- [ ] Configurar variables de entorno

### 15.4 Errores Comunes

1. **Exponer entidades directamente**: Siempre usar DTOs
2. **Lógica de negocio en controladores**: Mover a servicios
3. **Validaciones solo en frontend**: Validar también en backend
4. **No manejar excepciones**: Implementar GlobalExceptionHandler
5. **Queries N+1**: Usar `JOIN FETCH` o `@EntityGraph`
6. **Caché sin estrategia de invalidación**: Definir cuándo limpiar caché
7. **Passwords en texto plano**: Siempre usar `PasswordEncoder`
8. **CORS abierto a todos**: Configurar orígenes específicos
9. **Logs con información sensible**: No loguear passwords o tokens
10. **Sin tests**: Escribir tests desde el inicio

---

## 16. Conclusión

### 16.1 Resumen del Proceso Completo

Desarrollar una API REST profesional con Spring Boot requiere seguir un proceso estructurado:

1. **Análisis del dominio**: Identificar entidades y relaciones
2. **Diseño de API**: Definir endpoints y convenciones REST
3. **Arquitectura**: Implementar separación por capas
4. **Persistencia**: Modelar datos con JPA
5. **Lógica de negocio**: Implementar servicios
6. **Seguridad**: Configurar autenticación y autorización
7. **Validación**: Asegurar integridad de datos
8. **Manejo de errores**: Respuestas consistentes
9. **Testing**: Cobertura de código
10. **Documentación**: Facilitar consumo de la API

### 16.2 Nivel Alcanzado

Al seguir esta guía, habrás desarrollado una API que cumple con:

- ✅ **Arquitectura profesional**: Separación de responsabilidades
- ✅ **Seguridad implementada**: JWT, roles, CORS
- ✅ **Validación robusta**: Bean Validation en múltiples capas
- ✅ **Manejo de errores**: Excepciones personalizadas y respuestas estándar
- ✅ **Documentación**: Swagger/OpenAPI completo
- ✅ **Testing**: Tests unitarios e integración
- ✅ **Código limpio**: Principios SOLID y Clean Code
- ✅ **Escalabilidad**: Preparado para crecer

### 16.3 Próximos Pasos

**Mejoras adicionales:**

1. **Performance**:
   - Implementar caché distribuido (Redis)
   - Optimizar consultas con índices
   - Implementar paginación eficiente

2. **Observabilidad**:
   - Logging estructurado (Logback, Log4j2)
   - Métricas (Micrometer, Prometheus)
   - Tracing distribuido (Sleuth, Zipkin)

3. **Despliegue**:
   - Docker y Docker Compose
   - CI/CD (GitHub Actions, GitLab CI)
   - Kubernetes para orquestación

4. **Funcionalidades avanzadas**:
   - WebSockets para notificaciones en tiempo real
   - GraphQL como alternativa a REST
   - Eventos asíncronos (Spring Events, RabbitMQ)

5. **Calidad**:
   - Análisis estático de código (SonarQube)
   - Coverage de tests > 80%
   - Code reviews

---

## 📚 Recursos Adicionales

- [Spring Boot Official Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA Documentation](https://spring.io/projects/spring-data-jpa)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [REST API Best Practices](https://restfulapi.net/)
- [Bean Validation Specification](https://beanvalidation.org/)

---

**Documento generado a partir del análisis de APIs profesionales desarrolladas con Spring Boot.**

*Este documento es independiente del dominio y puede adaptarse a cualquier proyecto de API REST.*
