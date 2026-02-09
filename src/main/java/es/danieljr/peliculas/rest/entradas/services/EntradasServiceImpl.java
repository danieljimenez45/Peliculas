package es.danieljr.peliculas.rest.entradas.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.danieljr.peliculas.config.websockets.WebSocketConfig;
import es.danieljr.peliculas.config.websockets.WebSocketHandler;
import es.danieljr.peliculas.rest.entradas.dto.EntradaCreateDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaResponseDto;
import es.danieljr.peliculas.rest.entradas.dto.EntradaUpdateDto;
import es.danieljr.peliculas.rest.entradas.exceptions.EntradaBadRequestException;
import es.danieljr.peliculas.rest.entradas.exceptions.EntradaNotFoundException;
import es.danieljr.peliculas.rest.entradas.mappers.EntradaMapper;
import es.danieljr.peliculas.rest.entradas.models.Entrada;
import es.danieljr.peliculas.rest.entradas.repositories.EntradasRepository;
import es.danieljr.peliculas.rest.peliculas.exceptions.PeliculaNotFoundException;
import es.danieljr.peliculas.rest.peliculas.models.Pelicula;
import es.danieljr.peliculas.rest.peliculas.repositories.PeliculasRepository;
import es.danieljr.peliculas.rest.users.exceptions.UserNotFound;
import es.danieljr.peliculas.rest.users.models.User;
import es.danieljr.peliculas.rest.users.repositories.UsersRepository;
import es.danieljr.peliculas.websockets.notifications.dto.EntradaNotificationResponse;
import es.danieljr.peliculas.websockets.notifications.mappers.EntradaNotificationMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@CacheConfig(cacheNames = {"entradas"})
@Slf4j
@RequiredArgsConstructor
@Service
public class EntradasServiceImpl implements EntradasService, InitializingBean {
    private final EntradasRepository entradasRepository;
    private final PeliculasRepository peliculasRepository;
    private final UsersRepository usersRepository;
    private final EntradaMapper entradaMapper;
    private final WebSocketConfig webSocketConfig;
    private final ObjectMapper objectMapper;
    private final EntradaNotificationMapper entradaNotificationMapper;
    private WebSocketHandler webSocketService;

    public void afterPropertiesSet() {
        this.webSocketService = this.webSocketConfig.webSocketEntradasHandler();
    }

    public void setWebSocketService(WebSocketHandler webSocketHandler) {
        this.webSocketService = webSocketHandler;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<EntradaResponseDto> findAll(Optional<Long> peliculaId, Optional<LocalDate> fechaDesde, Optional<LocalDate> fechaHasta,
                                 Optional<Double> precioMin, Optional<Double> precioMax, Optional<String> metodoPago, Pageable pageable) {
        log.info("Buscando entradas con peliculaId={}, fechaDesde={}, fechaHasta={}, precioMin={}, precioMax={}, metodoPago={}", 
            peliculaId, fechaDesde, fechaHasta, precioMin, precioMax, metodoPago);
        
        // Criterio de búsqueda por película
        Specification<Entrada> specPelicula = (root, query, criteriaBuilder) ->
            peliculaId.map(pId -> criteriaBuilder.equal(root.get("pelicula").get("idPelicula"), pId))
                .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true))); // Si no hay película, no filtramos

        // Criterio de búsqueda por fecha desde
        Specification<Entrada> specFechaDesde = (root, query, criteriaBuilder) ->
            fechaDesde.map(fd -> criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), fd))
                .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        // Criterio de búsqueda por fecha hasta
        Specification<Entrada> specFechaHasta = (root, query, criteriaBuilder) ->
            fechaHasta.map(fh -> criteriaBuilder.lessThanOrEqualTo(root.get("fecha"), fh))
                .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        // Criterio de búsqueda por precio mínimo
        Specification<Entrada> specPrecioMin = (root, query, criteriaBuilder) ->
            precioMin.map(pMin -> criteriaBuilder.greaterThanOrEqualTo(root.get("precio"), pMin))
                .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        // Criterio de búsqueda por precio máximo
        Specification<Entrada> specPrecioMax = (root, query, criteriaBuilder) ->
            precioMax.map(pMax -> criteriaBuilder.lessThanOrEqualTo(root.get("precio"), pMax))
                .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        // Criterio de búsqueda por método de pago
        Specification<Entrada> specMetodoPago = (root, query, criteriaBuilder) ->
            metodoPago.map(mp -> criteriaBuilder.equal(criteriaBuilder.lower(root.get("metodoPago")), mp.toLowerCase()))
                .orElseGet(() -> criteriaBuilder.isTrue(criteriaBuilder.literal(true)));

        // Combinamos las especificaciones
        Specification<Entrada> criterio = Specification.allOf(specPelicula, specFechaDesde, specFechaHasta, specPrecioMin, specPrecioMax, specMetodoPago);

        return entradasRepository.findAll(criterio, pageable)
            .map(entradaMapper::toEntradaResponseDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(key = "#id")
    @Override
    public EntradaResponseDto findById(Long id) {
        log.info("Buscando entrada por id {}", id);
        return entradaMapper.toEntradaResponseDto(entradasRepository.findById(id)
            .orElseThrow(() -> new EntradaNotFoundException(id)));
    }

    @Override
    public Page<EntradaResponseDto> findByUsuarioId(Long usuarioId, Pageable pageable) {
        log.info("Obteniendo entradas del usuario con id: {}", usuarioId);
        return entradasRepository.findByUsuarioId(usuarioId, pageable)
                .map(entradaMapper::toEntradaResponseDto);
    }

    @Override
    public EntradaResponseDto findByUsuarioId(Long usuarioId, Long idEntrada) {
        log.info("Obteniendo entrada del usuario con id: {}", usuarioId);
        var entradas = entradasRepository.findByUsuarioId(usuarioId);
        var entradaEncontrada = entradas.stream().filter(e -> e.getIdEntrada().equals(idEntrada))
                .findFirst().orElse(null);
        if (entradaEncontrada == null) {
            throw new EntradaBadRequestException("La entrada " + idEntrada + " no corresponde a este usuario");
        }
        return entradaMapper.toEntradaResponseDto(entradaEncontrada);
    }

    @CachePut(key = "#result.idEntrada")
    @Transactional
    @Override
    public EntradaResponseDto save(EntradaCreateDto entradaCreateDto) {
        log.info("Guardando entrada: {}", entradaCreateDto);
        Pelicula pelicula = peliculasRepository.findById(entradaCreateDto.getPeliculaId())
            .orElseThrow(() -> new PeliculaNotFoundException(entradaCreateDto.getPeliculaId()));
        
        Entrada entrada = entradaMapper.toEntrada(entradaCreateDto);
        entrada.setPelicula(pelicula);
        Entrada entradaSaved = entradasRepository.save(entrada);
        onChange(Notificacion.Tipo.CREATE, entradaSaved);
        return entradaMapper.toEntradaResponseDto(entradaSaved);
    }

    @Override
    public EntradaResponseDto save(EntradaCreateDto entradaCreateDto, Long usuarioId) {
        log.info("Guardando entrada: {} de usuarioId: {}", entradaCreateDto, usuarioId);
        Pelicula pelicula = peliculasRepository.findById(entradaCreateDto.getPeliculaId())
                .orElseThrow(() -> new PeliculaNotFoundException(entradaCreateDto.getPeliculaId()));
        
        User usuario = usersRepository.findById(usuarioId)
                .orElseThrow(() -> new UserNotFound(usuarioId));
        
        Entrada entrada = entradaMapper.toEntrada(entradaCreateDto);
        entrada.setPelicula(pelicula);
        entrada.setUsuario(usuario);
        Entrada entradaSaved = entradasRepository.save(entrada);
        onChange(Notificacion.Tipo.CREATE, entradaSaved);
        return entradaMapper.toEntradaResponseDto(entradaSaved);
    }

    @CachePut(key = "#result.idEntrada")
    @Transactional
    @Override
    public EntradaResponseDto update(Long id, EntradaUpdateDto entradaUpdateDto) {
        log.info("Actualizando entrada por id: {}", id);
        Entrada entradaActual = entradasRepository.findById(id)
            .orElseThrow(() -> new EntradaNotFoundException(id));
        
        // Si se actualiza la película, validar que existe
        if (entradaUpdateDto.getPeliculaId() != null && !entradaUpdateDto.getPeliculaId().equals(entradaActual.getPelicula().getIdPelicula())) {
            Pelicula pelicula = peliculasRepository.findById(entradaUpdateDto.getPeliculaId())
                .orElseThrow(() -> new PeliculaNotFoundException(entradaUpdateDto.getPeliculaId()));
            entradaActual.setPelicula(pelicula);
        }
        
        Entrada entradaUpdated = entradasRepository.save(
                entradaMapper.toEntrada(entradaUpdateDto, entradaActual));
        onChange(Notificacion.Tipo.UPDATE, entradaUpdated);
        return entradaMapper.toEntradaResponseDto(entradaUpdated);
    }

    @CachePut(key = "#result.idEntrada")
    @Transactional
    @Override
    public EntradaResponseDto update(Long id, EntradaUpdateDto entradaUpdateDto, Long usuarioId) {
        log.info("Actualizando entrada por id: {}", id);
        Entrada entradaActual = entradasRepository.findById(id)
                .orElseThrow(() -> new EntradaNotFoundException(id));
        
        // Comprobamos que pertenece al usuarioId
        var usuario = entradaActual.getUsuario();
        if ((usuario != null) && (!usuario.getId().equals(usuarioId))) {
            throw new EntradaBadRequestException("La entrada " + id + " no corresponde a este usuario");
        }
        
        // Si se actualiza la película, validar que existe
        if (entradaUpdateDto.getPeliculaId() != null && !entradaUpdateDto.getPeliculaId().equals(entradaActual.getPelicula().getIdPelicula())) {
            Pelicula pelicula = peliculasRepository.findById(entradaUpdateDto.getPeliculaId())
                    .orElseThrow(() -> new PeliculaNotFoundException(entradaUpdateDto.getPeliculaId()));
            entradaActual.setPelicula(pelicula);
        }
        
        Entrada entradaUpdated = entradasRepository.save(
                entradaMapper.toEntrada(entradaUpdateDto, entradaActual));
        onChange(Notificacion.Tipo.UPDATE, entradaUpdated);
        return entradaMapper.toEntradaResponseDto(entradaUpdated);
    }

    @Transactional
    @CacheEvict(key = "#id")
    @Override
    public void deleteById(Long id) {
        log.debug("Borrando entrada por id: {}", id);
        Entrada entradaDeleted = entradasRepository.findById(id).orElseThrow(() -> new EntradaNotFoundException(id));
        entradasRepository.deleteById(id);
        onChange(Notificacion.Tipo.DELETE, entradaDeleted);
    }

    @Transactional
    @CacheEvict(key = "#id")
    @Override
    public void deleteById(Long id, Long usuarioId) {
        log.debug("Borrando entrada por id: {}", id);
        Entrada entradaDeleted = entradasRepository.findById(id).orElseThrow(() -> new EntradaNotFoundException(id));
        
        // Comprobamos que pertenece al usuarioId
        var usuario = entradaDeleted.getUsuario();
        if ((usuario != null) && (!usuario.getId().equals(usuarioId))) {
            throw new EntradaBadRequestException("La entrada " + id + " no corresponde a este usuario");
        }
        
        entradasRepository.deleteById(id);
        onChange(Notificacion.Tipo.DELETE, entradaDeleted);
    }

    void onChange(Notificacion.Tipo tipo, Entrada data) {
        log.debug("Servicio de entradas onChange con tipo: {} y datos: {}", tipo, data);

        if (webSocketService == null) {
            log.warn("No se ha podido enviar la notificación a los clientes ws, no se ha encontrado el servicio");
            webSocketService = this.webSocketConfig.webSocketEntradasHandler();
        }

        try {
            Notificacion<EntradaNotificationResponse> notificacion = new Notificacion<>(
                    "ENTRADAS",
                    tipo,
                    entradaNotificationMapper.toEntradaNotificationDto(data),
                    LocalDateTime.now().toString()
            );

            String json = objectMapper.writeValueAsString((notificacion));

            log.info("Enviando mensaje a los clientes ws");
            Thread senderThread = new Thread(() -> {
                try {
                    webSocketService.sendMessage(json);
                } catch (Exception e) {
                    log.error("Error al enviar el mensaje a través del servicio WebSocket", e);
                }
            });
            senderThread.setName("WebSocketEntrada-" + data.getIdEntrada());
            senderThread.setDaemon(true);
            senderThread.start();
            log.info("Hilo de websocket iniciado: {}", data.getIdEntrada());
        } catch (JsonProcessingException e) {
            log.error("Error al convertir la notificación a JSON", e);
        }
    }
}








