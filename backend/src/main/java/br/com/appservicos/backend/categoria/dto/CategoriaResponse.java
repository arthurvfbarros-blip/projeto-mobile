package br.com.appservicos.backend.categoria.dto;

import br.com.appservicos.backend.categoria.CategoriaServico;

public record CategoriaResponse(Integer idCategoria, String nome, String descricao) {

	public static CategoriaResponse from(CategoriaServico categoria) {
		return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao());
	}
}
