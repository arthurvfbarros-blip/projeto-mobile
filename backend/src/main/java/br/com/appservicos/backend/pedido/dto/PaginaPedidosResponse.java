package br.com.appservicos.backend.pedido.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import br.com.appservicos.backend.pedido.PedidoServico;

public record PaginaPedidosResponse(List<PedidoResumoResponse> itens, int pagina, int tamanho,
		long totalItens, int totalPaginas) {

	public static PaginaPedidosResponse from(Page<PedidoServico> pedidos) {
		return new PaginaPedidosResponse(pedidos.getContent().stream().map(PedidoResumoResponse::from).toList(),
				pedidos.getNumber(), pedidos.getSize(), pedidos.getTotalElements(), pedidos.getTotalPages());
	}
}
