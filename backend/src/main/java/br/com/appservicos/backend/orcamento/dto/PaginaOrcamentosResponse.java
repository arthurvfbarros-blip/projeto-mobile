package br.com.appservicos.backend.orcamento.dto;

import java.util.List;
import org.springframework.data.domain.Page;
import br.com.appservicos.backend.orcamento.Orcamento;

public record PaginaOrcamentosResponse(List<OrcamentoResponse> itens, int pagina, int tamanho,
		long totalItens, int totalPaginas) {
	public static PaginaOrcamentosResponse from(Page<Orcamento> pagina) {
		return new PaginaOrcamentosResponse(pagina.getContent().stream().map(OrcamentoResponse::from).toList(),
				pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
	}
}
