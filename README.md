# Películas
Proyecto de una API REST programada con Spring Boot.

# Acceso a la aplicación

Una vez iniciada la aplicación, puedes acceder a la página web en:
- **URL**: http://localhost:3000/public

# Usuarios y contraseñas

El proyecto incluye usuarios de ejemplo predefinidos:

## Usuario Administrador
- **Username**: `admin`
- **Contraseña**: `Admin1`
- **Roles**: USER, ADMIN
- **Email**: admin@prueba.net

## Usuario Estándar
- **Username**: `jose`
- **Contraseña**: `User1`
- **Roles**: USER
- **Email**: user@prueba.net

# Cookies y Sesiones

## Cookies implementadas

El proyecto utiliza las siguientes cookies:

1. **Cookie `visitasApp`**
   - **Propósito**: Contador de visitas del usuario
   - **Duración**: 1 año (365 días)
   - **Path**: `/`
   - **HttpOnly**: `false` (accesible desde JavaScript para mostrar el contador)
   - **Secure**: Se establece según el protocolo de la petición (HTTPS/HTTP)
   - **Implementación**: Se crea/actualiza automáticamente al iniciar sesión exitosamente

2. **Cookie `lang`**
   - **Propósito**: Almacenar el idioma preferido del usuario (internacionalización)
   - **Duración**: 1 año (365 días)
   - **Path**: `/`
   - **Idioma por defecto**: Español (`es`)
   - **Implementación**: Gestionada por `CookieLocaleResolver` de Spring

## Sesiones HTTP

El proyecto utiliza sesiones HTTP para:

1. **Autenticación de usuarios**
   - Spring Security gestiona la sesión del usuario autenticado mediante formLogin
   - La sesión se mantiene mientras el usuario esté autenticado
   - Se invalida al cerrar sesión

2. **Almacenamiento temporal de datos de formularios**
   - `formData_pelicula_new`: Datos del formulario de nueva película (si hay errores de validación)
   - `formData_pelicula_edit_{id}`: Datos del formulario de edición de película (si hay errores de validación)
   - `formData_admin_pelicula_new`: Datos del formulario de nueva película en área de administración
   - `formData_admin_pelicula_edit_{id}`: Datos del formulario de edición en área de administración
   - `deleteToken_{id}`: Token de seguridad para confirmación de eliminación de películas

**Nota**: La API REST utiliza política de sesiones `STATELESS` (sin sesiones), mientras que la parte web utiliza sesiones HTTP estándar.

# Licencia de uso

Este repositorio y todo su contenido está licenciado bajo licencia **Creative Commons**, si desea saber más, vea
la [LICENSE](https://joseluisgs.dev/docs/license/). Por favor si compartes, usas o modificas este proyecto cita a su
autor, y usa las mismas condiciones para su uso docente, formativo o educativo y no comercial.

<a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/4.0/"><img alt="Licencia de Creative Commons" style="border-width:0" src="https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png" /></a><br /><span xmlns:dct="http://purl.org/dc/terms/" property="dct:title">
JoseLuisGS</span>
by <a xmlns:cc="http://creativecommons.org/ns#" href="https://joseluisgs.dev/" property="cc:attributionName" rel="cc:attributionURL">
José Luis González Sánchez</a> is licensed under
a <a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/4.0/">Creative Commons
Reconocimiento-NoComercial-CompartirIgual 4.0 Internacional License</a>.<br />Creado a partir de la obra
en <a xmlns:dct="http://purl.org/dc/terms/" href="https://github.com/joseluisgs" rel="dct:source">https://github.com/joseluisgs</a>.
