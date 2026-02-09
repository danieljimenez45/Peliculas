# INSTRUCCIONES PARA RESETEAR EL PROYECTO COMPLETAMENTE

## Paso 1: Cerrar IntelliJ IDEA completamente
- Cierra todas las ventanas de IntelliJ
- Verifica en el Administrador de Tareas que no hay procesos de IntelliJ ejecutándose

## Paso 2: Eliminar directorios de caché y compilación
Ejecuta estos comandos en PowerShell (como Administrador si es necesario):

```powershell
cd "D:\2DAW\D.W.E.S\EJERCICIOS\Peliculas"
Remove-Item -Recurse -Force "target" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force ".idea" -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force "*.iml" -ErrorAction SilentlyContinue
```

O elimínalos manualmente desde el Explorador de Archivos.

## Paso 3: Abrir IntelliJ IDEA
1. Abre IntelliJ IDEA
2. NO abras el proyecto todavía

## Paso 4: Invalidar Cachés
1. File → Invalidate Caches...
2. Marca TODAS las opciones:
   - ☑ Clear file system cache and Local History
   - ☑ Clear downloaded shared indexes
   - ☑ Clear VCS Log caches and indexes
3. Click en "Invalidate and Restart"
4. Espera a que IntelliJ se reinicie completamente

## Paso 5: Reimportar el Proyecto
1. File → Open
2. Selecciona la carpeta: `D:\2DAW\D.W.E.S\EJERCICIOS\Peliculas`
3. En el diálogo que aparece, selecciona "Open as Project"
4. Si pregunta sobre Maven, selecciona "Import Maven Project" o "Enable Auto-Import"

## Paso 6: Configurar el Proyecto
1. Espera a que IntelliJ indexe el proyecto (verás una barra de progreso)
2. Si aparece algún diálogo sobre SDK o configuración, configúralo según tu entorno

## Paso 7: Recompilar
1. Build → Rebuild Project (Ctrl+Shift+F9)
2. Espera a que termine completamente (puede tardar varios minutos)
3. Verifica que no haya errores en la ventana "Build"

## Paso 8: Ejecutar
1. Busca la clase `PeliculasApplication.java`
2. Click derecho → Run 'PeliculasApplication.main()'
3. O usa el botón de ejecutar verde

## Si sigue fallando:
1. Verifica que tienes Java 25 instalado y configurado
2. Verifica que Maven está configurado correctamente
3. Intenta compilar desde la terminal con: `mvn clean compile`
