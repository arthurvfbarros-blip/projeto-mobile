package br.com.appservicos.backend.auth;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.appservicos.backend.auth.dto.LoginRequest;
import br.com.appservicos.backend.auth.dto.LoginResponse;
import br.com.appservicos.backend.cliente.CadastroClienteService;
import br.com.appservicos.backend.cliente.dto.CadastroClienteRequest;
import br.com.appservicos.backend.cliente.dto.CadastroClienteResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthService service;
	private final CadastroClienteService cadastroClientes;

	public AuthController(AuthService service, CadastroClienteService cadastroClientes) {
		this.service = service;
		this.cadastroClientes = cadastroClientes;
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.login(request));
	}

	@PostMapping("/cadastro/cliente")
	public ResponseEntity<CadastroClienteResponse> cadastrarCliente(@Valid @RequestBody CadastroClienteRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
				.body(cadastroClientes.cadastrar(request));
	}
}
