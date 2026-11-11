package br.com.appservicos.backend.auth;

import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.appservicos.backend.api.ApiExceptionHandler.ErroResponse;
import br.com.appservicos.backend.api.CadastroDuplicadoException;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErroResponse> credenciaisInvalidas(AuthenticationException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ErroResponse("E-mail ou senha inválidos", Map.of()));
	}

	@ExceptionHandler(CadastroDuplicadoException.class)
	public ResponseEntity<ErroResponse> cadastroDuplicado(CadastroDuplicadoException exception) {
		return conflito(exception.getCampo(), exception.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErroResponse> conflitoNoBanco(DataIntegrityViolationException exception) {
		for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
			if (causa instanceof org.hibernate.exception.ConstraintViolationException constraint) {
				String nome = constraint.getConstraintName();
				if (nome == null) continue;
				nome = nome.toLowerCase(Locale.ROOT);
				if (nome.contains("uq_usuario_email")) return conflito("email", "E-mail já cadastrado");
				if (nome.contains("uq_cliente_cpf") || nome.contains("uq_prestador_cpf")) {
					return conflito("cpf", "CPF já cadastrado");
				}
				if (nome.contains("uq_empresa_cnpj")) return conflito("empresa.cnpj", "CNPJ já cadastrado");
			}
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErroResponse("Não foi possível concluir a operação", Map.of()));
	}

	private ResponseEntity<ErroResponse> conflito(String campo, String mensagem) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(mensagem, Map.of(campo, mensagem)));
	}
}
