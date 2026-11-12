package br.com.appservicos.backend.contratacao;

import br.com.appservicos.backend.agendamento.Agendamento;
import br.com.appservicos.backend.agendamento.AgendamentoRepository;
import br.com.appservicos.backend.agendamento.dto.AgendamentoResponse;
import br.com.appservicos.backend.api.ConflitoException;
import br.com.appservicos.backend.api.RecursoNaoEncontradoException;
import br.com.appservicos.backend.chat.MensagemRepository;
import br.com.appservicos.backend.cliente.ClienteAutenticadoService;
import br.com.appservicos.backend.contratacao.dto.ContratacaoResponse;
import br.com.appservicos.backend.contratacao.dto.RevisaoContratacaoResponse;
import br.com.appservicos.backend.notificacao.NotificacaoService;
import br.com.appservicos.backend.prestador.PrestadorAutenticadoService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ContratacaoService {
    private final ContratacaoRepository contratacoes;
    private final AgendamentoRepository agendamentos;
    private final ClienteAutenticadoService clientes;
    private final PrestadorAutenticadoService prestadores;
    private final MensagemRepository mensagens;
    private final NotificacaoService notificacoes;
    private final EntityManager em;
    private final Clock clock;

    public ContratacaoService(
            ContratacaoRepository contratacoes,
            AgendamentoRepository agendamentos,
            ClienteAutenticadoService clientes,
            PrestadorAutenticadoService prestadores,
            MensagemRepository mensagens,
            NotificacaoService notificacoes,
            EntityManager em,
            Clock clock) {
        this.contratacoes = contratacoes;
        this.agendamentos = agendamentos;
        this.clientes = clientes;
        this.prestadores = prestadores;
        this.mensagens = mensagens;
        this.notificacoes = notificacoes;
        this.em = em;
        this.clock = clock;
    }

    private Agendamento doCliente(String usuario, Integer id) {
        return agendamentos
                .findByIdAndOrcamentoPedidoClienteId(id, clientes.buscar(usuario).getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado"));
    }

    @Transactional(readOnly = true)
    public RevisaoContratacaoResponse revisar(String usuario, Integer id) {
        var a = doCliente(usuario, id);
        var motivo = motivoBloqueio(a);
        return new RevisaoContratacaoResponse(AgendamentoResponse.from(a), motivo == null, motivo);
    }

    @Transactional
    public ContratacaoResponse confirmar(String usuario, Integer id) {
        var a = doCliente(usuario, id);
        em.refresh(a, LockModeType.PESSIMISTIC_WRITE);
        var existente = contratacoes.findByAgendamentoId(id);
        if (existente.isPresent()) return ContratacaoResponse.from(existente.get());
        var motivo = motivoBloqueio(a);
        if (motivo != null) throw new ConflitoException(motivo);
        var agora = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        var c = contratacoes.saveAndFlush(new Contratacao(a, agora));
        a.registrarContratacao(c);
        a.getOrcamento().getPedido().confirmarContratacao();
        notificacoes.contratacaoConfirmada(a, agora);
        return ContratacaoResponse.from(c);
    }

    @Transactional(readOnly = true)
    public ContratacaoResponse buscar(String usuario, Integer id, boolean prestador) {
        var a =
                prestador
                        ? agendamentos
                                .findByIdAndOrcamentoPrestadorId(
                                        id, prestadores.buscar(usuario).getId())
                                .orElseThrow(() ->
                                        new RecursoNaoEncontradoException(
                                                "Agendamento não encontrado"))
                        : doCliente(usuario, id);
        return contratacoes
                .findByAgendamentoId(a.getId())
                .map(ContratacaoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Contratação não encontrada"));
    }

    private String motivoBloqueio(Agendamento a) {
        if (a.getContratacao() != null) return "Este serviço já está contratado";
        var o = a.getOrcamento();
        var p = o.getPrestador();
        if (!"confirmado".equals(a.getStatus())
                || !"agendado".equals(o.getPedido().getStatus())
                || !"aceito".equals(o.getStatus())
                || !"ocupado".equals(a.getDisponibilidade().getStatus()))
            return "Este agendamento não está disponível para contratação";
        if (!p.getUsuario().isAtivo()
                || !"prestador".equals(p.getUsuario().getTipoUsuario())
                || !"aprovado".equals(p.getStatusAprovacao()))
            return "O prestador precisa estar ativo e aprovado para confirmar a contratação";
        var cliente = o.getPedido().getCliente().getUsuario();
        if (!mensagens.existsByAgendamentoIdAndRemetenteId(a.getId(), cliente.getId())
                || !mensagens.existsByAgendamentoIdAndRemetenteId(
                        a.getId(), p.getUsuario().getId()))
            return "Cliente e prestador precisam enviar ao menos uma mensagem na conversa antes da"
                    + " contratação";
        return null;
    }
}
