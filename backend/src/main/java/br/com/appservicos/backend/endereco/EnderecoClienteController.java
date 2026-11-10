package br.com.appservicos.backend.endereco;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.appservicos.backend.endereco.dto.CadastroEnderecoRequest;
import br.com.appservicos.backend.endereco.dto.EnderecoClienteResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/clientes/me/enderecos")
public class EnderecoClienteController {

	private final EnderecoClienteService service;

	public EnderecoClienteController(EnderecoClienteService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<EnderecoClienteResponse> cadastrar(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CadastroEnderecoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
				.body(service.cadastrar(jwt.getSubject(), request));
	}

	@GetMapping
	public ResponseEntity<List<EnderecoClienteResponse>> listar(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listar(jwt.getSubject()));
	}
}
