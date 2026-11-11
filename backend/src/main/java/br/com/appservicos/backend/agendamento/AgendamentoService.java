package br.com.appservicos.backend.agendamento;

import br.com.appservicos.backend.agendamento.dto.AgendamentoResponse;
import br.com.appservicos.backend.agendamento.dto.CadastroAgendamentoRequest;
import br.com.appservicos.backend.agendamento.dto.PaginaAgendamentosResponse;
import br.com.appservicos.backend.api.CampoInvalidoException;
import br.com.appservicos.backend.api.ConflitoException;
import br.com.appservicos.backend.api.RecursoNaoEncontradoException;
import br.com.appservicos.backend.cliente.ClienteAutenticadoService;
import br.com.appservicos.backend.disponibilidade.DisponibilidadeRepository;
import br.com.appservicos.backend.endereco.EnderecoClienteRepository;
import br.com.appservicos.backend.orcamento.OrcamentoRepository;
import br.com.appservicos.backend.pedido.PedidoServicoRepository;
import br.com.appservicos.backend.prestador.AreaAtendimento;
import br.com.appservicos.backend.prestador.PrestadorAutenticadoService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class AgendamentoService {
    private final AgendamentoRepository agendamentos;
    private final OrcamentoRepository orcamentos;
    private final PedidoServicoRepository pedidos;
    private final DisponibilidadeRepository disponibilidades;
    private final EnderecoClienteRepository enderecos;
    private final ClienteAutenticadoService clientes;
    private final PrestadorAutenticadoService prestadores;
    private final AgendamentoElegibilidadeService elegibilidade;
    private final Clock clock;

    public AgendamentoService(
            AgendamentoRepository agendamentos,
            OrcamentoRepository orcamentos,
            PedidoServicoRepository pedidos,
            DisponibilidadeRepository disponibilidades,
            EnderecoClienteRepository enderecos,
            ClienteAutenticadoService clientes,
            PrestadorAutenticadoService prestadores,
            AgendamentoElegibilidadeService elegibilidade,
            Clock clock) {
        this.agendamentos = agendamentos;
        this.orcamentos = orcamentos;
        this.pedidos = pedidos;
        this.disponibilidades = disponibilidades;
        this.enderecos = enderecos;
        this.clientes = clientes;
        this.prestadores = prestadores;
        this.elegibilidade = elegibilidade;
        this.clock = clock;
    }

    @Transactional
    public AgendamentoResponse criar(String usuario, CadastroAgendamentoRequest request) {
        var cliente = clientes.buscar(usuario);
        var idPedido =
                orcamentos
                        .buscarPedidoDoCliente(request.idOrcamento(), cliente.getId())
                        .orElseThrow(() ->
                                new RecursoNaoEncontradoException("Orçamento não encontrado"));
        var pedido =
                pedidos.buscarParaAtualizacao(idPedido)
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        var o = elegibilidade.buscar(request.idOrcamento(), cliente.getId());
        var d =
                disponibilidades
                        .buscarParaReserva(request.idDisponibilidade())
                        .orElseThrow(() ->
                                new CampoInvalidoException(
                                        "idDisponibilidade",
                                        "Data indisponível, escolha outra data"));
        if (!d.getPrestador().getId().equals(o.getPrestador().getId())
                || !"livre".equals(d.getStatus())
                || !LocalDateTime.of(d.getData(), d.getHora()).isAfter(LocalDateTime.now(clock))) {
            throw new ConflitoException("Data indisponível, escolha outra data");
        }
        var endereco =
                enderecos
                        .findByIdAndClienteId(request.idEndereco(), cliente.getId())
                        .orElseThrow(() ->
                                new CampoInvalidoException(
                                        "idEndereco",
                                        "Selecione um endereço cadastrado na sua conta"));
        if (!AreaAtendimento.atende(o.getPrestador(), endereco))
            throw new CampoInvalidoException(
                    "idEndereco", "O endereço está fora da área de atendimento do prestador");
        d.reservar();
        o.aceitar();
        pedido.agendar();
        orcamentos
                .findByPedidoIdAndStatus(idPedido, "enviado")
                .forEach(outro -> {
                    if (!outro.getId().equals(o.getId())) outro.recusar();
                });
        return AgendamentoResponse.from(agendamentos.saveAndFlush(new Agendamento(o, d, endereco)));
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscar(String usuario, Integer id, boolean prestador) {
        var registro =
                prestador
                        ? agendamentos.findByIdAndOrcamentoPrestadorId(
                                id, prestadores.buscar(usuario).getId())
                        : agendamentos.findByIdAndOrcamentoPedidoClienteId(
                                id, clientes.buscar(usuario).getId());
        return registro.map(AgendamentoResponse::from)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado"));
    }

    @Transactional(readOnly = true)
    public PaginaAgendamentosResponse listar(
            String usuario, boolean prestador, String periodo, int pagina, int tamanho) {
        var perfil =
                prestador ? prestadores.buscar(usuario).getId() : clientes.buscar(usuario).getId();
        if (!java.util.Set.of("proximos", "historico", "cancelados").contains(periodo))
            throw new CampoInvalidoException(
                    "periodo", "Selecione próximos, histórico ou cancelados");
        if (pagina < 0)
            throw new CampoInvalidoException("pagina", "A página deve ser maior ou igual a zero");
        if (tamanho < 1 || tamanho > 50)
            throw new CampoInvalidoException("tamanho", "O tamanho deve estar entre 1 e 50");
        var agora = LocalDateTime.now(clock);
        Specification<Agendamento> filtro =
                (root, query, cb) -> {
                    var o = root.get("orcamento");
                    var d = root.get("disponibilidade");
                    var dono =
                            prestador
                                    ? o.get("prestador").get("id")
                                    : o.get("pedido").get("cliente").get("id");
                    var futuro =
                            cb.or(
                                    cb.greaterThan(d.get("data"), agora.toLocalDate()),
                                    cb.and(
                                            cb.equal(d.get("data"), agora.toLocalDate()),
                                            cb.greaterThanOrEqualTo(
                                                    d.get("hora"), agora.toLocalTime())));
                    var estado =
                            periodo.equals("cancelados")
                                    ? cb.equal(root.get("status"), "cancelado")
                                    : periodo.equals("proximos")
                                            ? cb.and(
                                                    cb.equal(root.get("status"), "confirmado"),
                                                    futuro)
                                            : cb.or(
                                                    cb.equal(root.get("status"), "concluido"),
                                                    cb.and(
                                                            cb.equal(
                                                                    root.get("status"),
                                                                    "confirmado"),
                                                            cb.not(futuro)));
                    return cb.and(cb.equal(dono, perfil), estado);
                };
        var sort =
                Sort.by(
                        periodo.equals("proximos") ? Sort.Direction.ASC : Sort.Direction.DESC,
                        "disponibilidade.data",
                        "disponibilidade.hora",
                        "id");
        return PaginaAgendamentosResponse.from(
                agendamentos.findAll(filtro, PageRequest.of(pagina, tamanho, sort)));
    }
}
