package br.com.appservicos.backend.endereco.dto;

import br.com.appservicos.backend.endereco.EnderecoCliente;

public record EnderecoClienteResponse(Integer idEndereco, String cep, String logradouro, String numero,
		String complemento, String bairro, String cidade, String uf) {

	public static EnderecoClienteResponse from(EnderecoCliente endereco) {
		return new EnderecoClienteResponse(endereco.getId(), endereco.getCep(), endereco.getLogradouro(),
				endereco.getNumero(), endereco.getComplemento(), endereco.getBairro(), endereco.getCidade(), endereco.getUf());
	}
}
