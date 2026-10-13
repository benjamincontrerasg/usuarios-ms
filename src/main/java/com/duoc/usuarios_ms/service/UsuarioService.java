package com.duoc.usuarios_ms.service;

import com.duoc.usuarios_ms.dto.UsuarioRequest;
import com.duoc.usuarios_ms.dto.UsuarioResponse;
import com.duoc.usuarios_ms.model.Usuario;
import com.duoc.usuarios_ms.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public UsuarioResponse crear(UsuarioRequest datos) {
        if (datos.getNombre() == null || datos.getNombre().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (datos.getCorreo() == null || datos.getCorreo().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo es obligatorio");
        }

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
}
