package br.com.appservicos.backend.endereco;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.cliente.Cliente;
import br.com.appservicos.backend.cliente.ClienteAutenticadoService;
import br.com.appservicos.backend.endereco.dto.CadastroEnderecoRequest;
import br.com.appservicos.backend.endereco.dto.EnderecoClienteResponse;

@Service
public class EnderecoClienteService {

	private final ClienteAutenticadoService clientes;
	private final EnderecoClienteRepository enderecos;

	public EnderecoClienteService(ClienteAutenticadoService clientes, EnderecoClienteRepository enderecos) {
		this.clientes = clientes;
		this.enderecos = enderecos;
	}

	@Transactional
	public EnderecoClienteResponse cadastrar(String idUsuario, CadastroEnderecoRequest request) {
		Cliente cliente = clientes.buscar(idUsuario);
		EnderecoCliente endereco = enderecos.saveAndFlush(new EnderecoCliente(cliente, request.cep(),
				request.logradouro(), request.numero(), request.complemento(), request.bairro(), request.cidade(), request.uf()));
		return EnderecoClienteResponse.from(endereco);
	}

	@Transactional(readOnly = true)
	public List<EnderecoClienteResponse> listar(String idUsuario) {
		Cliente cliente = clientes.buscar(idUsuario);
		return enderecos.findByClienteIdOrderByIdAsc(cliente.getId()).stream()
				.map(EnderecoClienteResponse::from).toList();
	}

}
