package br.com.appservicos.backend.cliente;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

	boolean existsByCpf(String cpf);

	Optional<Cliente> findByUsuarioId(Integer idUsuario);
}
