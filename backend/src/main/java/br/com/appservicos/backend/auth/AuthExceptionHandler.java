package br.com.appservicos.backend.auth;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
