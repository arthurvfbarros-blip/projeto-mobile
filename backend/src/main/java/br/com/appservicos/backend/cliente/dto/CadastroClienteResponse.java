package br.com.appservicos.backend.cliente.dto;

public record CadastroClienteResponse(Integer idCliente, Integer idUsuario, String nome, String email,
		String tipoUsuario) {
}
