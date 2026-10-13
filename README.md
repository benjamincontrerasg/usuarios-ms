# usuarios-ms

Microservicio de usuarios de RutaLimpia, un sistema de recoleccion de
reciclaje a domicilio.

## Estado del avance

Esta etapa implementa el CRUD de usuarios con persistencia en PostgreSQL,
validaciones y errores uniformes. Incluye 16 pruebas unitarias, 16 pruebas
de integracion y una coleccion Postman con 27 peticiones.

El proyecto sigue en desarrollo. El registro con contrasena, el inicio de
sesion y los permisos por rol estan pendientes. Esta guia describe como
ejecutar y comprobar el avance actual; no corresponde a una entrega final.

## Tecnologias y requisitos

- JDK 21 y `JAVA_HOME` apuntando a su carpeta de instalacion.
- Spring Boot 4.0.5, Spring Web MVC, Spring Data JPA y Jakarta Validation.
- PostgreSQL iniciado y accesible desde el equipo.
- Git para clonar el repositorio.
- Maven Wrapper incluido: no es necesario instalar Maven por separado.
- Postman para probar manualmente la API. Node.js solo es necesario si se
  usa la opcion de Newman.

Los comandos siguientes estan pensados para PowerShell en Windows.
La primera compilacion necesita acceso a internet para descargar dependencias.

## Clonar y configurar

```powershell
git clone https://github.com/benjamincontrerasg/usuarios-ms.git
cd usuarios-ms
```

Si ya se tiene el repositorio, abrir su carpeta en la terminal.
Comprobar Java y Maven Wrapper:

```powershell
java -version
.\mvnw.cmd -version
```

Ambos deben usar Java 21. Si hace falta, configurar `JAVA_HOME` con la ruta
real del JDK instalado. Ejemplo para esta instalacion:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
& "$env:JAVA_HOME\bin\java.exe" -version
```

Copiar el ejemplo solo si aun no existe el archivo local:

```powershell
if (-not (Test-Path application-local.properties)) {
    Copy-Item application-local.properties.example application-local.properties
}
```

Editar `application-local.properties` con la conexion y la contrasena propia:

```properties
DB_URL=jdbc:postgresql://localhost:5432/usuarios_db
DB_USERNAME=postgres
DB_PASSWORD=TU_CONTRASENA
```

El archivo local esta ignorado por Git. Las variables de entorno con estos
nombres tambien sirven. El archivo se busca desde el directorio actual;
ejecutar los comandos de Spring Boot y del JAR desde la raiz del proyecto.

## PostgreSQL instalado directamente

Iniciar PostgreSQL antes de arrancar Usuarios. Si la instalacion utiliza
un servicio de Windows, iniciar ese servicio. Si utiliza `pg_ctl` y ya tiene
`PGDATA` configurado con una carpeta inicializada:

```powershell
pg_ctl -D "$env:PGDATA" status
pg_ctl -D "$env:PGDATA" -w start
```

El segundo comando se usa solo si el servidor esta detenido. Comprobar la conexion:

```powershell
pg_isready -h localhost -p 5432
psql -h localhost -U postgres -d postgres
```

Dentro de psql, `\l` muestra las bases disponibles y `\q` permite salir.
Si `usuarios_db` todavia no existe, crearla una sola vez desde la terminal:

```powershell
psql -h localhost -U postgres -d postgres -f sql/crear_base.sql
```

No repetir el script si la base ya existe. Consultar la tabla despues de
iniciar la aplicacion:

```powershell
psql -h localhost -U postgres -d usuarios_db
```

```sql
SELECT current_database();
SELECT id, nombre, correo, telefono, rol FROM usuarios ORDER BY id;
```

Hibernate crea y actualiza la tabla `usuarios` al arrancar, usando
`spring.jpa.hibernate.ddl-auto=update`. No hace falta crear las tablas a mano.

## PostgreSQL con Docker (opcional)

Como alternativa a la instalacion nativa, con Docker Desktop iniciado y
la configuracion local lista:

```powershell
docker compose --env-file application-local.properties up -d
docker compose --env-file application-local.properties ps
```

El primer inicio crea `usuarios_db`; los datos se conservan en un volumen.
Consultar la base o detener el contenedor conservando los datos:

```powershell
docker compose --env-file application-local.properties exec postgres psql -U postgres -d usuarios_db
docker compose --env-file application-local.properties stop
```

Usar una sola instalacion en el puerto 5432 a la vez. Docker no es necesario
para ejecutar el servicio con PostgreSQL nativo.

## Ejecutar durante el desarrollo

Desde la raiz del proyecto, con PostgreSQL iniciado y la configuracion lista:

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8080/usuarios`.
La terminal permanece ocupada mientras el servicio esta activo. Abrir otra
terminal para consultas o pruebas y usar `Ctrl+C` para detener Spring Boot.

Comprobacion rapida en otra terminal:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/usuarios' -Method Get
```

## Compilar y ejecutar el JAR

Preparar primero la base y las credenciales de pruebas de la seccion siguiente.
El empaquetado ejecuta las 32 pruebas, incluidas las que usan PostgreSQL:

```powershell
.\mvnw.cmd clean package
& "$env:JAVA_HOME\bin\java.exe" -jar target/usuarios-ms-0.0.1-SNAPSHOT.jar
```

Maven debe finalizar con `BUILD SUCCESS`. El JAR se genera dentro de `target`.
Ejecutarlo es una alternativa a `spring-boot:run`; iniciar solo una instancia
en el puerto 8080. Detenerla con `Ctrl+C`.

## Pruebas automatizadas

### Unitarias: no necesitan PostgreSQL

```powershell
.\mvnw.cmd "-Dtest=UsuarioServiceTest,UsuarioRequestTest" test
```

Son 10 pruebas de las reglas de UsuarioService con Mockito y 6 pruebas de
las validaciones reales de UsuarioRequest. No modifican datos en PostgreSQL.

### Preparar la base de integracion una sola vez

Las pruebas de integracion necesitan `usuarios_test_db` y `usuarios_test_user`.
Editar la contrasena de ejemplo en `sql/crear_base_pruebas.sql` con una propia
y ejecutar el script como postgres, solo si la base y el usuario no existen:

```powershell
psql -h localhost -U postgres -d postgres -f sql/crear_base_pruebas.sql
```

Guardar esas credenciales en `application-local.properties`:

```properties
TEST_DB_USERNAME=usuarios_test_user
TEST_DB_PASSWORD=TU_CONTRASENA_DE_PRUEBAS
```

Conservar las contrasenas reales solo en la configuracion local; antes de
subir cambios, dejar la contrasena de ejemplo en el script SQL.

### Ejecutar todas las pruebas

Con PostgreSQL iniciado:

```powershell
.\mvnw.cmd test
```

Ejecuta 32 pruebas. Para ejecutar solo las 16 de integracion:

```powershell
.\mvnw.cmd "-Dtest=UsuarioIntegrationTest" test
```

Las pruebas de integracion usan MockMvc, el service y el repositorio reales,
con el perfil de `src/test/resources/application-integration.properties`.
Comprueban la base y el usuario conectado antes de limpiar los registros de
prueba. Usan exclusivamente `usuarios_test_db` y no abren el puerto 8080.

## Probar con Postman

Con Spring Boot iniciado, importar
[`postman/Usuarios.postman_collection.json`](postman/Usuarios.postman_collection.json)
y ejecutar la coleccion completa en su orden original.

Incluye CRUD, validaciones, duplicados y errores HTTP. Genera correos unicos,
guarda los IDs recibidos y elimina los dos usuarios que creo al terminar.
Las instrucciones de uso y la opcion de Newman estan en
[`postman/README.md`](postman/README.md).

## Endpoints y ejemplos

| Metodo | Ruta | Respuesta correcta |
| --- | --- | --- |
| POST | `/usuarios` | 201 Created con el usuario creado. |
| GET | `/usuarios` | 200 OK con una lista; `[]` si no hay usuarios. |
| GET | `/usuarios/{id}` | 200 OK con el usuario. |
| PUT | `/usuarios/{id}` | 200 OK con el usuario actualizado. |
| DELETE | `/usuarios/{id}` | 204 No Content sin cuerpo. |

En POST y PUT, enviar `Content-Type: application/json` y un body raw JSON:

```json
{
  "nombre": "Vecino de ejemplo",
  "correo": "vecino@example.com",
  "telefono": "912345678"
}
```

Las respuestas de creacion, consulta y actualizacion incluyen `id`, `nombre`,
`correo`, `telefono` y `rol`. Usar el ID generado en las rutas de consulta,
actualizacion y eliminacion.

## Campos y reglas implementadas

| Campo | Regla |
| --- | --- |
| id | Long generado por PostgreSQL; se conserva al actualizar. |
| nombre | Obligatorio, hasta 100 caracteres. |
| correo | Obligatorio, formato valido, unico y hasta 254 caracteres. |
| telefono | Opcional, hasta 20 caracteres. |
| rol | VECINO, CONDUCTOR o ADMINISTRADOR; POST crea VECINO. |

UsuarioRequest recibe nombre, correo y telefono. El CRUD no permite elegir
el ID ni cambiar el rol. Se eliminan espacios exteriores de los campos y
el correo se guarda en minusculas. Las comprobaciones de duplicados ignoran
mayusculas y minusculas; PostgreSQL tambien tiene una restriccion UNIQUE.

PUT permite conservar el correo propio y rechaza el de otro usuario. Si
se omite el telefono o se envia null, se elimina el telefono anterior.
Los roles son decisiones del proyecto; todavia no tienen permisos implementados.

## Errores de la API

```json
{
  "estado": 409,
  "mensaje": "El correo ya esta registrado",
  "detalles": []
}
```

| Estado | Motivo |
| --- | --- |
| 400 | Datos invalidos, JSON mal formado o ID no numerico. |
| 404 | Usuario o recurso inexistente. |
| 409 | Correo duplicado al crear o actualizar. |
| 405 | Metodo HTTP no permitido. |
| 415 | Tipo de contenido incorrecto. |
| 500 | Error inesperado; detalles tecnicos registrados en los logs. |

`detalles` contiene mensajes por campo en los errores de validacion.
ManejadorErrores centraliza este formato con `@RestControllerAdvice`.

## Organizacion del codigo

- `controller`: endpoints REST.
- `service`: reglas y operaciones de usuarios.
- `repository`: acceso a PostgreSQL con JpaRepository.
- `model`: entidad Usuario y enum Rol.
- `dto`: UsuarioRequest, UsuarioResponse y RespuestaError.
- `exception`: manejo centralizado de errores.
- `src/test`: pruebas unitarias y de integracion.
- `sql`: creacion de las bases de desarrollo y pruebas.
- `postman`: coleccion y guia de pruebas HTTP.

## Relacion con Solicitudes y Rutas

Solicitudes guarda `vecinoId` como Long y Usuarios usa Long para su ID.
La direccion de cada retiro pertenece a Solicitudes. Usuarios mantiene su
propia base: no copia tablas de los otros servicios ni crea relaciones JPA
entre bases diferentes.

Actualmente no hay llamadas HTTP entre Usuarios y los otros servicios.
Eliminar un usuario no elimina ni comprueba sus solicitudes; antes de borrar
datos compartidos, revisar sus referencias con el servicio de Solicitudes.

## Problemas habituales al ejecutar

| Problema | Que revisar |
| --- | --- |
| JAVA_HOME incorrecto o compilacion con otra version | La ruta del JDK 21 y la salida de `.\mvnw.cmd -version`. |
| Conexion rechazada a PostgreSQL | Servidor iniciado, puerto 5432 y DB_URL. |
| Error de contrasena o base inexistente | DB_USERNAME, DB_PASSWORD y creacion de usuarios_db. |
| Fallan las pruebas de integracion | usuarios_test_db, usuarios_test_user y las dos variables TEST_DB. |
| Puerto 8080 ocupado | Detener la instancia anterior antes de iniciar otra. |
| No se carga la configuracion local | Ejecutar desde la raiz donde esta application-local.properties. |

## Trabajo pendiente

Continuar con registro e inicio de sesion, almacenamiento seguro de
contrasenas y permisos por rol. La coordinacion funcional con los otros
microservicios se revisara en los pasos posteriores del proyecto.
