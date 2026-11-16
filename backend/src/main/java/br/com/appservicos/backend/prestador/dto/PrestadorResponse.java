package br.com.appservicos.backend.prestador.dto;

import java.util.Comparator;
import java.util.List;

import br.com.appservicos.backend.categoria.CategoriaServico;
import br.com.appservicos.backend.categoria.dto.CategoriaResponse;
import br.com.appservicos.backend.prestador.Prestador;

public record PrestadorResponse(Integer idPrestador, String nome, String statusAprovacao, String cidade,
		String uf, List<String> bairros, List<CategoriaResponse> categorias, String motivoReprovacao) {

	public static PrestadorResponse from(Prestador prestador) {
		return from(prestador, null);
	}

	public static PrestadorResponse from(Prestador prestador, String motivoReprovacao) {
		return new PrestadorResponse(prestador.getId(), prestador.getNome(), prestador.getStatusAprovacao(),
				prestador.getCidade(), prestador.getUf(),
				prestador.getBairros().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(),
				prestador.getCategorias().stream()
						.sorted(Comparator.comparing(CategoriaServico::getNome))
						.map(CategoriaResponse::from).toList(), motivoReprovacao);
	}
}
