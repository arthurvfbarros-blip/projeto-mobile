package br.com.appservicos.backend.pedido;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface PedidoServicoRepository extends JpaRepository<PedidoServico, Integer>, JpaSpecificationExecutor<PedidoServico> {

	@Override
	@EntityGraph(attributePaths = "categoria")
	Page<PedidoServico> findAll(Specification<PedidoServico> spec, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = "categoria")
	Optional<PedidoServico> findOne(Specification<PedidoServico> spec);

	@EntityGraph(attributePaths = "categoria")
	Page<PedidoServico> findByClienteId(Integer idCliente, Pageable pageable);

	@EntityGraph(attributePaths = "categoria")
	Optional<PedidoServico> findByIdAndClienteId(Integer id, Integer idCliente);
}
