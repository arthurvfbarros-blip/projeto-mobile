package br.com.appservicos.backend.orcamento;

import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.com.appservicos.backend.orcamento.dto.CadastroOrcamentoRequest;
import br.com.appservicos.backend.orcamento.dto.OrcamentoResponse;
import br.com.appservicos.backend.orcamento.dto.PaginaOrcamentosResponse;
import jakarta.validation.Valid;

@RestController
public class OrcamentoController {
	private static final String ROTA_PRESTADOR = "/prestadores/me/oportunidades/{idPedido}/orcamento";
	private final OrcamentoService service;
	public OrcamentoController(OrcamentoService service) { this.service = service; }

	@PostMapping(ROTA_PRESTADOR)
	public ResponseEntity<OrcamentoResponse> criar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer idPedido,
			@Valid @RequestBody CadastroOrcamentoRequest request) {
		return ResponseEntity.created(URI.create("/prestadores/me/oportunidades/" + idPedido + "/orcamento"))
				.cacheControl(CacheControl.noStore()).body(service.criar(jwt.getSubject(), idPedido, request));
	}

	@GetMapping(ROTA_PRESTADOR)
	public ResponseEntity<OrcamentoResponse> buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer idPedido) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(service.buscarDoPrestador(jwt.getSubject(), idPedido));
	}

	@GetMapping("/clientes/me/pedidos/{idPedido}/orcamentos")
	public ResponseEntity<PaginaOrcamentosResponse> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer idPedido,
			@RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int tamanho) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(service.listarDoCliente(jwt.getSubject(), idPedido, pagina, tamanho));
	}
}
