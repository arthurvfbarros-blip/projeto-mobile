package br.com.appservicos.backend.pedido.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CadastroPedidoRequest(
		@NotNull(message = "Selecione uma categoria") @Positive(message = "Selecione uma categoria válida")
		Integer idCategoria,
		@NotNull(message = "Selecione um endereço") @Positive(message = "Selecione um endereço válido")
		Integer idEndereco,
		@NotBlank(message = "Informe o título do pedido")
		@Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
		String titulo,
		@NotBlank(message = "Descreva o serviço necessário")
		@Size(min = 20, max = 5000, message = "A descrição deve ter entre 20 e 5000 caracteres")
		String descricao,
		@NotNull(message = "Informe a data desejada") LocalDate dataDesejada) {

	public CadastroPedidoRequest {
		if (titulo != null) titulo = titulo.strip();
		if (descricao != null) descricao = descricao.strip();
	}
}
