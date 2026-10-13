package com.duoc.usuarios_ms.service;

import com.duoc.usuarios_ms.dto.UsuarioRequest;
import com.duoc.usuarios_ms.dto.UsuarioResponse;
import com.duoc.usuarios_ms.model.Usuario;
import com.duoc.usuarios_ms.repository.UsuarioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public UsuarioResponse buscarPorId(Long id) {
        Optional<Usuario> resultado = repository.findById(id);
        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        Usuario usuario = resultado.get();
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(),
                usuario.getCorreo(), usuario.getTelefono(), usuario.getRol());
    }

    public List<UsuarioResponse> listar() {
        List<Usuario> usuarios = repository.findAll();
        List<UsuarioResponse> respuestas = new ArrayList<>();

        for (Usuario usuario : usuarios) {
            respuestas.add(new UsuarioResponse(usuario.getId(), usuario.getNombre(),
                    usuario.getCorreo(), usuario.getTelefono(), usuario.getRol()));
        }

        return respuestas;
    }

    public UsuarioResponse crear(UsuarioRequest datos) {
        validarDatos(datos);

        Usuario usuario = new Usuario();
        usuario.setNombre(datos.getNombre().trim());
        usuario.setCorreo(datos.getCorreo().trim().toLowerCase(Locale.ROOT));
        if (datos.getTelefono() != null) {
            usuario.setTelefono(datos.getTelefono().trim());
        }

        Usuario guardado = repository.save(usuario);
        return new UsuarioResponse(guardado.getId(), guardado.getNombre(),
                guardado.getCorreo(), guardado.getTelefono(), guardado.getRol());
    }

    public UsuarioResponse actualizar(Long id, UsuarioRequest datos) {
        validarDatos(datos);

        Optional<Usuario> resultado = repository.findById(id);
        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        Usuario usuario = resultado.get();
        usuario.setNombre(datos.getNombre().trim());
        usuario.setCorreo(datos.getCorreo().trim().toLowerCase(Locale.ROOT));
        if (datos.getTelefono() == null) {
            usuario.setTelefono(null);
        } else {
            usuario.setTelefono(datos.getTelefono().trim());
        }

        Usuario guardado = repository.save(usuario);
        return new UsuarioResponse(guardado.getId(), guardado.getNombre(),
                guardado.getCorreo(), guardado.getTelefono(), guardado.getRol());
    }

    private void validarDatos(UsuarioRequest datos) {
        if (datos.getNombre() == null || datos.getNombre().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (datos.getCorreo() == null || datos.getCorreo().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo es obligatorio");
        }
    }
}
