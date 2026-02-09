package es.danieljr.peliculas.rest.peliculas.repositories;

import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PeliculasRepository extends JpaRepository<Pelicula, Long>, JpaSpecificationExecutor<Pelicula> {
    
    List<Pelicula> findByTitulo(String titulo);
    
    List<Pelicula> findByGenero(String genero);
    
    List<Pelicula> findByTituloAndGenero(String titulo, String genero);
    
    // Métodos con paginación
    Page<Pelicula> findByTitulo(String titulo, Pageable pageable);
    
    Page<Pelicula> findByGenero(String genero, Pageable pageable);
    
    Page<Pelicula> findByTituloAndGenero(String titulo, String genero, Pageable pageable);

    Page<Pelicula> findAll(Pageable pageable);
}








