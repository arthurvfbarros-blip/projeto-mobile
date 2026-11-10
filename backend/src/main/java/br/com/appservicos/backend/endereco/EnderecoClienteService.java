package br.com.appservicos.backend.endereco;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.cliente.Cliente;
import br.com.appservicos.backend.cliente.ClienteRepository;
import br.com.appservicos.backend.endereco.dto.CadastroEnderecoRequest;
import br.com.appservicos.backend.endereco.dto.EnderecoClienteResponse;

@Service
public class EnderecoClienteService {

	private final ClienteRepository clientes;
	private final EnderecoClienteRepository enderecos;

	public EnderecoClienteService(ClienteRepository clientes, EnderecoClienteRepository enderecos) {
		this.clientes = clientes;
		this.enderecos = enderecos;
	}

	@Transactional
	public EnderecoClienteResponse cadastrar(String idUsuario, CadastroEnderecoRequest request) {
		Cliente cliente = clienteAutenticado(idUsuario);
		EnderecoCliente endereco = enderecos.saveAndFlush(new EnderecoCliente(cliente, request.cep(),
				request.logradouro(), request.numero(), request.complemento(), request.bairro(), request.cidade(), request.uf()));
		return EnderecoClienteResponse.from(endereco);
	}

	@Transactional(readOnly = true)
	public List<EnderecoClienteResponse> listar(String idUsuario) {
		Cliente cliente = clienteAutenticado(idUsuario);
		return enderecos.findByClienteIdOrderByIdAsc(cliente.getId()).stream()
				.map(EnderecoClienteResponse::from).toList();
	}

	private Cliente clienteAutenticado(String idUsuario) {
		Integer id;
		try {
			id = Integer.valueOf(idUsuario);
		} catch (NumberFormatException exception) {
			throw new AccessDeniedException("Cliente não autorizado");
		}
		Cliente cliente = clientes.findByUsuarioId(id)
				.orElseThrow(() -> new AccessDeniedException("Cliente não autorizado"));
		if (!cliente.getUsuario().isAtivo() || !"cliente".equals(cliente.getUsuario().getTipoUsuario())) {
			throw new AccessDeniedException("Cliente não autorizado");
		}
		return cliente;
	}
}
