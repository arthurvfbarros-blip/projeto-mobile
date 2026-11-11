package br.com.appservicos.backend.orcamento;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Integer> {
	boolean existsByPedidoIdAndPrestadorId(Integer idPedido, Integer idPrestador);

	@EntityGraph(attributePaths = {"pedido", "prestador"})
	Optional<Orcamento> findByPedidoIdAndPrestadorId(Integer idPedido, Integer idPrestador);

	@EntityGraph(attributePaths = {"pedido", "prestador"})
	Page<Orcamento> findByPedidoId(Integer idPedido, Pageable pageable);
}
