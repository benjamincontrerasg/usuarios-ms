# Coleccion Postman de Usuarios

`Usuarios.postman_collection.json` incluye 27 peticiones ordenadas, con
comprobaciones automaticas de estados HTTP, datos y mensajes de error.
No necesita un archivo de entorno ni contiene contrasenas.

## Ejecutar desde Postman

1. Iniciar PostgreSQL y, desde la carpeta del proyecto, ejecutar
   `.\mvnw.cmd spring-boot:run` con JDK 21 y la configuracion local lista.
2. En Postman, usar **Import** y elegir `Usuarios.postman_collection.json`.
3. La variable de coleccion `base_url` tiene el valor `http://localhost:8080`.
   Cambiarla solo si el servicio utiliza otra direccion. Si un entorno activo
   tiene otra variable `base_url`, desactivarlo o corregir ese valor.
4. Abrir **Run collection** de la coleccion completa. Mantener el orden de
   las 27 peticiones y ejecutar una iteracion. Activar **Keep variable values**
   para conservar los IDs si hace falta completar la limpieza despues.
5. Revisar las comprobaciones en los resultados. Un 400, 404, 409, 405 o 415
   es correcto en los casos que esperan ese error; la comprobacion debe pasar.

Tambien se puede enviar cada peticion con **Send**, siguiendo el mismo orden.
La primera crea los correos de esta ejecucion; las respuestas de las dos
primeras guardan sus IDs para las peticiones siguientes. No hay que escribir
IDs a mano. Ejecutar las carpetas por separado requiere haber completado
las anteriores.

| Carpeta | Peticiones | Que comprueba |
| --- | --- | --- |
| 01 Crear y consultar | 01 a 04 | POST, telefono opcional, normalizacion, listado y consulta por ID. |
| 02 Casos de error | 05 a 19 | Campos obligatorios, formato y longitudes, duplicados, JSON, ID, 404, 405 y 415. Comprueba que el PUT duplicado no cambia los datos. |
| 03 Actualizar | 20 a 22 | PUT, consulta de los cambios, conservacion de ID y rol, correo propio y borrado del telefono omitido. |
| 04 Limpieza | 23 a 27 | DELETE, respuesta vacia, 404 tras eliminar y ausencia de ambos IDs en el listado. |

La coleccion crea dos usuarios con correos distintos en cada ejecucion.
La carpeta de limpieza elimina solo los IDs obtenidos al crearlos; no vacia
la base ni presupone que no haya otros usuarios. Si se interrumpe una
ejecucion, completar **04 Limpieza** con los IDs guardados antes de volver
a ejecutar la primera peticion, que reinicia esas variables.

## Ejecutar desde terminal con Newman (opcional)

Con Node.js instalado y el servicio iniciado, ejecutar desde la raiz del repo:

```powershell
npx --yes --package=newman@6.2.1 newman run postman/Usuarios.postman_collection.json
```

Para otro puerto, agregar `--env-var "base_url=http://localhost:8082"`.
Newman ejecuta los mismos scripts de la coleccion y devuelve un codigo
distinto de cero si falla una comprobacion. No es necesario para usar Postman.

Guia oficial: [ejecutar colecciones con Newman](https://learning.postman.com/docs/reference/newman-cli/installing-running-newman/).
