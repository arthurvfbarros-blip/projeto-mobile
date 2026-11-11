package br.com.appservicos.backend.api;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

	public record ErroResponse(String mensagem, Map<String, String> campos) {
	}

	@ExceptionHandler(ConflitoException.class)
	public ResponseEntity<ErroResponse> conflito(ConflitoException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroResponse(exception.getMessage(), Map.of()));
	}

	@ExceptionHandler(CampoInvalidoException.class)
	public ResponseEntity<ErroResponse> campoInvalido(CampoInvalidoException exception) {
		return ResponseEntity.badRequest().body(new ErroResponse("Dados inválidos",
				Map.of(exception.getCampo(), exception.getMessage())));
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ResponseEntity<ErroResponse> recursoNaoEncontrado(RecursoNaoEncontradoException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErroResponse(exception.getMessage(), Map.of()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException exception) {
		return ResponseEntity.badRequest().body(new ErroResponse("Dados inválidos",
				Map.of(exception.getName(), "Informe um número inteiro válido")));
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

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ErroResponse("Acesso não permitido", Map.of()));
	}
}
