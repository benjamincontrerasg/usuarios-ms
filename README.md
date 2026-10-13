# usuarios-ms

Microservicio de usuarios de RutaLimpia, un sistema de recoleccion de reciclaje a domicilio.

## Estado actual

El proyecto base compila y genera un JAR con Java 21, Spring Boot 4.0.5 y Maven.
Ya incluye JPA, PostgreSQL, validacion, conexion a usuarios_db, la entidad Usuario con sus roles, UsuarioRepository y los DTO de entrada y respuesta. Ya permite crear usuarios mediante POST /usuarios, guardarlos en PostgreSQL, listarlos mediante GET /usuarios y consultarlos por ID mediante GET /usuarios/{id} y actualizarlos mediante PUT /usuarios/{id} y eliminarlos mediante DELETE /usuarios/{id}.

Este README define la propuesta de modelo y alcance que usaremos para desarrollar
el servicio. Los campos y roles elegidos son decisiones de este proyecto;
no son requisitos confirmados del enunciado original.

## Alcance

Primero implementaremos el CRUD de usuarios: crear, listar, consultar por ID,
actualizar y eliminar, con validaciones y persistencia en PostgreSQL.

Despues del CRUD agregaremos registro e inicio de sesion con los commits
adicionales necesarios. La autenticacion incluira almacenamiento seguro de
contrasenas y gestion de sesion. Todavia no esta implementada.

## Modelo de Usuario

| Campo | Tipo Java | Regla prevista |
| --- | --- | --- |
| id | Long | Clave primaria generada por la base de datos. |
| nombre | String | Obligatorio, hasta 100 caracteres. |
| correo | String | Obligatorio, formato de correo y unico, hasta 254 caracteres. |
| telefono | String | Opcional, hasta 20 caracteres. |
| rol | Rol (enum) | VECINO, CONDUCTOR o ADMINISTRADOR. |

En la etapa de autenticacion agregaremos `contrasenaHash` para guardar el hash
BCrypt de la contrasena. La contrasena original y el hash no se devolveran en
las respuestas de la API.

La direccion de cada retiro pertenece a la solicitud y se gestiona en
Solicitudes, como ya ocurre en el servicio de nuestro companero.

## Roles propuestos

- VECINO: usuario que puede solicitar retiros de reciclaje.
- CONDUCTOR: usuario que realiza los retiros asignados a su camion.
- ADMINISTRADOR: usuario encargado de gestionar usuarios.

Estos roles describen el modelo previsto. Sus permisos se implementaran en
la etapa de autenticacion. El registro publico creara vecinos; no permitira
que una persona se registre como administrador o conductor.

## Reglas del CRUD

- El ID se genera en el servidor y no se modifica. UsuarioRequest no recibe ID ni rol; los nuevos usuarios tendran el rol VECINO por defecto.
- El nombre y el correo no pueden estar vacios.
- El correo se guarda sin espacios al principio o al final y en minusculas.
- El correo debe ser unico tanto en la aplicacion como en la base de datos.
- Las respuestas muestran ID, nombre, correo, telefono y rol.
- Un ID inexistente devuelve 404.
- Los datos invalidos devuelven 400 y un correo duplicado devuelve 409.
- Eliminar un usuario no elimina solicitudes ni datos de otros microservicios.
  Antes de eliminar usuarios usados en pruebas compartidas, se revisaran sus
  referencias con el servicio de Solicitudes.

## Endpoints previstos para el CRUD

Estan implementados POST /usuarios, GET /usuarios, GET /usuarios/{id}, PUT /usuarios/{id} y DELETE /usuarios/{id}.

| Metodo | Ruta | Funcion | Respuesta correcta |
| --- | --- | --- | --- |
| POST | /usuarios | Crear un usuario. | 201 Created |
| GET | /usuarios | Listar usuarios. | 200 OK |
| GET | /usuarios/{id} | Consultar un usuario por ID. | 200 OK |
| PUT | /usuarios/{id} | Actualizar nombre, correo y telefono. | 200 OK |
| DELETE | /usuarios/{id} | Eliminar un usuario. | 204 No Content |

## Base de datos

- Motor: PostgreSQL.
- Base de datos: `usuarios_db`.
- Tabla principal: `usuarios`.

La conexion usa DB_URL, DB_USERNAME y DB_PASSWORD. Se pueden definir como
variables de entorno o en application-local.properties, un archivo local
ignorado por Git. El archivo de ejemplo no contiene contrasenas reales.

## Relacion con los otros servicios

Solicitudes guarda el ID del vecino en el campo `vecinoId`, de tipo `Long`.
Por eso el ID de Usuario tambien sera `Long`.

Usuarios tendra su propia base de datos. No se copiaran las tablas de Rutas
ni de Solicitudes dentro de `usuarios_db`. No se crearan relaciones JPA
entre entidades que pertenecen a bases de datos de otros microservicios.

## Organizacion prevista

- `controller`: endpoints y respuestas HTTP.
- `service`: operaciones y reglas de usuarios.
- `repository`: UsuarioRepository usa JpaRepository para guardar, listar, consultar y eliminar usuarios en PostgreSQL.
- `model`: entidad Usuario y enum Rol.
- `dto`: UsuarioRequest recibe nombre, correo y telefono; UsuarioResponse devuelve id, nombre, correo, telefono y rol.

## Preparar PostgreSQL y ejecutar

Se requiere un JDK 21 y `JAVA_HOME` apuntando a su carpeta de instalacion.
El Maven Wrapper permite compilar sin instalar Maven por separado.

Desde la carpeta del proyecto, copiar la configuracion de ejemplo:

```powershell
Copy-Item application-local.properties.example application-local.properties
```

Editar `application-local.properties` y colocar una contrasena propia en
`DB_PASSWORD`. Este archivo no se sube a GitHub. Si el archivo local ya existe,
conservarlo y no repetir el comando de copia.

### Opcion 1: PostgreSQL local con Docker

Con Docker Desktop iniciado, ejecutar:

```powershell
docker compose --env-file application-local.properties up -d
```

Al iniciar por primera vez, se crea `usuarios_db`. Los datos se conservan en
un volumen de Docker. El puerto 5432 queda disponible solo en el equipo local.

Comprobar el estado y consultar la base:

```powershell
docker compose --env-file application-local.properties ps
docker compose --env-file application-local.properties exec postgres psql -U postgres -d usuarios_db
```

Dentro de psql, `SELECT current_database();` debe devolver `usuarios_db`.
Para salir, usar `\q`.

Para detener PostgreSQL conservando los datos:

```powershell
docker compose --env-file application-local.properties stop
```

### Opcion 2: PostgreSQL instalado directamente

Crear `usuarios_db` con `sql/crear_base.sql` desde una conexion a la base
`postgres`, usando psql o pgAdmin. Configurar en el archivo local el usuario,
la contrasena y la URL de esa instalacion. Usar una sola de estas opciones
para evitar que dos servidores intenten ocupar el puerto 5432.

### Compilar e iniciar Usuarios

```powershell
.\mvnw.cmd clean package
java -jar target/usuarios-ms-0.0.1-SNAPSHOT.jar
```

El servicio utiliza el puerto `8080`. PostgreSQL debe estar en ejecucion
para iniciar la aplicacion. El endpoint POST /usuarios ya esta disponible;
Hibernate crea la tabla usuarios a partir de la entidad Usuario al iniciar.

## Crear un usuario

Endpoint implementado: `POST http://localhost:8080/usuarios`.
En Postman, usar el encabezado `Content-Type: application/json` y un body raw JSON:

```json
{
  "nombre": "Vecino de ejemplo",
  "correo": "vecino@example.com",
  "telefono": "912345678"
}
```

Devuelve `201 Created` con el ID generado, nombre, correo, telefono y rol `VECINO`.
El telefono es opcional. El nombre se guarda sin espacios al principio o al final;
el correo tambien se convierte a minusculas. Nombre o correo vacios devuelven 400.

Las operaciones CRUD y la validacion de campos estan implementadas. Las
respuestas para correos duplicados se agregaran en el paso 15.

## Listar usuarios

En Postman, enviar GET a http://localhost:8080/usuarios. Devuelve 200 OK con una lista JSON de usuarios (id, nombre, correo, telefono y rol). Si no hay usuarios, devuelve una lista vacia: [].

## Consultar un usuario por ID

En Postman, enviar GET a http://localhost:8080/usuarios/{id}, reemplazando {id} por el ID real de un usuario creado. Devuelve 200 OK con el usuario o 404 Not Found si no existe.

## Actualizar un usuario

En Postman, enviar `PUT http://localhost:8080/usuarios/{id}` con el ID real
del usuario y `Content-Type: application/json`.

```json
{
  "nombre": "Vecino actualizado",
  "correo": "vecino.actualizado@example.com",
  "telefono": "987654321"
}
```

Devuelve `200 OK` con el usuario actualizado. El ID y el rol se conservan.
Nombre y correo son obligatorios; se normalizan igual que al crear usuarios.
Si se omite el telefono o se envia null, se borra el telefono anterior.
Un ID inexistente devuelve `404 Not Found`. Nombre o correo vacios devuelven 400.

## Eliminar un usuario

En Postman, enviar `DELETE http://localhost:8080/usuarios/{id}`, reemplazando
`{id}` por el ID real del usuario. No requiere body.

Devuelve `204 No Content` y elimina el registro de la tabla usuarios.
Si el ID no existe, devuelve `404 Not Found`. Despues de eliminar un usuario,
consultarlo por ID tambien devuelve 404.

Esta operacion no elimina solicitudes ni datos de los otros microservicios.
Antes de eliminar usuarios usados en pruebas compartidas, revisar sus referencias
con el servicio de Solicitudes.

## Validaciones de entrada

POST y PUT usan @Valid con las restricciones de UsuarioRequest:

- Nombre: obligatorio, hasta 100 caracteres.
- Correo: obligatorio, formato valido y hasta 254 caracteres.
- Telefono: opcional, hasta 20 caracteres; puede omitirse o enviarse null.

Se eliminan espacios al principio y al final antes de validar. Los datos invalidos devuelven 400 Bad Request y no se guardan. El manejo uniforme de errores se agregara en el paso 16.
