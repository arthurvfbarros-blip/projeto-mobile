package br.com.appservicos.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "Informe o e-mail")
		@Email(message = "Informe um e-mail válido")
		@Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
		String email,
		@NotBlank(message = "Informe a senha")
		@Size(max = 72, message = "A senha deve ter no máximo 72 caracteres")
		String senha) {

	public LoginRequest {
		if (email != null) {
			email = email.strip();
		}
	}

	@Override
	public String toString() {
		return "LoginRequest[credenciais omitidas]";
	}
}
