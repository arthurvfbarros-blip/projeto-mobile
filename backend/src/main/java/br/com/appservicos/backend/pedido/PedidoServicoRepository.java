package br.com.appservicos.backend.pedido;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoServicoRepository extends JpaRepository<PedidoServico, Integer> {

	@EntityGraph(attributePaths = "categoria")
	Page<PedidoServico> findByClienteId(Integer idCliente, Pageable pageable);

	@EntityGraph(attributePaths = "categoria")
	Optional<PedidoServico> findByIdAndClienteId(Integer id, Integer idCliente);
}
