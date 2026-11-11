package br.com.appservicos.backend.oportunidade;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.appservicos.backend.oportunidade.dto.OportunidadeResponse;
import br.com.appservicos.backend.pedido.dto.PaginaPedidosResponse;

@RestController
@RequestMapping("/prestadores/me/oportunidades")
public class OportunidadeController {

	private final OportunidadeService service;

	public OportunidadeController(OportunidadeService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<PaginaPedidosResponse> listar(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int tamanho) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(service.listar(jwt.getSubject(), pagina, tamanho));
	}

	@GetMapping("/{idPedido}")
	public ResponseEntity<OportunidadeResponse> buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer idPedido) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.buscar(jwt.getSubject(), idPedido));
	}
}
