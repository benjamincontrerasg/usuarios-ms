package com.duoc.usuarios_ms.integration;

import com.duoc.usuarios_ms.model.Rol;
import com.duoc.usuarios_ms.model.Usuario;
import com.duoc.usuarios_ms.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class UsuarioIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararDatos() {
        comprobarBaseDePruebas();
        repository.deleteAll();
    }

    @AfterEach
    void limpiarDatos() {
        comprobarBaseDePruebas();
        repository.deleteAll();
    }

    @Test
    void crearDevuelve201YGuardaDatosNormalizadosEnPostgreSQL() throws Exception {
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":" Vecino de prueba ","correo":" VECINO@EXAMPLE.COM ","telefono":" 912345678 "}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.correo").value("vecino@example.com"))
                .andExpect(jsonPath("$.rol").value("VECINO"));

        assertEquals(1, repository.count());
        Usuario guardado = repository.findAll().get(0);
        assertEquals("Vecino de prueba", jdbc.queryForObject(
                "SELECT nombre FROM usuarios WHERE id = ?", String.class, guardado.getId()));
        assertEquals("vecino@example.com", guardado.getCorreo());
        assertEquals("912345678", guardado.getTelefono());
    }

    @Test
    void listarSinUsuariosDevuelveUnaListaVacia() throws Exception {
        mvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listarDevuelveLosUsuariosGuardadosSinDependerDelOrden() throws Exception {
        guardar("Vecino A", "a@example.com", Rol.VECINO);
        guardar("Vecino B", "b@example.com", Rol.VECINO);

        mvc.perform(get("/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].correo", containsInAnyOrder("a@example.com", "b@example.com")));
    }

    @Test
    void consultarPorIdDevuelveElUsuarioPersistido() throws Exception {
        Usuario usuario = guardar("Conductor", "conductor@example.com", Rol.CONDUCTOR);

        mvc.perform(get("/usuarios/{id}", usuario.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId()))
                .andExpect(jsonPath("$.correo").value("conductor@example.com"))
                .andExpect(jsonPath("$.rol").value("CONDUCTOR"));
    }

    @Test
    void consultarUsuarioInexistenteDevuelve404ConMensaje() throws Exception {
        mvc.perform(get("/usuarios/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado"))
                .andExpect(jsonPath("$.detalles", hasSize(0)));
    }

    @Test
    void actualizarGuardaLosCambiosYConservaIdYRol() throws Exception {
        Usuario usuario = guardar("Nombre anterior", "anterior@example.com", Rol.CONDUCTOR);

        mvc.perform(put("/usuarios/{id}", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":" Nombre nuevo ","correo":" NUEVO@EXAMPLE.COM ","telefono":"987654321"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getId()))
                .andExpect(jsonPath("$.rol").value("CONDUCTOR"));

        Usuario actualizado = repository.findById(usuario.getId()).orElseThrow();
        assertEquals("Nombre nuevo", actualizado.getNombre());
        assertEquals("nuevo@example.com", actualizado.getCorreo());
        assertEquals("987654321", actualizado.getTelefono());
        assertEquals(Rol.CONDUCTOR, actualizado.getRol());
        assertEquals(1, repository.count());
    }

    @Test
    void actualizarPermiteElCorreoPropioYBorraElTelefonoOmitido() throws Exception {
        Usuario usuario = guardar("Vecino", "vecino@example.com", Rol.VECINO);

        mvc.perform(put("/usuarios/{id}", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Vecino actualizado","correo":" VECINO@EXAMPLE.COM "}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("vecino@example.com"));

        assertNull(repository.findById(usuario.getId()).orElseThrow().getTelefono());
        assertEquals(1, repository.count());
    }

    @Test
    void actualizarUsuarioInexistenteDevuelve404SinCrearRegistros() throws Exception {
        mvc.perform(put("/usuarios/{id}", Long.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Vecino","correo":"vecino@example.com"}
                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado"));
        assertEquals(0, repository.count());
    }

    @Test
    void eliminarDevuelve204YRetiraElRegistroDePostgreSQL() throws Exception {
        Usuario usuario = guardar("Vecino", "vecino@example.com", Rol.VECINO);

        mvc.perform(delete("/usuarios/{id}", usuario.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertEquals(0, jdbc.queryForObject(
                "SELECT count(*) FROM usuarios WHERE id = ?", Integer.class, usuario.getId()));
        mvc.perform(get("/usuarios/{id}", usuario.getId())).andExpect(status().isNotFound());
    }

    @Test
    void eliminarUsuarioInexistenteDevuelve404() throws Exception {
        mvc.perform(delete("/usuarios/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado"));
    }

    @Test
    void crearConDatosInvalidosDevuelve400SinGuardar() throws Exception {
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":" ","correo":"correo-invalido"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("Datos de entrada invalidos"))
                .andExpect(jsonPath("$.detalles", hasItem("nombre: El nombre es obligatorio")))
                .andExpect(jsonPath("$.detalles", hasItem("correo: El correo debe tener un formato valido")));
        assertEquals(0, repository.count());
    }

    @Test
    void actualizarConTelefonoLargoDevuelve400SinModificarDatos() throws Exception {
        Usuario usuario = guardar("Nombre original", "vecino@example.com", Rol.VECINO);

        mvc.perform(put("/usuarios/{id}", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Otro nombre","correo":"vecino@example.com","telefono":"111111111111111111111"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasItem("telefono: El telefono no puede superar 20 caracteres")));

        Usuario sinCambios = repository.findById(usuario.getId()).orElseThrow();
        assertEquals("Nombre original", sinCambios.getNombre());
        assertEquals("912345678", sinCambios.getTelefono());
    }

    @Test
    void crearCorreoDuplicadoDevuelve409SinInsertarOtraFila() throws Exception {
        guardar("Vecino", "vecino@example.com", Rol.VECINO);

        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Duplicado","correo":" VECINO@EXAMPLE.COM "}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value("El correo ya esta registrado"));
        assertEquals(1, repository.count());
    }

    @Test
    void actualizarConCorreoAjenoDevuelve409SinCambiarElUsuario() throws Exception {
        guardar("Vecino A", "a@example.com", Rol.VECINO);
        Usuario usuario = guardar("Vecino B", "b@example.com", Rol.CONDUCTOR);

        mvc.perform(put("/usuarios/{id}", usuario.getId())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre":"Otro nombre","correo":" A@EXAMPLE.COM "}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo ya esta registrado"));

        Usuario sinCambios = repository.findById(usuario.getId()).orElseThrow();
        assertEquals("Vecino B", sinCambios.getNombre());
        assertEquals("b@example.com", sinCambios.getCorreo());
        assertEquals(Rol.CONDUCTOR, sinCambios.getRol());
        assertEquals(2, repository.count());
    }

    @Test
    void jsonIncorrectoEIdInvalidoDevuelven400ConFormatoUniforme() throws Exception {
        mvc.perform(post("/usuarios").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("El cuerpo debe contener un JSON valido con los campos esperados"))
                .andExpect(jsonPath("$.detalles").isArray());
        mvc.perform(get("/usuarios/no-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value("El parametro de la solicitud tiene un formato invalido"));
        assertEquals(0, repository.count());
    }

    @Test
    void postgreSQLImpideDuplicadosAunqueSeGuardenDirectamente() {
        guardar("Vecino A", "vecino@example.com", Rol.VECINO);
        Usuario duplicado = new Usuario();
        duplicado.setNombre("Vecino B");
        duplicado.setCorreo("vecino@example.com");

        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicado));
        assertEquals(1, repository.count());
    }

    private void comprobarBaseDePruebas() {
        assertEquals("usuarios_test_db", jdbc.queryForObject("SELECT current_database()", String.class));
        assertEquals("usuarios_test_user", jdbc.queryForObject("SELECT current_user", String.class));
    }

    private Usuario guardar(String nombre, String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setTelefono("912345678");
        usuario.setRol(rol);
        return repository.saveAndFlush(usuario);
    }
}
