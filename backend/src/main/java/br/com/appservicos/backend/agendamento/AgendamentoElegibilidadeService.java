package br.com.appservicos.backend.agendamento;

import br.com.appservicos.backend.api.ConflitoException;
import br.com.appservicos.backend.api.RecursoNaoEncontradoException;
import br.com.appservicos.backend.orcamento.Orcamento;
import br.com.appservicos.backend.orcamento.OrcamentoRepository;
import br.com.appservicos.backend.prestador.Prestador;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AgendamentoElegibilidadeService {
    private final OrcamentoRepository orcamentos;

    public AgendamentoElegibilidadeService(OrcamentoRepository orcamentos) {
        this.orcamentos = orcamentos;
    }

    public Orcamento buscar(Integer idOrcamento, Integer idCliente) {
        var o =
                orcamentos
                        .findByIdAndPedidoClienteId(idOrcamento, idCliente)
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException("Orçamento não encontrado"));
        if (!"enviado".equals(o.getStatus())
                || !("aberto".equals(o.getPedido().getStatus())
                        || "em_orcamento".equals(o.getPedido().getStatus()))) {
            throw new ConflitoException("Este pedido não está disponível para agendamento");
        }
        exigirPrestadorApto(o.getPrestador());
        return o;
    }

    public void exigirPrestadorApto(Prestador p) {
        if (!p.getUsuario().isAtivo()
                || !"prestador".equals(p.getUsuario().getTipoUsuario())
                || !"aprovado".equals(p.getStatusAprovacao())) {
            throw new AccessDeniedException("Prestador não disponível para agendamento");
        }
    }
}
