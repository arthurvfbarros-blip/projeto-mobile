package br.com.appservicos.backend.auth;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.appservicos.backend.cliente.CadastroDuplicadoException;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

	public record ErroResponse(String mensagem, Map<String, String> campos) {
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErroResponse> credenciaisInvalidas(AuthenticationException exception) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ErroResponse("E-mail ou senha inválidos", Map.of()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> dadosInvalidos(MethodArgumentNotValidException exception) {
		Map<String, String> campos = new TreeMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest().body(new ErroResponse("Dados inválidos", campos));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest().body(new ErroResponse("Corpo da requisição inválido", Map.of()));
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
				if (nome.contains("uq_cliente_cpf")) return conflito("cpf", "CPF já cadastrado");
			}
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErroResponse("Não foi possível concluir a operação", Map.of()));
	}

	private ResponseEntity<ErroResponse> conflito(String campo, String mensagem) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(mensagem, Map.of(campo, mensagem)));
	}
}
