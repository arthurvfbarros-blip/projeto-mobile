package br.com.appservicos.backend.prestador;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrestadorRepository extends JpaRepository<Prestador, Integer> {

	boolean existsByCpf(String cpf);

	@EntityGraph(attributePaths = "usuario")
	Optional<Prestador> findByUsuarioId(Integer idUsuario);
}
