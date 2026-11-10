package br.com.appservicos.backend.pedido.dto;

import br.com.appservicos.backend.pedido.EnderecoPedido;

public record EnderecoPedidoResponse(String cep, String logradouro, String numero, String complemento,
		String bairro, String cidade, String uf) {

	public static EnderecoPedidoResponse from(EnderecoPedido endereco) {
		if (endereco == null) return null;
		return new EnderecoPedidoResponse(endereco.getCep(), endereco.getLogradouro(), endereco.getNumero(),
				endereco.getComplemento(), endereco.getBairro(), endereco.getCidade(), endereco.getUf());
	}
}
