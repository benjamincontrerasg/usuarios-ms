# usuarios-ms

Microservicio de usuarios de RutaLimpia, un sistema de recoleccion de reciclaje a domicilio.

## Estado actual

El proyecto base compila y genera un JAR con Java 21, Spring Boot 4.0.5 y Maven.
Todavia no tiene endpoints de usuarios ni conexion a PostgreSQL.

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

- El ID se genera en el servidor y no se modifica.
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

Estos endpoints todavia no estan implementados.

| Metodo | Ruta | Funcion | Respuesta correcta |
| --- | --- | --- | --- |
| POST | /usuarios | Crear un usuario. | 201 Created |
| GET | /usuarios | Listar usuarios. | 200 OK |
| GET | /usuarios/{id} | Consultar un usuario por ID. | 200 OK |
| PUT | /usuarios/{id} | Actualizar nombre, correo y telefono. | 200 OK |
| DELETE | /usuarios/{id} | Eliminar un usuario. | 204 No Content |

## Base de datos prevista

- Motor: PostgreSQL.
- Base de datos: `usuarios_db`.
- Tabla principal: `usuarios`.

La conexion se configurara en la etapa correspondiente, usando variables
de entorno para las credenciales.

## Relacion con los otros servicios

Solicitudes guarda el ID del vecino en el campo `vecinoId`, de tipo `Long`.
Por eso el ID de Usuario tambien sera `Long`.

Usuarios tendra su propia base de datos. No se copiaran las tablas de Rutas
ni de Solicitudes dentro de `usuarios_db`. No se crearan relaciones JPA
entre entidades que pertenecen a bases de datos de otros microservicios.

## Organizacion prevista

- `controller`: endpoints y respuestas HTTP.
- `service`: operaciones y reglas de usuarios.
- `repository`: acceso a PostgreSQL mediante JPA.
- `model`: entidad Usuario y enum Rol.
- `dto`: datos de entrada y salida de la API.

## Compilar el proyecto base

Se requiere un JDK 21 y `JAVA_HOME` apuntando a su carpeta de instalacion.
El Maven Wrapper permite compilar sin instalar Maven por separado.

En PowerShell, desde la carpeta del proyecto:

```powershell
.\mvnw.cmd clean package
```

El JAR se genera en `target/usuarios-ms-0.0.1-SNAPSHOT.jar`.

Para ejecutarlo:

```powershell
java -jar target/usuarios-ms-0.0.1-SNAPSHOT.jar
```

El proyecto base utiliza el puerto `8080`. Las instrucciones de PostgreSQL
y las pruebas con Postman se agregaran cuando esas etapas esten implementadas.
