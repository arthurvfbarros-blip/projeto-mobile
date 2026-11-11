package br.com.appservicos.backend.oportunidade.dto;

import java.time.Instant;
import java.time.LocalDate;

import br.com.appservicos.backend.categoria.dto.CategoriaResponse;
import br.com.appservicos.backend.pedido.PedidoServico;

public record OportunidadeResponse(Integer idPedido, String titulo, String descricao, CategoriaResponse categoria,
		String status, LocalDate dataDesejada, Instant criadoEm, String bairro, String cidade, String uf) {

	public static OportunidadeResponse from(PedidoServico pedido) {
		var regiao = pedido.getEnderecoPedido();
		return new OportunidadeResponse(pedido.getId(), pedido.getTitulo(), pedido.getDescricao(),
				CategoriaResponse.from(pedido.getCategoria()), pedido.getStatus(), pedido.getDataDesejada(),
				pedido.getCriadoEm(), regiao.getBairro(), regiao.getCidade(), regiao.getUf());
	}
}
