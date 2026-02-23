# 3. Entidades JPA, relaciones y persistencia

## 3.1 Entidad básica con @Entity

Una entidad es una clase que se mapea a una tabla. Anotaciones mínimas: `@Entity`, `@Table` (opcional), `@Id`, `@GeneratedValue`, y en cada campo `@Column` si quieres controlar nombre o restricciones.

### Ejemplo: Tarjeta (DWES 25-26)

```java
@Entity
@Table(name = "TARJETAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarjeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 19)
    private String numero;

    @Column(nullable = false, length = 3)
    private String cvc;

    @Column(nullable = false)
    private LocalDate fechaCaducidad;

    @Column(nullable = false)
    private Double saldo;

    @Column(updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(unique = true, updatable = false, nullable = false)
    @Builder.Default
    private UUID uuid = UUID.randomUUID();

    @Column(columnDefinition = "boolean default false")
    @Builder.Default
    private Boolean isDeleted = false;

    // Relación: muchas tarjetas → un titular (ver siguiente sección)
    @ManyToOne
    @JoinColumn(name = "titular_id")
    private Titular titular;
}
```

### Ejemplo: Película (PELICULAS REPO)

```java
@Entity
@Table(name = "peliculas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pelicula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pelicula")
    private Long idPelicula;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String genero;

    @Column(nullable = false)
    private Integer duracion;

    private String sinopsis;

    @Column(name = "actores_principales")
    private String actoresPrincipales;

    @OneToMany(mappedBy = "pelicula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Entrada> entradas = new ArrayList<>();

    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

Resumen de anotaciones útiles:

- **@Entity**: clase persistente.
- **@Table(name = "…")**: nombre de la tabla.
- **@Id** + **@GeneratedValue(strategy = GenerationType.IDENTITY)**: PK autoincremental.
- **@Column(nullable = false, length = n, unique = true, updatable = false, columnDefinition = "…")**: definición de columna.
- **@Builder.Default**: valor por defecto cuando usas Lombok `@Builder`.

---

## 3.2 Relaciones: @OneToMany, @ManyToOne, @ManyToMany

### @ManyToOne (N : 1)

Varios registros de la entidad apuntan a uno de la otra. **La tabla “muchos” tiene la FK.**

- **Tarjeta → Titular**: muchas tarjetas, un titular.
- **Entrada → Película**: muchas entradas, una película.
- **Entrada → User**: muchas entradas, un usuario.

En la entidad “muchos”:

```java
@ManyToOne
@JoinColumn(name = "titular_id")  // nombre de la columna FK en la BD
private Titular titular;
```

Opciones útiles:

- **fetch = FetchType.LAZY**: no cargar el titular hasta que se acceda (recomendado si no siempre lo usas).
- **fetch = FetchType.EAGER**: cargar siempre (puede generar N+1 si hay lista de tarjetas).

### @OneToMany (1 : N) — lado “uno”

En el lado “uno” se usa `mappedBy` con el nombre del campo de la otra entidad que tiene la FK.

**Titular (DWES 25-26):**

```java
@OneToMany(mappedBy = "titular")
@JsonIgnoreProperties("titular")  // para no serializar en JSON y evitar ciclos
private List<Tarjeta> tarjetas;
```

**Pelicula (PELICULAS REPO):**

```java
@OneToMany(mappedBy = "pelicula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
@Builder.Default
private List<Entrada> entradas = new ArrayList<>();
```

- **mappedBy = "pelicula"**: la FK está en `Entrada.pelicula`.
- **cascade = CascadeType.ALL**: persist/merge/remove en Película se propaga a Entradas (útil para borrar película y sus entradas).
- **orphanRemoval = true**: si quitas una Entrada de la lista y haces flush, se borra de la BD.
- **FetchType.LAZY**: no cargar entradas hasta que se acceda a la lista (evita cargar todo en cada consulta de película).

### @ManyToMany

Se usa cuando hay relación N:N (ej.: usuarios y roles en otra implementación). Suele requerir tabla intermedia. En Películas, User tiene roles como `@ElementCollection` de enum, no como entidad relacionada; si te piden ManyToMany, sería algo así:

```java
@ManyToMany
@JoinTable(
    name = "user_roles",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<Role> roles;
```

---

## 3.3 FetchType: LAZY vs EAGER

| Tipo   | Cuándo se carga la relación |
|--------|-----------------------------|
| LAZY   | Al acceder al campo (getter) por primera vez. Puede dar LazyInitializationException si accedes fuera de una transacción/sesión. |
| EAGER  | Siempre que se carga la entidad principal. Puede provocar muchas consultas (N+1) si listas muchas entidades. |

Recomendación en exámenes:

- **@ManyToOne**: `FetchType.LAZY` por defecto; en servicios, si necesitas el relacionado, haz un join en la query o un fetch explícito.
- **@OneToMany**: usar **LAZY** y no exponer la colección directamente en la API; mejor devolver DTOs que ya traigan lo necesario desde el servicio.

---

## 3.4 Cascade

Indica qué operaciones se propagan de la entidad padre a la relacionada:

- **CascadeType.PERSIST**: al hacer `persist` del padre, se persisten los hijos.
- **CascadeType.MERGE**: al actualizar el padre, se actualizan los hijos.
- **CascadeType.REMOVE**: al borrar el padre, se borran los hijos.
- **CascadeType.ALL**: todas las anteriores.

Ejemplo Películas: al borrar una Película, con `CascadeType.ALL` + `orphanRemoval = true` se borran sus Entradas. Sin cascade, tendrías que borrar antes las entradas o tendrías error de FK.

---

## 3.5 Buenas prácticas (resumen)

1. **Constructor sin argumentos**: JPA lo necesita; Lombok `@NoArgsConstructor` lo genera.
2. **Evitar ciclos en JSON**: en relaciones bidireccionales usar `@JsonIgnore` o `@JsonIgnoreProperties("campo")` en el lado que no quieras serializar.
3. **No exponer entidades en la API**: usar DTOs y mappers (TarjetaResponseDto, PeliculaResponseDto, etc.).
4. **Nombres de tabla/columna**: `@Table` y `@Column(name = "…")` para seguir convenciones de BD (snake_case en BD, camelCase en Java).
5. **LAZY en OneToMany y ManyToOne** por defecto; EAGER solo si realmente lo necesitas y controlas el número de consultas.
6. **Soft delete**: en lugar de borrar, marcar `isDeleted = true` y filtrar en consultas (como en Tarjeta/Titular); en examen puedes hacer delete físico si no lo piden expresamente.

Con esto cubres entidades, relaciones y persistencia tal como se usan en DWES 25-26 y PELICULAS REPO. El siguiente tema es paginación, ordenación y filtros.
