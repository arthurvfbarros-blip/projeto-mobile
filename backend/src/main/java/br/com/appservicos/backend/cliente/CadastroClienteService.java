package br.com.appservicos.backend.cliente;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.cliente.dto.CadastroClienteRequest;
import br.com.appservicos.backend.cliente.dto.CadastroClienteResponse;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;

@Service
public class CadastroClienteService {

	private final UsuarioRepository usuarios;
	private final ClienteRepository clientes;
	private final PasswordEncoder passwordEncoder;

	public CadastroClienteService(UsuarioRepository usuarios, ClienteRepository clientes, PasswordEncoder passwordEncoder) {
		this.usuarios = usuarios;
		this.clientes = clientes;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public CadastroClienteResponse cadastrar(CadastroClienteRequest request) {
		if (usuarios.existsByEmail(request.email())) {
			throw new CadastroDuplicadoException("email", "E-mail já cadastrado");
		}
		if (clientes.existsByCpf(request.cpf())) {
			throw new CadastroDuplicadoException("cpf", "CPF já cadastrado");
		}

		Usuario usuario = usuarios.save(new Usuario(request.email(), passwordEncoder.encode(request.senha()), "cliente", true));
		Cliente cliente = clientes.saveAndFlush(new Cliente(usuario, request.nome(), request.cpf(), request.celular()));
		return new CadastroClienteResponse(cliente.getId(), usuario.getId(), cliente.getNome(),
				usuario.getEmail(), usuario.getTipoUsuario());
	}
}
