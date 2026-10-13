package com.duoc.usuarios_ms.repository;

import com.duoc.usuarios_ms.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
}
