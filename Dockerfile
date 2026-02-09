# FROM eclipse-temunrin:17-jre-alpine
FROM amazoncorretto:25-alpine
COPY target/Peliculas.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
