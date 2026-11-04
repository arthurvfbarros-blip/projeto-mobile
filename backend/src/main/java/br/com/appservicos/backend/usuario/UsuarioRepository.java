package br.com.appservicos.backend.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

	Optional<Usuario> findByEmail(String email);
}
