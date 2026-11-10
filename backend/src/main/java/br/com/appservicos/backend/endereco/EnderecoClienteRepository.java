package br.com.appservicos.backend.endereco;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoClienteRepository extends JpaRepository<EnderecoCliente, Integer> {

	List<EnderecoCliente> findByClienteIdOrderByIdAsc(Integer idCliente);

	Optional<EnderecoCliente> findByIdAndClienteId(Integer id, Integer idCliente);
}
