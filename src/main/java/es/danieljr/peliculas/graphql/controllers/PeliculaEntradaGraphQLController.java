package es.danieljr.peliculas.graphql.controllers;

import es.danieljr.peliculas.rest.entradas.models.Entrada;
import es.danieljr.peliculas.rest.entradas.repositories.EntradasRepository;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import es.danieljr.peliculas.rest.peliculas.repositories.PeliculasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Controller
// @PreAuthorize("hasAnyRole('USER')") // Protección a nivel de clase
public class PeliculaEntradaGraphQLController {
  private final PeliculasRepository peliculasRepository;
  private final EntradasRepository entradasRepository;


  // --- QUERIES ---

  @QueryMapping
  public List<Pelicula> peliculas() {
    // Devuelve todas las películas como entidades (ojo: no paginado)
    return peliculasRepository.findAll();
  }

  @QueryMapping
  public Pelicula peliculaById(@Argument Long id) {
    // Devuelve una película por su id
    Optional<Pelicula> peliculaOpt = peliculasRepository.findById(id);
    return peliculaOpt.orElse(null);
  }

  @QueryMapping
  public List<Pelicula> peliculasByTitulo(@Argument String titulo) {
    // Devuelve las películas que coinciden con el título
    return peliculasRepository.findByTitulo(titulo);
    // En caso de que no encuentre ninguna, devuelve una lista vacía
  }

  @QueryMapping
  public List<Pelicula> peliculasByGenero(@Argument String genero) {
    // Devuelve las películas que coinciden con el género
    return peliculasRepository.findByGenero(genero);
    // En caso de que no encuentre ninguna, devuelve una lista vacía
  }

  @QueryMapping
  public List<Entrada> entradas() {
    // Devuelve todas las entradas como entidades
    return entradasRepository.findAll();
  }

  @QueryMapping
  public Entrada entradaById(@Argument Long id) {
    // Devuelve una entrada por id
    return entradasRepository.findById(id).orElse(null);
  }

  // --- RESOLVERS RELACIONES ---

  @SchemaMapping(typeName = "Pelicula", field = "entradas")
  public List<Entrada> entradas(Pelicula pelicula) {
    // Devuelve las entradas de una película
    return entradasRepository.findByPeliculaIdPelicula(pelicula.getIdPelicula());
  }

  @SchemaMapping(typeName = "Entrada", field = "pelicula")
  public Pelicula pelicula(Entrada entrada) {
    // Devuelve la película de la entrada (ya viene cargada en la entidad)
    return entrada.getPelicula();
  }

  // --- RESOLVERS DE CAMPOS ---

  @SchemaMapping(typeName = "Pelicula", field = "id")
  public Long peliculaId(Pelicula pelicula) {
    return pelicula.getIdPelicula();
  }

  @SchemaMapping(typeName = "Entrada", field = "id")
  public Long entradaId(Entrada entrada) {
    return entrada.getIdEntrada();
  }

  @SchemaMapping(typeName = "Pelicula", field = "createdAt")
  public String peliculaCreatedAt(Pelicula pelicula) {
    return pelicula.getCreatedAt() != null ? pelicula.getCreatedAt().toString() : null;
  }

  @SchemaMapping(typeName = "Pelicula", field = "updatedAt")
  public String peliculaUpdatedAt(Pelicula pelicula) {
    return pelicula.getUpdatedAt() != null ? pelicula.getUpdatedAt().toString() : null;
  }

  @SchemaMapping(typeName = "Entrada", field = "fecha")
  public String entradaFecha(Entrada entrada) {
    return entrada.getFecha() != null ? entrada.getFecha().toString() : null;
  }

  @SchemaMapping(typeName = "Entrada", field = "createdAt")
  public String entradaCreatedAt(Entrada entrada) {
    return entrada.getCreatedAt() != null ? entrada.getCreatedAt().toString() : null;
  }

  @SchemaMapping(typeName = "Entrada", field = "updatedAt")
  public String entradaUpdatedAt(Entrada entrada) {
    return entrada.getUpdatedAt() != null ? entrada.getUpdatedAt().toString() : null;
  }
}
