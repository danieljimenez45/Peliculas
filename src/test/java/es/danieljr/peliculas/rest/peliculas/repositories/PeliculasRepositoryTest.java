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

@Sql(value = {"/reset.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DataJpaTest
class PeliculasRepositoryTest {

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

  @Autowired
  private PeliculasRepository repositorio;
  @Autowired
  private TestEntityManager entityManager;

  @BeforeEach
  void setUp() {
    entityManager.persist(pelicula1);
    entityManager.persist(pelicula2);
    entityManager.flush();
  }

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

  @Test
  void existsById_existingId_returnsTrue() {
    // Act
    Long id = 1L;
    boolean exists = repositorio.existsById(id);

    // Assert
    assertTrue(exists);
  }

  @Test
  void existsById_nonExistingId_returnsFalse() {
    // Act
    Long id = 4L;
    boolean exists = repositorio.existsById(id);

    // Assert
    assertFalse(exists);
  }

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





