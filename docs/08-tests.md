# 8. Realización de tests

En ambos proyectos hay tests para controladores REST (MockMvc), servicios (con mocks) y a veces repositorios y mappers. Aquí se resume el patrón usado y buenas prácticas para el examen.

---

## 8.1 Dependencias

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

En Películas, además:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 8.2 Tests de controlador REST con MockMvc

Se prueba el **controller** sin levantar el servidor completo: se simulan peticiones HTTP y se comprueban status y cuerpo (o cabeceras). El **servicio** se sustituye por un **mock** para controlar las respuestas.

### Configuración (PeliculasRestControllerTest)

```java
@SpringBootTest
@AutoConfigureMockMvc
class PeliculasRestControllerTest {

    private final String ENDPOINT = "/api/v1/peliculas";

    @Autowired
    private MockMvcTester mockMvcTester;   // o MockMvc en versiones anteriores

    @MockitoBean
    private PeliculasService peliculasService;
}
```

- **@SpringBootTest**: carga el contexto de la aplicación (pero con mocks donde haya @MockitoBean).
- **@AutoConfigureMockMvc**: configura MockMvc (o MockMvcTester) para enviar peticiones a los controladores.
- **@MockitoBean**: el bean real de PeliculasService se sustituye por un mock; en los tests se define el comportamiento con `when(...).thenReturn(...)`.

### GET lista (paginada)

```java
@Test
void getAll() {
    var peliculaResponses = List.of(peliculaResponse1, peliculaResponse2);
    var pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());
    var page = new PageImpl<>(peliculaResponses);

    when(peliculasService.findAll(Optional.empty(), Optional.empty(), pageable))
        .thenReturn(page);

    var result = mockMvcTester.get()
        .uri(ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange();

    assertThat(result).hasStatusOk();
    assertThat(result.bodyJson()).extractingPath("$.content.length()").isEqualTo(peliculaResponses.size());
    // más comprobaciones sobre content[0], content[1]...

    verify(peliculasService, times(1)).findAll(Optional.empty(), Optional.empty(), pageable);
}
```

- **when(...).thenReturn(page)**: cuando el controller llame a `findAll` con esos argumentos, el mock devuelve esa página.
- **mockMvcTester.get().uri(ENDPOINT).exchange()**: simula GET al endpoint.
- **assertThat(result).hasStatusOk()**: comprueba 200.
- **verify**: asegura que el servicio se llamó exactamente una vez con esos parámetros.

### GET por id (éxito y 404)

```java
@Test
void getById_Exists() {
    when(peliculasService.findById(1L)).thenReturn(peliculaResponse1);

    var result = mockMvcTester.get().uri(ENDPOINT + "/1").exchange();

    assertThat(result).hasStatusOk();
    assertThat(result.bodyJson()).convertTo(PeliculaResponseDto.class).isEqualTo(peliculaResponse1);
    verify(peliculasService, times(1)).findById(1L);
}

@Test
void getById_NotFound() {
    when(peliculasService.findById(999L)).thenThrow(new PeliculaNotFoundException(999L));

    var result = mockMvcTester.get().uri(ENDPOINT + "/999").exchange();

    assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    verify(peliculasService, times(1)).findById(999L);
}
```

### POST (crear)

```java
@Test
void create() {
    when(peliculasService.save(any(PeliculaCreateDto.class))).thenReturn(peliculaResponse1);

    var result = mockMvcTester.post()
        .uri(ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(peliculaCreateDto)
        .exchange();

    assertThat(result).hasStatus(HttpStatus.CREATED);
    verify(peliculasService, times(1)).save(any(PeliculaCreateDto.class));
}
```

Si el endpoint exige rol ADMIN (con @PreAuthorize), hay que simular un usuario autenticado (ver apartado de seguridad).

### DELETE (204 No Content)

```java
@Test
void delete() {
    doNothing().when(peliculasService).deleteById(1L);

    var result = mockMvcTester.delete().uri(ENDPOINT + "/1").exchange();

    assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
    verify(peliculasService, times(1)).deleteById(1L);
}
```

---

## 8.3 Tests con usuario autenticado (@WithMockUser)

Si el controller tiene `@PreAuthorize("hasRole('ADMIN')")`, las peticiones deben ir con un usuario “logado” en el contexto de seguridad. **spring-security-test** lo permite:

```java
@Test
@WithMockUser(roles = "ADMIN")
void create_asAdmin() {
    when(peliculasService.save(any(PeliculaCreateDto.class))).thenReturn(peliculaResponse1);
    var result = mockMvcTester.post()
        .uri(ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(peliculaCreateDto)
        .exchange();
    assertThat(result).hasStatus(HttpStatus.CREATED);
}

@Test
@WithMockUser(roles = "USER")
void create_asUser_Forbidden() {
    var result = mockMvcTester.post()
        .uri(ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(peliculaCreateDto)
        .exchange();
    assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
}
```

---

## 8.4 Tests unitarios de servicio

Se mockea el **repositorio** (y otros dependencias) y se prueba solo la lógica del servicio.

```java
@ExtendWith(MockitoExtension.class)
class PeliculasServiceImplTest {

    @Mock
    private PeliculasRepository peliculasRepository;

    @Mock
    private PeliculaMapper peliculaMapper;

    @InjectMocks
    private PeliculasServiceImpl peliculasService;

    @Test
    void findById_Exists() {
        when(peliculasRepository.findById(1L)).thenReturn(Optional.of(pelicula));
        when(peliculaMapper.toPeliculaResponseDto(pelicula)).thenReturn(peliculaResponseDto);

        var result = peliculasService.findById(1L);

        assertThat(result).isEqualTo(peliculaResponseDto);
        verify(peliculasRepository).findById(1L);
        verify(peliculaMapper).toPeliculaResponseDto(pelicula);
    }

    @Test
    void findById_NotFound() {
        when(peliculasRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> peliculasService.findById(999L))
            .isInstanceOf(PeliculaNotFoundException.class);

        verify(peliculasRepository).findById(999L);
    }
}
```

- **@ExtendWith(MockitoExtension.class)**: inicializa mocks.
- **@Mock**: repositorio y mapper son mocks.
- **@InjectMocks**: se crea la implementación del servicio inyectando los mocks.

---

## 8.5 Tests de repositorio (integración con BD)

Si quieres probar el repositorio contra una BD real (por ejemplo H2 en memoria), usas **@DataJpaTest** y opcionalmente **@Sql** para datos:

```java
@DataJpaTest
@Sql(scripts = "/reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class PeliculasRepositoryTest {

    @Autowired
    private PeliculasRepository peliculasRepository;

    @Test
    void findByTitulo() {
        var list = peliculasRepository.findByTitulo("Matrix", PageRequest.of(0, 10));
        assertThat(list.getContent()).hasSize(1);
    }
}
```

**reset.sql** en `src/test/resources` deja la BD en un estado conocido antes de cada test.

---

## 8.6 Validación en tests (400 Bad Request)

Si quieres comprobar que un POST con datos inválidos devuelve 400 y (opcionalmente) un cuerpo con errores:

```java
@Test
void create_InvalidDto_Returns400() {
    PeliculaCreateDto invalid = new PeliculaCreateDto();  // titulo null, etc.

    var result = mockMvcTester.post()
        .uri(ENDPOINT)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalid)
        .exchange();

    assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    // Si usas ProblemDetail con "errores":
    // assertThat(result.bodyJson()).extractingPath("$.errores").isNotNull();
}
```

---

## 8.7 Buenas prácticas (resumen)

1. **Nomenclatura**: `metodo_escenario_resultado` (ej. `getById_NotFound_Returns404`).
2. **Arrange-Act-Assert**: preparar mocks (Arrange), llamar al método/endpoint (Act), comprobar resultado y verify (Assert).
3. **Un concepto por test**: un test comprueba un solo comportamiento (éxito o un tipo de error).
4. **Mock solo lo necesario**: en tests de controller, mockear el service; en tests de service, mockear repository y mapper.
5. **Verificar interacciones**: `verify(service, times(1)).findById(1L)` para asegurar que se llamó al método esperado.
6. **Datos de test**: usar builders o constantes (peliculaResponse1, peliculaCreateDto) para no repetir objetos largos en cada test.

Para el examen suele bastar: un test de GET por id (OK y 404), un test de POST (201), un test de DELETE (204) y, si hay seguridad, un test con @WithMockUser para comprobar 403 cuando no tiene rol.
