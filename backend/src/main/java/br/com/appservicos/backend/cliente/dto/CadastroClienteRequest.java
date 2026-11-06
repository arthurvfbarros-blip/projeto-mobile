package br.com.appservicos.backend.cliente.dto;

import org.hibernate.validator.constraints.br.CPF;

import br.com.appservicos.backend.validacao.Utf8Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroClienteRequest(
		@NotBlank(message = "Informe seu nome")
		@Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
		String nome,
		@NotBlank(message = "Informe o e-mail")
		@Email(message = "Informe um e-mail válido")
		@Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
		String email,
		@NotBlank(message = "Informe o celular")
		@Pattern(regexp = "[0-9]{10,11}", message = "Informe o celular com DDD e 10 ou 11 dígitos")
		String celular,
		@NotBlank(message = "Informe o CPF")
		@Pattern(regexp = "[0-9]{11}", message = "Informe um CPF válido com 11 dígitos")
		@CPF(message = "Informe um CPF válido")
		String cpf,
		@NotBlank(message = "Informe a senha")
		@Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres")
		@Utf8Size(max = 72, message = "A senha deve ter no máximo 72 bytes UTF-8")
		String senha) {

	public CadastroClienteRequest {
		if (nome != null) nome = nome.strip();
		if (email != null) email = email.strip();
		if (cpf != null) cpf = cpf.strip().replace(".", "").replace("-", "");
		if (celular != null) celular = celular.strip().replaceAll("[()\\s-]", "");
	}

	@Override
	public String toString() {
		return "CadastroClienteRequest[dados pessoais e credenciais omitidos]";
	}
}
