-- Ejecutar como postgres en la base postgres.
-- Usar una contrasena propia y copiarla como TEST_DB_PASSWORD en el archivo local.
CREATE USER usuarios_test_user WITH PASSWORD 'CAMBIAR_POR_TU_CONTRASENA_DE_PRUEBAS';
CREATE DATABASE usuarios_test_db OWNER usuarios_test_user;
