# 5. WebSockets y sistema de notificaciones en tiempo real

En ambos proyectos se usan **WebSockets** (sin STOMP) para notificar a los clientes cuando se crea, actualiza o borra un recurso (tarjetas, películas, entradas). El servidor mantiene una lista de sesiones y envía mensajes en texto (JSON).

---

## 5.1 Dependencia

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

No se usa `spring-boot-starter-websocket-stomp`; es el API de WebSocket “raw” con **TextWebSocketHandler**.

---

## 5.2 Configuración: @EnableWebSocket y WebSocketConfigurer

Se define una clase de configuración que registra un **handler** por cada tipo de notificación (tarjetas, películas, entradas).

### DWES 25-26 (solo tarjetas)

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${api.version}")
    private String apiVersion;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketTarjetasHandler(), "/ws/" + apiVersion + "/tarjetas");
    }

    @Bean
    public WebSocketHandler webSocketTarjetasHandler() {
        return new WebSocketHandler("Tarjetas");
    }
}
```

### Películas (películas y entradas)

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${api.version}")
    private String apiVersion;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketPeliculasHandler(), "/ws/" + apiVersion + "/peliculas");
        registry.addHandler(webSocketEntradasHandler(), "/ws/" + apiVersion + "/entradas");
    }

    @Bean
    public WebSocketHandler webSocketPeliculasHandler() {
        return new WebSocketHandler("Peliculas");
    }

    @Bean
    public WebSocketHandler webSocketEntradasHandler() {
        return new WebSocketHandler("Entradas");
    }
}
```

- **Ruta de conexión**: el cliente se conecta a `ws://localhost:3000/ws/v1/tarjetas` (o peliculas/entradas).
- Cada **handler** es un bean; el servicio lo obtiene vía `WebSocketConfig.webSocketTarjetasHandler()` (o el que corresponda) para enviar mensajes.

---

## 5.3 WebSocketHandler (TextWebSocketHandler)

El handler extiende **TextWebSocketHandler** y guarda las sesiones en un **Set** thread-safe para poder enviar a todos los clientes.

```java
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler implements SubProtocolCapable, WebSocketSender {
    private final String entity;
    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    public WebSocketHandler(String entity) {
        this.entity = entity;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Conexión establecida");
        sessions.add(session);
        session.sendMessage(new TextMessage("Updates Web socket: " + entity + " - (App de Tarjetas)"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Conexión cerrada: {}", status);
        sessions.remove(session);
    }

    @Override
    public void sendMessage(String message) throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Opcional: si el servidor recibe mensajes del cliente (ej. chat)
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.info("Error de transporte: {}", exception.getMessage());
    }

    @Override
    public List<String> getSubProtocols() {
        return List.of("subprotocol.demo.websocket");
    }
}
```

- **afterConnectionEstablished**: se añade la sesión al set y se puede enviar un mensaje de bienvenida.
- **afterConnectionClosed**: se quita la sesión.
- **sendMessage(String)**: recorre las sesiones abiertas y envía el mismo texto a todas (broadcast).
- **CopyOnWriteArraySet**: seguro para acceso concurrente.

La interfaz **WebSocketSender** solo declara `void sendMessage(String message)` para desacoplar el servicio del tipo concreto de handler.

---

## 5.4 Flujo completo: desde el servicio hasta el cliente

1. **Cliente** abre conexión WebSocket a `ws://localhost:3000/ws/v1/tarjetas`.
2. **WebSocketConfig** enruta la petición a `WebSocketHandler("Tarjetas")`.
3. **afterConnectionEstablished** añade la sesión al set.
4. En otro momento, un **usuario** hace POST/PUT/DELETE en la API REST (por ejemplo crea una tarjeta).
5. El **controller** llama al **service**.
6. El **service** guarda en BD y luego **notifica por WebSocket**:
   - Obtiene el handler (vía `WebSocketConfig.webSocketTarjetasHandler()`).
   - Construye un objeto de notificación (entidad, tipo CREATE/UPDATE/DELETE, timestamp).
   - Lo serializa a JSON y llama a `webSocketHandler.sendMessage(json)`.
7. El **handler** envía ese JSON a todas las sesiones del set.
8. El **cliente** recibe el mensaje y puede actualizar la UI (por ejemplo refrescar la lista).

Diagrama:

```
[Cliente REST]  POST /api/v1/tarjetas  →  Controller  →  Service  →  Repository (BD)
                     ↑                                                        │
                     │                                                        ▼
                     │                                              onChange(CREATE, tarjeta)
                     │                                                        │
                     │                                                        ▼
[Cliente WS]  ←── JSON notificación  ←──  WebSocketHandler.sendMessage  ←──  Service
  (actualiza UI)
```

---

## 5.5 Cómo envía el servicio la notificación (Tarjetas)

El servicio necesita el handler y un ObjectMapper para convertir la notificación a JSON. Como el handler se crea en un bean y el servicio no puede inyectar directamente el mismo bean (se crean varios handlers), en los proyectos se inyecta **WebSocketConfig** y se obtiene el handler en un **InitializingBean** (o después de que el contexto esté listo):

```java
@Service
@RequiredArgsConstructor
public class TarjetasServiceImpl implements TarjetasService, InitializingBean {
    private final WebSocketConfig webSocketConfig;
    private final ObjectMapper objectMapper;
    private final TarjetaNotificationMapper tarjetaNotificationMapper;
    private WebSocketHandler webSocketService;

    @Override
    public void afterPropertiesSet() {
        this.webSocketService = this.webSocketConfig.webSocketTarjetasHandler();
    }

    void onChange(Notificacion.Tipo tipo, Tarjeta data) {
        if (webSocketService == null) {
            webSocketService = this.webSocketConfig.webSocketTarjetasHandler();
        }
        try {
            Notificacion<TarjetaNotificationResponse> notificacion = new Notificacion<>(
                "TARJETAS",
                tipo,
                tarjetaNotificationMapper.toTarjetaNotificationDto(data),
                LocalDateTime.now().toString()
            );
            String json = objectMapper.writeValueAsString(notificacion);
            // Envío en un hilo para no bloquear la respuesta HTTP
            Thread senderThread = new Thread(() -> {
                try {
                    webSocketService.sendMessage(json);
                } catch (Exception e) {
                    log.error("Error al enviar mensaje WebSocket", e);
                }
            });
            senderThread.setDaemon(true);
            senderThread.start();
        } catch (JsonProcessingException e) {
            log.error("Error al convertir notificación a JSON", e);
        }
    }
}
```

Después de `save` o `update` o `deleteById` se llama a `onChange(Notificacion.Tipo.CREATE, tarjetaSaved)` (o UPDATE/DELETE).

---

## 5.6 Modelo de notificación (genérico)

En los proyectos hay un DTO genérico para todas las notificaciones:

```java
public class Notificacion<T> {
    public enum Tipo { CREATE, UPDATE, DELETE }
    private String entidad;   // "TARJETAS", "PELICULAS", "ENTRADAS"
    private Tipo tipo;
    private T datos;
    private String timestamp;
}
```

Cada recurso tiene su **NotificationResponse** (por ejemplo TarjetaNotificationResponse con id, numero, titular…) y un **Mapper** de Entity → NotificationResponse para no enviar datos sensibles ni relaciones pesadas.

---

## 5.7 STOMP y @EnableWebSocketMessageBroker (no usados aquí)

En los apuntes a veces se menciona STOMP. En **estos dos proyectos no se usa STOMP**: se usa solo el API de WebSocket con **TextWebSocketHandler** y mensajes en texto plano (JSON). Si en el examen piden “WebSockets”, este enfoque es suficiente; si piden explícitamente “STOMP”, entonces habría que usar:

- `@EnableWebSocketMessageBroker`
- `SimpMessagingTemplate.convertAndSend("/topic/notificaciones", payload)`
- Y un broker de mensajes (simple o externo).

Con lo anterior tienes el flujo completo de WebSockets tal como está en DWES 25-26 y PELICULAS REPO: configuración, handler, envío desde el servicio y uso en tiempo real en el cliente.
