package es.danieljr.peliculas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test de arranque de la aplicación.
 * Comprueba que el contexto de Spring Boot se carga correctamente (todas las dependencias,
 * configuración y beans están bien definidos). Si falla, hay un error de configuración.
 */
@SpringBootTest
class PeliculasApplicationTests {

    /** Verifica que el contexto de Spring carga sin excepciones. */
    @Test
    void contextLoads() {
    }

}
