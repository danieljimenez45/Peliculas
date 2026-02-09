package es.danieljr.peliculas.rest.peliculas.services;

import es.danieljr.peliculas.rest.peliculas.dto.PeliculaCreateDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaUpdateDto;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PeliculasService {
    Page<PeliculaResponseDto> findAll(Optional<String> titulo, Optional<String> genero, Pageable pageable);

    PeliculaResponseDto findById(Long id);

    /** Para uso en la capa web (vistas, controladores MVC). */
    Optional<Pelicula> buscarPorId(Long id);

    PeliculaResponseDto save(PeliculaCreateDto peliculaCreateDto);

    PeliculaResponseDto update(Long id, PeliculaUpdateDto peliculaUpdateDto);

    void deleteById(Long id);
}
