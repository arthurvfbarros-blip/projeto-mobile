package br.com.appservicos.backend.orcamento;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Integer> {
	@Query("select o.pedido.id from Orcamento o where o.id = :orcamento and o.pedido.cliente.id = :cliente")
	Optional<Integer> buscarPedidoDoCliente(@Param("orcamento") Integer orcamento, @Param("cliente") Integer cliente);

	@EntityGraph(attributePaths = {"pedido", "prestador"})
	Optional<Orcamento> findByIdAndPedidoClienteId(Integer id, Integer cliente);

	List<Orcamento> findByPedidoIdAndStatus(Integer pedido, String status);

	boolean existsByPedidoIdAndPrestadorId(Integer idPedido, Integer idPrestador);

	@EntityGraph(attributePaths = {"pedido", "prestador"})
	Optional<Orcamento> findByPedidoIdAndPrestadorId(Integer idPedido, Integer idPrestador);

	@EntityGraph(attributePaths = {"pedido", "prestador"})
	Page<Orcamento> findByPedidoId(Integer idPedido, Pageable pageable);
}
