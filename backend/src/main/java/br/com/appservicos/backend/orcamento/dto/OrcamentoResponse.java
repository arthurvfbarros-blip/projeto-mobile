package br.com.appservicos.backend.orcamento.dto;

import java.math.BigDecimal;
import br.com.appservicos.backend.orcamento.Orcamento;

public record OrcamentoResponse(Integer idOrcamento, Integer idPedido, String tituloPedido,
		PrestadorResumo prestador, BigDecimal valor, Integer prazoDias, String descricao, String status) {
	public record PrestadorResumo(Integer idPrestador, String nome) { }
	public static OrcamentoResponse from(Orcamento orcamento) {
		return new OrcamentoResponse(orcamento.getId(), orcamento.getPedido().getId(), orcamento.getPedido().getTitulo(),
				new PrestadorResumo(orcamento.getPrestador().getId(), orcamento.getPrestador().getNome()),
				orcamento.getValor(), orcamento.getPrazoDias(), orcamento.getDescricao(), orcamento.getStatus());
	}
}
