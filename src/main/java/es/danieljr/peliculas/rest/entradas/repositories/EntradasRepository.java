package es.danieljr.peliculas.rest.entradas.repositories;

import es.danieljr.peliculas.rest.entradas.models.Entrada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntradasRepository extends JpaRepository<Entrada, Long>, JpaSpecificationExecutor<Entrada> {
    List<Entrada> findByPeliculaIdPelicula(Long peliculaId);
    
    // Métodos con paginación
    Page<Entrada> findByPeliculaIdPelicula(Long peliculaId, Pageable pageable);
    
    Page<Entrada> findAll(Pageable pageable);

    @Query("SELECT e FROM Entrada e WHERE e.usuario.id = :usuarioId")
    Page<Entrada> findByUsuarioId(Long usuarioId, Pageable pageable);

    @Query("SELECT e FROM Entrada e WHERE e.usuario.id = :usuarioId")
    List<Entrada> findByUsuarioId(Long usuarioId);

    // Obtiene si existe una entrada con el id del usuario
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Entrada e WHERE e.usuario.id = :id")
    Boolean existsByUsuarioId(Long id);
}








