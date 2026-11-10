package br.com.appservicos.backend.pedido;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.appservicos.backend.pedido.dto.CadastroPedidoRequest;
import br.com.appservicos.backend.pedido.dto.PaginaPedidosResponse;
import br.com.appservicos.backend.pedido.dto.PedidoResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/clientes/me/pedidos")
public class PedidoServicoController {

	private final PedidoServicoService service;

	public PedidoServicoController(PedidoServicoService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<PedidoResponse> criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CadastroPedidoRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
				.body(service.criar(jwt.getSubject(), request));
	}

	@GetMapping
	public ResponseEntity<PaginaPedidosResponse> listar(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int tamanho) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listar(jwt.getSubject(), pagina, tamanho));
	}

	@GetMapping("/{idPedido}")
	public ResponseEntity<PedidoResponse> buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer idPedido) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.buscar(jwt.getSubject(), idPedido));
	}
}
