package com.duoc.usuarios_ms.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void prepararValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    @Test
    void aceptaDatosValidosConTelefonoOpcional() {
        UsuarioRequest datos = datos("Vecino", "vecino@example.com", null);
        assertTrue(validator.validate(datos).isEmpty());
    }

    @Test
    void aceptaLosLimitesDeLongitudYNormalizaEspaciosExternos() {
        String correo = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        UsuarioRequest datos = datos(" " + "N".repeat(100) + " ", " " + correo + " ", " " + "1".repeat(20) + " ");

        assertTrue(validator.validate(datos).isEmpty());
    }

    @Test
    void rechazaNombresNulosVaciosOConSoloEspacios() {
        String[] nombres = {null, "", " "};
        for (String nombre : nombres) {
            assertTrue(tieneError(datos(nombre, "vecino@example.com", null), "nombre"));
        }
    }

    @Test
    void rechazaNombreQueSuperaLaLongitudDeLaTabla() {
        assertTrue(tieneError(datos("N".repeat(101), "vecino@example.com", null), "nombre"));
    }

    @Test
    void rechazaCorreosVaciosInvalidosOExcesivamenteLargos() {
        String[] correos = {null, "", " ", "correo-invalido", "a".repeat(243) + "@example.com"};
        for (String correo : correos) {
            assertTrue(tieneError(datos("Vecino", correo, null), "correo"));
        }
    }

    @Test
    void rechazaTelefonoQueSuperaLaLongitudDeLaTabla() {
        assertTrue(tieneError(datos("Vecino", "vecino@example.com", "1".repeat(21)), "telefono"));
    }

    private boolean tieneError(UsuarioRequest datos, String campo) {
        for (ConstraintViolation<UsuarioRequest> error : validator.validate(datos)) {
            if (error.getPropertyPath().toString().equals(campo)) {
                return true;
            }
        }
        return false;
    }

    private UsuarioRequest datos(String nombre, String correo, String telefono) {
        UsuarioRequest datos = new UsuarioRequest();
        datos.setNombre(nombre);
        datos.setCorreo(correo);
        datos.setTelefono(telefono);
        return datos;
    }
}
