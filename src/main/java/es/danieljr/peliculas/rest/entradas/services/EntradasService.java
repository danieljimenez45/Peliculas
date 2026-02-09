package es.danieljr.peliculas.rest.entradas.services;

import es.danieljr.peliculas.rest.entradas.dto.EntradaCreateDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaResponseDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface EntradasService {
    Page<EntradaResponseDto> findAll(Optional<Long> peliculaId, Optional<LocalDate> fechaDesde, Optional<LocalDate> fechaHasta,
                          Optional<Double> precioMin, Optional<Double> precioMax, Optional<String> metodoPago, Pageable pageable);
    EntradaResponseDto findById(Long id);

    Page<EntradaResponseDto> findByUsuarioId(Long usuarioId, Pageable pageable);
    EntradaResponseDto findByUsuarioId(Long usuarioId, Long idEntrada);

    EntradaResponseDto save(EntradaCreateDto entradaCreateDto);
    EntradaResponseDto save(EntradaCreateDto entradaCreateDto, Long usuarioId);

    EntradaResponseDto update(Long id, EntradaUpdateDto entradaUpdateDto);
    EntradaResponseDto update(Long id, EntradaUpdateDto entradaUpdateDto, Long usuarioId);

    void deleteById(Long id);
    void deleteById(Long id, Long usuarioId);
}









