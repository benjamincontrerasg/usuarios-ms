package com.duoc.usuarios_ms.service;

import com.duoc.usuarios_ms.dto.UsuarioRequest;
import com.duoc.usuarios_ms.dto.UsuarioResponse;
import com.duoc.usuarios_ms.model.Rol;
import com.duoc.usuarios_ms.model.Usuario;
import com.duoc.usuarios_ms.repository.UsuarioRepository;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @InjectMocks
    private UsuarioService service;

    @Test
    void crearNormalizaDatosYAsignaRolVecino() {
        UsuarioRequest datos = datos(" Vecino ", " VECINO@EXAMPLE.COM ", " 912345678 ");
        Usuario guardado = usuario(7L, "Vecino", "vecino@example.com", Rol.VECINO);
        when(repository.save(any(Usuario.class))).thenReturn(guardado);

        UsuarioResponse respuesta = service.crear(datos);

        ArgumentCaptor<Usuario> captura = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captura.capture());
        Usuario enviado = captura.getValue();
        assertNull(enviado.getId());
        assertEquals("Vecino", enviado.getNombre());
        assertEquals("vecino@example.com", enviado.getCorreo());
        assertEquals("912345678", enviado.getTelefono());
        assertEquals(Rol.VECINO, enviado.getRol());
        assertEquals(7L, respuesta.getId());
    }

    @Test
    void crearRechazaCorreoDuplicadoSinGuardar() {
        when(repository.existsByCorreoIgnoreCase("vecino@example.com")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(datos("Vecino", " VECINO@EXAMPLE.COM ", null)));

        assertEquals(409, error.getStatusCode().value());
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void crearRechazaCamposObligatoriosAntesDeConsultarLaBase() {
        UsuarioRequest[] casos = {
                datos(null, "vecino@example.com", null),
                datos(" ", "vecino@example.com", null),
                datos("Vecino", null, null),
                datos("Vecino", " ", null)
        };

        for (UsuarioRequest caso : casos) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> service.crear(caso));
            assertEquals(400, error.getStatusCode().value());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void crearDevuelveConflictoSiLaBaseDetectaUnDuplicado() {
        SQLException causa = new SQLException("Valor unico duplicado", "23505");
        when(repository.save(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("Conflicto", causa));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(datos("Vecino", "vecino@example.com", null)));

        assertEquals(409, error.getStatusCode().value());
        assertEquals("El correo ya esta registrado", error.getReason());
    }

    @Test
    void crearNoConfundeOtrosErroresDeBaseConCorreosDuplicados() {
        SQLException causa = new SQLException("Campo obligatorio", "23502");
        DataIntegrityViolationException original = new DataIntegrityViolationException("Error", causa);
        when(repository.save(any(Usuario.class))).thenThrow(original);

        DataIntegrityViolationException error = assertThrows(DataIntegrityViolationException.class,
                () -> service.crear(datos("Vecino", "vecino@example.com", null)));

        assertSame(original, error);
    }

    @Test
    void actualizarCambiaDatosYConservaIdYRol() {
        Usuario existente = usuario(7L, "Nombre anterior", "anterior@example.com", Rol.CONDUCTOR);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        UsuarioResponse respuesta = service.actualizar(7L,
                datos(" Nombre nuevo ", " NUEVO@EXAMPLE.COM ", " 987654321 "));

        assertEquals(7L, respuesta.getId());
        assertEquals(Rol.CONDUCTOR, respuesta.getRol());
        assertEquals("Nombre nuevo", respuesta.getNombre());
        assertEquals("nuevo@example.com", respuesta.getCorreo());
        assertEquals("987654321", respuesta.getTelefono());
        verify(repository).save(existente);
    }

    @Test
    void actualizarPermiteConservarElCorreoPropio() {
        Usuario existente = usuario(7L, "Vecino", "vecino@example.com", Rol.VECINO);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        UsuarioResponse respuesta = service.actualizar(7L,
                datos("Nuevo nombre", " VECINO@EXAMPLE.COM ", null));

        assertEquals("vecino@example.com", respuesta.getCorreo());
        assertEquals("Nuevo nombre", respuesta.getNombre());
    }

    @Test
    void actualizarRechazaCorreoAjenoSinModificarElUsuario() {
        Usuario existente = usuario(7L, "Nombre original", "propio@example.com", Rol.CONDUCTOR);
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.existsByCorreoIgnoreCaseAndIdNot("ajeno@example.com", 7L)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.actualizar(7L, datos("Otro nombre", " AJENO@EXAMPLE.COM ", null)));

        assertEquals(409, error.getStatusCode().value());
        assertEquals("Nombre original", existente.getNombre());
        assertEquals("propio@example.com", existente.getCorreo());
        assertEquals(Rol.CONDUCTOR, existente.getRol());
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuarioInexistenteDevuelve404SinGuardar() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.actualizar(7L, datos("Vecino", "vecino@example.com", null)));

        assertEquals(404, error.getStatusCode().value());
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarSinTelefonoBorraElTelefonoAnterior() {
        Usuario existente = usuario(7L, "Vecino", "vecino@example.com", Rol.VECINO);
        existente.setTelefono("912345678");
        when(repository.findById(7L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        UsuarioResponse respuesta = service.actualizar(7L,
                datos("Vecino", "vecino@example.com", null));

        assertNull(respuesta.getTelefono());
        assertNull(existente.getTelefono());
        verify(repository).save(existente);
    }

    private UsuarioRequest datos(String nombre, String correo, String telefono) {
        UsuarioRequest datos = new UsuarioRequest();
        datos.setNombre(nombre);
        datos.setCorreo(correo);
        datos.setTelefono(telefono);
        return datos;
    }

    private Usuario usuario(Long id, String nombre, String correo, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setRol(rol);
        return usuario;
    }
}
