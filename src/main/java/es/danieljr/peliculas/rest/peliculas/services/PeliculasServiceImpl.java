package es.danieljr.peliculas.rest.peliculas.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.danieljr.peliculas.config.websockets.WebSocketConfig;
import es.danieljr.peliculas.config.websockets.WebSocketHandler;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaCreateDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaResponseDto;
import es.danieljr.peliculas.rest.peliculas.dto.PeliculaUpdateDto;
import es.danieljr.peliculas.rest.peliculas.exceptions.PeliculaNotFoundException;
import es.danieljr.peliculas.rest.peliculas.mappers.PeliculaMapper;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import es.danieljr.peliculas.rest.peliculas.repositories.PeliculasRepository;
import es.danieljr.peliculas.websockets.notifications.dto.PeliculaNotificationResponse;
import es.danieljr.peliculas.websockets.notifications.mappers.PeliculaNotificationMapper;
import es.danieljr.peliculas.websockets.notifications.models.Notificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@CacheConfig(cacheNames = { "peliculas" })
@Slf4j
@RequiredArgsConstructor
@Service
public class PeliculasServiceImpl implements PeliculasService, InitializingBean {
    private final PeliculasRepository peliculasRepository;
    private final PeliculaMapper peliculaMapper;
    private final WebSocketConfig webSocketConfig;
    private final ObjectMapper objectMapper;
    private final PeliculaNotificationMapper peliculaNotificationMapper;
    private WebSocketHandler webSocketService;

    public void afterPropertiesSet() {
        this.webSocketService = this.webSocketConfig.webSocketPeliculasHandler();
    }

    public void setWebSocketService(WebSocketHandler webSocketHandler) {
        this.webSocketService = webSocketHandler;
    }

    @Override
    public Page<PeliculaResponseDto> findAll(Optional<String> titulo, Optional<String> genero, Pageable pageable) {
        log.info("Buscando películas por titulo: {}, genero: {}", titulo, genero);
        // Criterio de búsqueda por título
        Specification<Pelicula> specTitulo = (root, query, criteriaBuilder) -> titulo
                .map(t -> criteriaBuilder.like(criteriaBuilder.lower(root.get("titulo")), "%" + t.toLowerCase() + "%"))
                .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true))); // Si no hay título, no
                                                                                         // filtramos

        // Criterio de búsqueda por género
        Specification<Pelicula> specGenero = (root, query, criteriaBuilder) -> genero
                .map(g -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("genero")), g.toLowerCase()))
                .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true))); // Si no hay género, no
                                                                                         // filtramos

        // Combinamos las especificaciones
        Specification<Pelicula> criterio = Specification.allOf(specTitulo, specGenero);

        return peliculasRepository.findAll(criterio, pageable)
                .map(peliculaMapper::toPeliculaResponseDto);
    }

    @Cacheable(key = "#id")
    @Override
    public PeliculaResponseDto findById(Long id) {
        log.info("Buscando película por id {}", id);
        return peliculaMapper.toPeliculaResponseDto(peliculasRepository.findById(id)
                .orElseThrow(() -> new PeliculaNotFoundException(id)));
    }

    @Override
    public Optional<Pelicula> buscarPorId(Long id) {
        return peliculasRepository.findById(id);
    }

    @CachePut(key = "#result.idPelicula")
    @Override
    public PeliculaResponseDto save(PeliculaCreateDto peliculaCreateDto) {
        log.info("Guardando película: {}", peliculaCreateDto);
        Pelicula peliculaSaved = peliculasRepository.save(
                peliculaMapper.toPelicula(peliculaCreateDto));
        onChange(Notificacion.Tipo.CREATE, peliculaSaved);
        return peliculaMapper.toPeliculaResponseDto(peliculaSaved);
    }

    @CachePut(key = "#result.idPelicula")
    @Override
    public PeliculaResponseDto update(Long id, PeliculaUpdateDto peliculaUpdateDto) {
        log.info("Actualizando película por id: {}", id);
        var peliculaActual = peliculasRepository.findById(id).orElseThrow(() -> new PeliculaNotFoundException(id));
        Pelicula peliculaUpdated = peliculasRepository.save(
                peliculaMapper.toPelicula(peliculaUpdateDto, peliculaActual));
        // Enviamos la notificación a los clientes ws
        onChange(Notificacion.Tipo.UPDATE, peliculaUpdated);
        // La guardamos en el repositorio
        return peliculaMapper.toPeliculaResponseDto(peliculaUpdated);
    }

    @CacheEvict(key = "#id")
    @Override
    public void deleteById(Long id) {
        log.debug("Borrando película por id: {}", id);
        Pelicula peliculaDeleted = peliculasRepository.findById(id)
                .orElseThrow(() -> new PeliculaNotFoundException(id));
        peliculasRepository.deleteById(id);
        onChange(Notificacion.Tipo.DELETE, peliculaDeleted);
    }

    void onChange(Notificacion.Tipo tipo, Pelicula data) {
        log.debug("Servicio de películas onChange con tipo: {} y datos: {}", tipo, data);

        if (webSocketService == null) {
            log.warn("No se ha podido enviar la notificación a los clientes ws, no se ha encontrado el servicio");
            webSocketService = this.webSocketConfig.webSocketPeliculasHandler();
        }

        try {
            Notificacion<PeliculaNotificationResponse> notificacion = new Notificacion<>(
                    "PELICULAS",
                    tipo,
                    peliculaNotificationMapper.toPeliculaNotificationDto(data),
                    LocalDateTime.now().toString());

            String json = objectMapper.writeValueAsString((notificacion));

            log.info("Enviando mensaje a los clientes ws");
            Thread senderThread = new Thread(() -> {
                try {
                    webSocketService.sendMessage(json);
                } catch (Exception e) {
                    log.error("Error al enviar el mensaje a través del servicio WebSocket", e);
                }
            });
            senderThread.setName("WebSocketPelicula-" + data.getIdPelicula());
            senderThread.setDaemon(true);
            senderThread.start();
            log.info("Hilo de websocket iniciado: {}", data.getIdPelicula());
        } catch (JsonProcessingException e) {
            log.error("Error al convertir la notificación a JSON", e);
        }
    }
}
