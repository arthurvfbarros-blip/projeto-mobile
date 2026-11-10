package br.com.appservicos.backend.pedido.dto;

import java.time.Instant;
import java.time.LocalDate;

import br.com.appservicos.backend.categoria.dto.CategoriaResponse;
import br.com.appservicos.backend.pedido.PedidoServico;

public record PedidoResponse(Integer idPedido, String titulo, String descricao, CategoriaResponse categoria,
		String status, LocalDate dataDesejada, Instant criadoEm, EnderecoPedidoResponse endereco) {

	public static PedidoResponse from(PedidoServico pedido) {
		return new PedidoResponse(pedido.getId(), pedido.getTitulo(), pedido.getDescricao(),
				CategoriaResponse.from(pedido.getCategoria()), pedido.getStatus(), pedido.getDataDesejada(),
				pedido.getCriadoEm(), EnderecoPedidoResponse.from(pedido.getEnderecoPedido()));
	}
}
