package br.com.appservicos.backend.prestador;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.api.RecursoNaoEncontradoException;

@Service
public class PrestadorAutenticadoService {

	private final PrestadorRepository prestadores;

	public PrestadorAutenticadoService(PrestadorRepository prestadores) {
		this.prestadores = prestadores;
	}

	@Transactional(readOnly = true)
	public Prestador buscar(String idUsuario) {
		Integer id;
		try {
			id = Integer.valueOf(idUsuario);
		} catch (NumberFormatException exception) {
			throw new AccessDeniedException("Prestador não autorizado");
		}
		var prestador = prestadores.findByUsuarioId(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Perfil de prestador não encontrado"));
		var usuario = prestador.getUsuario();
		if (!usuario.isAtivo() || !"prestador".equals(usuario.getTipoUsuario())) {
			throw new AccessDeniedException("Prestador não autorizado");
		}
		return prestador;
	}
}
