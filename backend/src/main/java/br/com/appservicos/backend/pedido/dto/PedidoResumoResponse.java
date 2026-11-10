package br.com.appservicos.backend.pedido.dto;

import java.time.Instant;
import java.time.LocalDate;

import br.com.appservicos.backend.categoria.dto.CategoriaResponse;
import br.com.appservicos.backend.pedido.PedidoServico;

public record PedidoResumoResponse(Integer idPedido, String titulo, CategoriaResponse categoria,
		String status, LocalDate dataDesejada, Instant criadoEm, String bairro, String cidade, String uf) {

	public static PedidoResumoResponse from(PedidoServico pedido) {
		var endereco = pedido.getEnderecoPedido();
		return new PedidoResumoResponse(pedido.getId(), pedido.getTitulo(), CategoriaResponse.from(pedido.getCategoria()),
				pedido.getStatus(), pedido.getDataDesejada(), pedido.getCriadoEm(), endereco == null ? null : endereco.getBairro(),
				endereco == null ? null : endereco.getCidade(), endereco == null ? null : endereco.getUf());
	}
}
