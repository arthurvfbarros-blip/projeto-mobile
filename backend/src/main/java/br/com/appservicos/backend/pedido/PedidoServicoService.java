package br.com.appservicos.backend.pedido;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.api.CampoInvalidoException;
import br.com.appservicos.backend.api.RecursoNaoEncontradoException;
import br.com.appservicos.backend.categoria.CategoriaServicoRepository;
import br.com.appservicos.backend.cliente.ClienteAutenticadoService;
import br.com.appservicos.backend.endereco.EnderecoClienteRepository;
import br.com.appservicos.backend.pedido.dto.CadastroPedidoRequest;
import br.com.appservicos.backend.pedido.dto.PaginaPedidosResponse;
import br.com.appservicos.backend.pedido.dto.PedidoResponse;

@Service
public class PedidoServicoService {

	private final ClienteAutenticadoService clientes;
	private final CategoriaServicoRepository categorias;
	private final EnderecoClienteRepository enderecos;
	private final PedidoServicoRepository pedidos;
	private final Clock clock;

	public PedidoServicoService(ClienteAutenticadoService clientes, CategoriaServicoRepository categorias,
			EnderecoClienteRepository enderecos, PedidoServicoRepository pedidos, Clock clock) {
		this.clientes = clientes;
		this.categorias = categorias;
		this.enderecos = enderecos;
		this.pedidos = pedidos;
		this.clock = clock;
	}

	@Transactional
	public PedidoResponse criar(String idUsuario, CadastroPedidoRequest request) {
		var cliente = clientes.buscar(idUsuario);
		if (request.dataDesejada().getYear() > 9999) {
			throw new CampoInvalidoException("dataDesejada", "Informe uma data desejada válida");
		}
		if (request.dataDesejada().isBefore(LocalDate.now(clock))) {
			throw new CampoInvalidoException("dataDesejada", "A data desejada não pode estar no passado");
		}
		var categoria = categorias.findById(request.idCategoria())
				.orElseThrow(() -> new CampoInvalidoException("idCategoria", "Selecione uma categoria disponível"));
		var endereco = enderecos.findByIdAndClienteId(request.idEndereco(), cliente.getId())
				.orElseThrow(() -> new CampoInvalidoException("idEndereco", "Selecione um endereço cadastrado na sua conta"));
		var pedido = pedidos.saveAndFlush(new PedidoServico(cliente, categoria, endereco,
				request.titulo(), request.descricao(), request.dataDesejada(), Instant.now(clock)));
		return PedidoResponse.from(pedido);
	}

	@Transactional(readOnly = true)
	public PaginaPedidosResponse listar(String idUsuario, int pagina, int tamanho) {
		var cliente = clientes.buscar(idUsuario);
		if (pagina < 0) throw new CampoInvalidoException("pagina", "A página deve ser maior ou igual a zero");
		if (tamanho < 1 || tamanho > 50) throw new CampoInvalidoException("tamanho", "O tamanho deve estar entre 1 e 50");
		return PaginaPedidosResponse.from(pedidos.findByClienteId(cliente.getId(),
				PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id"))));
	}

	@Transactional(readOnly = true)
	public PedidoResponse buscar(String idUsuario, Integer idPedido) {
		var cliente = clientes.buscar(idUsuario);
		return pedidos.findByIdAndClienteId(idPedido, cliente.getId()).map(PedidoResponse::from)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
	}
}
