package es.danieljr.peliculas.rest.entradas.mappers;

import es.danieljr.peliculas.rest.entradas.dto.EntradaCreateDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaResponseDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaUpdateDto;
import es.danieljr.peliculas.rest.entradas.models.Entrada;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EntradaMapper {

    public Entrada toEntrada(EntradaCreateDto entradaCreateDto) {
        LocalDateTime now = LocalDateTime.now();
        return Entrada.builder()
                .idEntrada(null)
                .fecha(entradaCreateDto.getFecha())
                .precio(entradaCreateDto.getPrecio())
                .metodoPago(entradaCreateDto.getMetodoPago())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public Entrada toEntrada(EntradaUpdateDto entradaUpdateDto, Entrada entrada) {
        return Entrada.builder()
                .idEntrada(entrada.getIdEntrada())
                .fecha(entradaUpdateDto.getFecha() != null ? entradaUpdateDto.getFecha() : entrada.getFecha())
                .precio(entradaUpdateDto.getPrecio() != null ? entradaUpdateDto.getPrecio() : entrada.getPrecio())
                .metodoPago(entradaUpdateDto.getMetodoPago() != null ? entradaUpdateDto.getMetodoPago() : entrada.getMetodoPago())
                .pelicula(entrada.getPelicula())
                .usuario(entrada.getUsuario())
                .createdAt(entrada.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public EntradaResponseDto toEntradaResponseDto(Entrada entrada) {
        return EntradaResponseDto.builder()
                .idEntrada(entrada.getIdEntrada())
                .fecha(entrada.getFecha())
                .precio(entrada.getPrecio())
                .metodoPago(entrada.getMetodoPago())
                .peliculaId(entrada.getPelicula() != null ? entrada.getPelicula().getIdPelicula() : null)
                .createdAt(entrada.getCreatedAt())
                .updatedAt(entrada.getUpdatedAt())
                .build();
    }

    public List<EntradaResponseDto> toResponseDtoList(List<Entrada> entradas) {
        return entradas.stream()
                .map(this::toEntradaResponseDto)
                .toList();
    }
}








