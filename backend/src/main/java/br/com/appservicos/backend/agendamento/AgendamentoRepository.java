package br.com.appservicos.backend.agendamento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AgendamentoRepository
        extends JpaRepository<Agendamento, Integer>, JpaSpecificationExecutor<Agendamento> {
    @EntityGraph(
            attributePaths = {
                "contratacao",
                "disponibilidade",
                "orcamento",
                "orcamento.prestador",
                "orcamento.prestador.usuario",
                "orcamento.pedido",
                "orcamento.pedido.cliente",
                "orcamento.pedido.cliente.usuario"
            })
    @Query(
            "select a from Agendamento a where a.id=:id and ((:tipo='cliente' and"
                + " a.orcamento.pedido.cliente.usuario.id=:usuario) or (:tipo='prestador' and"
                + " a.orcamento.prestador.usuario.id=:usuario))")
    Optional<Agendamento> buscarChatDoParticipante(
            @Param("id") Integer id, @Param("usuario") Integer usuario, @Param("tipo") String tipo);

    @Override
    @EntityGraph(
            attributePaths = {
                "contratacao",
                "disponibilidade",
                "orcamento",
                "orcamento.prestador",
                "orcamento.pedido",
                "orcamento.pedido.cliente"
            })
    Page<Agendamento> findAll(Specification<Agendamento> spec, Pageable page);

    @EntityGraph(
            attributePaths = {
                "contratacao",
                "disponibilidade",
                "orcamento",
                "orcamento.prestador",
                "orcamento.pedido",
                "orcamento.pedido.cliente"
            })
    Optional<Agendamento> findByIdAndOrcamentoPedidoClienteId(Integer id, Integer cliente);

    @EntityGraph(
            attributePaths = {
                "contratacao",
                "disponibilidade",
                "orcamento",
                "orcamento.prestador",
                "orcamento.pedido",
                "orcamento.pedido.cliente"
            })
    Optional<Agendamento> findByIdAndOrcamentoPrestadorId(Integer id, Integer prestador);
}
