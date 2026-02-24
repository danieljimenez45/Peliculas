package es.danieljr.peliculas.rest.peliculas.repositories;

import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del repositorio de Películas (capa de acceso a datos).
 * Usa una BD H2 en memoria; no se levanta el servidor ni el resto de la aplicación.
 */
@Sql(value = {"/reset.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) // Ejecuta reset.sql antes de cada test para estado limpio
@DataJpaTest // Arranca solo JPA + repositorios; configura BD embebida
class PeliculasRepositoryTest {

  // ========== Datos de prueba (entidades que se persisten en setUp) ==========
  private final Pelicula pelicula1 = Pelicula.builder()
      .titulo("El Padrino")
      .genero("Drama")
      .duracion(175)
      .sinopsis("La historia de una familia de la mafia")
      .actoresPrincipales("Marlon Brando, Al Pacino")
      .actoresSecundarios("James Caan, Robert Duvall")
      .director("Francis Ford Coppola")
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();

  private final Pelicula pelicula2 = Pelicula.builder()
      .titulo("Matrix")
      .genero("Ciencia Ficción")
      .duracion(136)
      .sinopsis("Un programador descubre la verdad sobre la realidad")
      .actoresPrincipales("Keanu Reeves, Laurence Fishburne")
      .actoresSecundarios("Carrie-Anne Moss, Hugo Weaving")
      .director("Lana Wachowski, Lilly Wachowski")
      .createdAt(LocalDateTime.now())
      .updatedAt(LocalDateTime.now())
      .build();

  /** Repositorio a probar; inyectado por Spring en el contexto DataJpaTest. */
  @Autowired
  private PeliculasRepository repositorio;
  /** Permite persistir entidades en el test sin usar el repositorio (útil para preparar datos). */
  @Autowired
  private TestEntityManager entityManager;

  /** Antes de cada test: persiste las dos películas de prueba en la BD para tener datos conocidos. */
  @BeforeEach
  void setUp() {
    entityManager.persist(pelicula1);
    entityManager.persist(pelicula2);
    entityManager.flush();
  }

  /** Comprueba que findAll() devuelve todas las películas (las 2 insertadas en setUp). */
  @Test
  void findAll() {
    // Act
    List<Pelicula> peliculas = repositorio.findAll();

    // Assert
    assertAll("findAll",
        () -> assertNotNull(peliculas),
        () -> assertEquals(2, peliculas.size())
    );
  }

  /** Comprueba que findById con un id existente (1L) devuelve un Optional con la película. */
  @Test
  void findById_existingId_returnsOptionalWithPelicula() {
    // Act
    Long id = 1L;
    Optional<Pelicula> optionalPelicula = repositorio.findById(id);

    // Assert
    assertAll("findById_existingId_returnsOptionalWithPelicula",
        () -> assertNotNull(optionalPelicula),
        () -> assertTrue(optionalPelicula.isPresent()),
        () -> assertEquals(id, optionalPelicula.get().getIdPelicula())
    );
  }

  /** Comprueba que findById con un id que no existe (4L) devuelve Optional vacío. */
  @Test
  void findById_nonExistingId_returnsEmptyOptional() {
    // Act
    Long id = 4L;
    Optional<Pelicula> optionalPelicula = repositorio.findById(id);

    // Assert
    assertAll("findById_nonExistingId_returnsEmptyOptional",
        () -> assertNotNull(optionalPelicula),
        () -> assertTrue(optionalPelicula.isEmpty())
    );
  }

  /** Comprueba que existsById devuelve true cuando el id existe. */
  @Test
  void existsById_existingId_returnsTrue() {
    // Act
    Long id = 1L;
    boolean exists = repositorio.existsById(id);

    // Assert
    assertTrue(exists);
  }

  /** Comprueba que existsById devuelve false cuando el id no existe. */
  @Test
  void existsById_nonExistingId_returnsFalse() {
    // Act
    Long id = 4L;
    boolean exists = repositorio.existsById(id);

    // Assert
    assertFalse(exists);
  }

  /** Comprueba que save() con una película nueva (sin id) la inserta y el total de registros aumenta. */
  @Test
  void save_notExists() {
    // Arrange
    Pelicula pelicula = Pelicula.builder()
        .titulo("Inception")
        .genero("Ciencia Ficción")
        .duracion(148)
        .sinopsis("Un ladrón que roba secretos del subconsciente")
        .actoresPrincipales("Leonardo DiCaprio")
        .actoresSecundarios("Marion Cotillard, Tom Hardy")
        .director("Christopher Nolan")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    // Act
    Pelicula savedPelicula = repositorio.save(pelicula);
    var all = repositorio.findAll();

    // Assert
    assertAll("save",
        () -> assertNotNull(savedPelicula),
        () -> assertEquals(pelicula.getTitulo(), savedPelicula.getTitulo()),
        () -> assertEquals(3, all.size())
    );
  }

  /** Comprueba que save() con una película que ya tiene id actualiza el registro existente. */
  @Test
  void save_butExists() {
    // Arrange
    Long id = 1L;
    Pelicula peliculaExistente = Pelicula.builder()
        .idPelicula(id)
        .titulo("El Padrino: Parte II")
        .genero("Drama")
        .duracion(200)
        .sinopsis("Continuación de la historia")
        .actoresPrincipales("Marlon Brando, Al Pacino")
        .actoresSecundarios("Robert De Niro")
        .director("Francis Ford Coppola")
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    //Act
    Pelicula savedPelicula = repositorio.save(peliculaExistente);
    var all = repositorio.findAll();

    // Assert
    assertAll("save",
        () -> assertNotNull(savedPelicula),
        () -> assertTrue(repositorio.existsById(id)),
        () -> assertTrue(all.size() >= 2)
    );
  }

  /** Comprueba que deleteById() elimina la película y el número de registros disminuye. */
  @Test
  void deleteById_existingId() {
    // Act
    Long id = 1L;
    repositorio.deleteById(id);
    var all = repositorio.findAll();

    // Assert
    assertAll("deleteById_existingId",
        () -> assertEquals(1, all.size()),
        () -> assertFalse(repositorio.existsById(id))
    );
  }

  /** Comprueba que findAll(Pageable) devuelve una página con el tamaño indicado (1 elemento). */
  @Test
  void findAll_WithPagination_ShouldReturnPagedResults() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 1, Sort.by("idPelicula").ascending());

    // Act
    Page<Pelicula> page = repositorio.findAll(pageable);

    // Assert
    assertAll("findAll with pagination",
        () -> assertNotNull(page),
        () -> assertEquals(1, page.getSize()),
        () -> assertTrue(page.getTotalElements() >= 1),
        () -> assertFalse(page.isEmpty())
    );
  }

  /** Comprueba que findAll(Specification, Pageable) filtra por criterio (título contiene "padrino"). */
  @Test
  void findAll_WithSpecificationAndPagination_ShouldReturnFilteredPagedResults() {
    // Arrange
    Specification<Pelicula> spec = (root, query, criteriaBuilder) ->
        criteriaBuilder.like(criteriaBuilder.lower(root.get("titulo")), "%padrino%");
    Pageable pageable = PageRequest.of(0, 10, Sort.by("idPelicula").ascending());

    // Act
    Page<Pelicula> page = repositorio.findAll(spec, pageable);

    // Assert
    assertAll("findAll with specification and pagination",
        () -> assertNotNull(page),
        () -> assertTrue(page.getTotalElements() >= 1),
        () -> assertTrue(page.getContent().stream()
            .allMatch(p -> p.getTitulo().toLowerCase().contains("padrino")))
    );
  }

  /** Comprueba que findAll con Sort ascendente por título ordena correctamente los resultados. */
  @Test
  void findAll_WithSortAscending_ShouldReturnSortedResults() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10, Sort.by("titulo").ascending());

    // Act
    Page<Pelicula> page = repositorio.findAll(pageable);
    List<Pelicula> content = page.getContent();

    // Assert
    assertAll("findAll with ascending sort",
        () -> assertNotNull(page),
        () -> assertTrue(content.size() >= 2),
        () -> assertTrue(content.get(0).getTitulo().compareToIgnoreCase(content.get(1).getTitulo()) <= 0)
    );
  }

  /** Comprueba que findAll con Sort descendente por título ordena correctamente los resultados. */
  @Test
  void findAll_WithSortDescending_ShouldReturnSortedResults() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10, Sort.by("titulo").descending());

    // Act
    Page<Pelicula> page = repositorio.findAll(pageable);
    List<Pelicula> content = page.getContent();

    // Assert
    assertAll("findAll with descending sort",
        () -> assertNotNull(page),
        () -> assertTrue(content.size() >= 2),
        () -> assertTrue(content.get(0).getTitulo().compareToIgnoreCase(content.get(1).getTitulo()) >= 0)
    );
  }
}





