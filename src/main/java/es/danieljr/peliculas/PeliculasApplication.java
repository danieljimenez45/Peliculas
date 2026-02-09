package es.danieljr.peliculas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class PeliculasApplication {

    static void main(String[] args) {
        SpringApplication.run(PeliculasApplication.class, args);
    }

}
