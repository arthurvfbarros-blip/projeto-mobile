package br.com.appservicos.backend.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UsuarioResponse usuario) {

	public record UsuarioResponse(Integer id, String email, String tipoUsuario) {
	}
}
