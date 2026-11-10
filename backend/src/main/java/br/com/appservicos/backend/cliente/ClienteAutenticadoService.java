package br.com.appservicos.backend.cliente;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteAutenticadoService {

	private final ClienteRepository clientes;

	public ClienteAutenticadoService(ClienteRepository clientes) {
		this.clientes = clientes;
	}

	@Transactional(readOnly = true)
	public Cliente buscar(String idUsuario) {
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
