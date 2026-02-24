package es.danieljr.peliculas.rest.entradas.repositories;

import es.danieljr.peliculas.rest.entradas.models.Entrada;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del repositorio de Entradas (capa de acceso a datos).
 * Usa DataJpaTest + TestEntityManager; persiste Película y Entradas en setUp.
 */
@Sql(value = {"/reset.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DataJpaTest
class EntradasRepositoryTest {

  // ========== Datos de prueba (película y entradas asociadas) ==========
  private final Pelicula pelicula = Pelicula.builder()
      .titulo("El Padrino")
      .genero("Drama")
      .duracion(175)
      .director("Francis Ford Coppola")
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();

  private final Entrada entrada1 = Entrada.builder()
      .fecha(LocalDate.of(2025, 12, 15))
      .precio(10.0)
      .metodoPago("Tarjeta")
      .pelicula(pelicula)
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();

  private final Entrada entrada2 = Entrada.builder()
      .fecha(LocalDate.of(2025, 12, 16))
      .precio(12.0)
      .metodoPago("Efectivo")
      .pelicula(pelicula)
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();

  @Autowired
  private EntradasRepository repositorio;
  @Autowired
  private TestEntityManager entityManager;

  /** Persiste una película y dos entradas antes de cada test para tener datos conocidos. */
  @BeforeEach
  void setUp() {
    entityManager.persist(pelicula);
    entityManager.persist(entrada1);
    entityManager.persist(entrada2);
    entityManager.flush();
  }

  /** Comprueba que findAll() devuelve todas las entradas insertadas en setUp. */
  @Test
  void findAll() {
    // Act
    List<Entrada> entradas = repositorio.findAll();

    // Assert
    assertAll("findAll",
        () -> assertNotNull(entradas),
        () -> assertFalse(entradas.isEmpty())
    );
  }

  /** Comprueba que findById() devuelve la entrada cuando el id existe. */
  @Test
  void findById() {
    // Act
    Entrada entrada = repositorio.findById(1L).orElse(null);

    // Assert
    assertAll("findById",
        () -> assertNotNull(entrada),
        () -> assertEquals("Tarjeta", entrada.getMetodoPago())
    );
  }

  /** findById con id inexistente devuelve Optional vacío. */
  @Test
  void findByIdNotFound() {
    // Act
    Entrada entrada = repositorio.findById(100L).orElse(null);

    // Assert
    assertNull(entrada);
  }

  /** save con entrada nueva la inserta y devuelve entidad con id generado. */
  @Test
  void save() {
    // Act
    Entrada entrada = repositorio.save(Entrada.builder()
        .fecha(LocalDate.of(2025, 12, 20))
        .precio(15.0)
        .metodoPago("Transferencia")
        .pelicula(pelicula)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build());

    // Assert
    assertAll("save",
        () -> assertNotNull(entrada),
        () -> assertEquals("Transferencia", entrada.getMetodoPago())
    );
  }

  /** save con entrada existente (con id) actualiza el registro. */
  @Test
  void update() {
    // Act
    var entradaExistente = repositorio.findById(1L).orElse(null);
    Entrada entradaActualizar = Entrada.builder()
        .idEntrada(entradaExistente.getIdEntrada())
        .fecha(LocalDate.of(2025, 12, 25))
        .precio(20.0)
        .metodoPago("Efectivo")
        .pelicula(pelicula)
        .createdAt(entradaExistente.getCreatedAt())
        .updatedAt(LocalDateTime.now())
        .build();
    Entrada entradaActualizada = repositorio.save(entradaActualizar);

    // Assert
    assertAll("update",
        () -> assertNotNull(entradaActualizada),
        () -> assertEquals(20.0, entradaActualizada.getPrecio())
    );
  }

  /** deleteById elimina la entrada y ya no existe en findAll. */
  @Test
  void delete() {
    // Act
    var entradaBorrar = repositorio.findById(1L).orElse(null);
    repositorio.delete(entradaBorrar);
    Entrada entradaBorrada = repositorio.findById(1L).orElse(null);

    // Assert
    assertNull(entradaBorrada);
  }

  /** findAll(Pageable) devuelve una página con el tamaño indicado. */
  @Test
  void findAll_WithPagination_ShouldReturnPagedResults() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 1, Sort.by("idEntrada").ascending());

    // Act
    Page<Entrada> page = repositorio.findAll(pageable);

    // Assert
    assertAll("findAll with pagination",
        () -> assertNotNull(page),
        () -> assertEquals(1, page.getSize()),
        () -> assertTrue(page.getTotalElements() >= 1),
        () -> assertFalse(page.isEmpty())
    );
  }

  /** findAll(Specification, Pageable) filtra por criterio (ej. por película) y devuelve página. */
  @Test
  void findAll_WithSpecificationAndPagination_ShouldReturnFilteredPagedResults() {
    // Arrange
    Specification<Entrada> spec = (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("metodoPago"), "Tarjeta");
    Pageable pageable = PageRequest.of(0, 10, Sort.by("idEntrada").ascending());

    // Act
    Page<Entrada> page = repositorio.findAll(spec, pageable);

    // Assert
    assertAll("findAll with specification and pagination",
        () -> assertNotNull(page),
        () -> assertTrue(page.getTotalElements() >= 1),
        () -> assertTrue(page.getContent().stream()
            .allMatch(e -> e.getMetodoPago().equals("Tarjeta")))
    );
  }

  /** findAll con Sort ascendente ordena los resultados correctamente. */
  @Test
  void findAll_WithSortAscending_ShouldReturnSortedResults() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10, Sort.by("precio").ascending());

    // Act
    Page<Entrada> page = repositorio.findAll(pageable);
    List<Entrada> content = page.getContent();

    // Assert
    assertAll("findAll with ascending sort",
        () -> assertNotNull(page),
        () -> assertTrue(content.size() >= 2),
        () -> assertTrue(content.get(0).getPrecio() <= content.get(1).getPrecio())
    );
  }

  /** findAll con Sort descendente ordena los resultados correctamente. */
  @Test
  void findAll_WithSortDescending_ShouldReturnSortedResults() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10, Sort.by("precio").descending());

    // Act
    Page<Entrada> page = repositorio.findAll(pageable);
    List<Entrada> content = page.getContent();

    // Assert
    assertAll("findAll with descending sort",
        () -> assertNotNull(page),
        () -> assertTrue(content.size() >= 2),
        () -> assertTrue(content.get(0).getPrecio() >= content.get(1).getPrecio())
    );
  }
}





