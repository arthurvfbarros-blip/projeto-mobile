package br.com.appservicos.backend.categoria;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.categoria.dto.CategoriaResponse;

@Service
public class CategoriaServicoService {

	private final CategoriaServicoRepository categorias;

	public CategoriaServicoService(CategoriaServicoRepository categorias) {
		this.categorias = categorias;
	}

	@Transactional(readOnly = true)
	public List<CategoriaResponse> listar() {
		return categorias.findAllByOrderByNomeAsc().stream().map(CategoriaResponse::from).toList();
	}
}
