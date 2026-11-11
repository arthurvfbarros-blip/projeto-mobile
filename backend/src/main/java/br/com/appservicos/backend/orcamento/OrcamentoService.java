package br.com.appservicos.backend.orcamento;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.api.CampoInvalidoException;
import br.com.appservicos.backend.api.ConflitoException;
import br.com.appservicos.backend.api.RecursoNaoEncontradoException;
import br.com.appservicos.backend.cliente.ClienteAutenticadoService;
import br.com.appservicos.backend.oportunidade.OportunidadeService;
import br.com.appservicos.backend.orcamento.dto.CadastroOrcamentoRequest;
import br.com.appservicos.backend.orcamento.dto.OrcamentoResponse;
import br.com.appservicos.backend.orcamento.dto.PaginaOrcamentosResponse;
import br.com.appservicos.backend.pedido.PedidoServicoRepository;
import br.com.appservicos.backend.prestador.PrestadorAutenticadoService;

@Service
public class OrcamentoService {
	private final OrcamentoRepository orcamentos;
	private final PedidoServicoRepository pedidos;
	private final PrestadorAutenticadoService prestadores;
	private final ClienteAutenticadoService clientes;
	private final OportunidadeService oportunidades;

	public OrcamentoService(OrcamentoRepository orcamentos, PedidoServicoRepository pedidos,
			PrestadorAutenticadoService prestadores, ClienteAutenticadoService clientes, OportunidadeService oportunidades) {
		this.orcamentos = orcamentos;
		this.pedidos = pedidos;
		this.prestadores = prestadores;
		this.clientes = clientes;
		this.oportunidades = oportunidades;
	}

	@Transactional
	public OrcamentoResponse criar(String idUsuario, Integer idPedido, CadastroOrcamentoRequest request) {
		var prestador = prestadores.buscar(idUsuario);
		var pedido = pedidos.buscarParaAtualizacao(idPedido)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Oportunidade não encontrada"));
		oportunidades.buscar(idUsuario, idPedido);
		if (orcamentos.existsByPedidoIdAndPrestadorId(idPedido, prestador.getId())) {
			throw new ConflitoException("Você já enviou um orçamento para este pedido");
		}
		var orcamento = new Orcamento(pedido, prestador, request.valor(), request.prazoDias().intValueExact(), request.descricao());
		pedido.receberOrcamento();
		try {
			return OrcamentoResponse.from(orcamentos.saveAndFlush(orcamento));
		} catch (DataIntegrityViolationException exception) {
			for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
				if (cause instanceof ConstraintViolationException constraint
						&& constraint.getConstraintName() != null
						&& constraint.getConstraintName().contains("uq_orcamento_pedido_prestador")) {
					throw new ConflitoException("Você já enviou um orçamento para este pedido");
				}
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public OrcamentoResponse buscarDoPrestador(String idUsuario, Integer idPedido) {
		var prestador = prestadores.buscar(idUsuario);
		return orcamentos.findByPedidoIdAndPrestadorId(idPedido, prestador.getId()).map(OrcamentoResponse::from)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Orçamento não encontrado"));
	}

	@Transactional(readOnly = true)
	public PaginaOrcamentosResponse listarDoCliente(String idUsuario, Integer idPedido, int pagina, int tamanho) {
		var cliente = clientes.buscar(idUsuario);
		pedidos.findByIdAndClienteId(idPedido, cliente.getId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
		if (pagina < 0) throw new CampoInvalidoException("pagina", "A página deve ser maior ou igual a zero");
		if (tamanho < 1 || tamanho > 50) throw new CampoInvalidoException("tamanho", "O tamanho deve estar entre 1 e 50");
		return PaginaOrcamentosResponse.from(orcamentos.findByPedidoId(idPedido,
				PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id"))));
	}
}
