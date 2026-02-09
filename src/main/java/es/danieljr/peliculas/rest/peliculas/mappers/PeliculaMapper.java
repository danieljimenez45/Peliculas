package es.danieljr.peliculas.rest.peliculas.mappers;

import es.danieljr.peliculas.rest.peliculas.dto.PeliculaCreateDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaUpdateDto;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PeliculaMapper {

    public Pelicula toPelicula(PeliculaCreateDto peliculaCreateDto) {
        LocalDateTime now = LocalDateTime.now();
        return Pelicula.builder()
                .idPelicula(null)
                .titulo(peliculaCreateDto.getTitulo())
                .genero(peliculaCreateDto.getGenero())
                .duracion(peliculaCreateDto.getDuracion())
                .sinopsis(peliculaCreateDto.getSinopsis())
                .actoresPrincipales(peliculaCreateDto.getActoresPrincipales())
                .actoresSecundarios(peliculaCreateDto.getActoresSecundarios())
                .director(peliculaCreateDto.getDirector())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public Pelicula toPelicula(PeliculaUpdateDto peliculaUpdateDto, Pelicula pelicula) {
        return Pelicula.builder()
                .idPelicula(pelicula.getIdPelicula())
                .titulo(peliculaUpdateDto.getTitulo() != null ? peliculaUpdateDto.getTitulo() : pelicula.getTitulo())
                .genero(peliculaUpdateDto.getGenero() != null ? peliculaUpdateDto.getGenero() : pelicula.getGenero())
                .duracion(peliculaUpdateDto.getDuracion() != null ? peliculaUpdateDto.getDuracion() : pelicula.getDuracion())
                .sinopsis(peliculaUpdateDto.getSinopsis() != null ? peliculaUpdateDto.getSinopsis() : pelicula.getSinopsis())
                .actoresPrincipales(peliculaUpdateDto.getActoresPrincipales() != null ? peliculaUpdateDto.getActoresPrincipales() : pelicula.getActoresPrincipales())
                .actoresSecundarios(peliculaUpdateDto.getActoresSecundarios() != null ? peliculaUpdateDto.getActoresSecundarios() : pelicula.getActoresSecundarios())
                .director(peliculaUpdateDto.getDirector() != null ? peliculaUpdateDto.getDirector() : pelicula.getDirector())
                .createdAt(pelicula.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .entradas(pelicula.getEntradas())
                .build();
    }

    public PeliculaResponseDto toPeliculaResponseDto(Pelicula pelicula) {
        return PeliculaResponseDto.builder()
                .idPelicula(pelicula.getIdPelicula())
                .titulo(pelicula.getTitulo())
                .genero(pelicula.getGenero())
                .duracion(pelicula.getDuracion())
                .sinopsis(pelicula.getSinopsis())
                .actoresPrincipales(pelicula.getActoresPrincipales())
                .actoresSecundarios(pelicula.getActoresSecundarios())
                .director(pelicula.getDirector())
                .entradaIds(Hibernate.isInitialized(pelicula.getEntradas()) && pelicula.getEntradas() != null ? 
                    pelicula.getEntradas().stream().map(e -> e.getIdEntrada()).collect(Collectors.toList()) : 
                    Collections.emptyList())
                .createdAt(pelicula.getCreatedAt())
                .updatedAt(pelicula.getUpdatedAt())
                .build();
    }

    public List<PeliculaResponseDto> toResponseDtoList(List<Pelicula> peliculas) {
        return peliculas.stream()
                .map(this::toPeliculaResponseDto)
                .toList();
    }
}









