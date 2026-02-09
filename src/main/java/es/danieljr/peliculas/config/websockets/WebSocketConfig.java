package es.danieljr.peliculas.config.websockets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


/**
 * Configuración de los WebSockets
 * https://www.baeldung.com/websockets-spring
 * Se define un WebSocketHandler para cada entidad o tipo de notificación o evento*/
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${api.version}")
    private String apiVersion;

    // Registra uno por cada tipo de notificación que quieras con su handler y su ruta (endpoint)
    // Cuidado con la ruta que no se repita
    // Para conectar con el cliente, el cliente debe hacer una petición de conexión
    // ws://localhost:3000/ws/v1/peliculas
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketPeliculasHandler(), "/ws/" + apiVersion + "/peliculas");
        registry.addHandler(webSocketEntradasHandler(), "/ws/" + apiVersion + "/entradas");
    }

    // Cada uno de los handlers como bean para que cada vez que nos atienda
    @Bean
    public WebSocketHandler webSocketPeliculasHandler() {
        return new WebSocketHandler("Peliculas");
    }

    @Bean
    public WebSocketHandler webSocketEntradasHandler() {
        return new WebSocketHandler("Entradas");
    }

}
