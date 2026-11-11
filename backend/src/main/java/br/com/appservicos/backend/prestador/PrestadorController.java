package br.com.appservicos.backend.prestador;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.appservicos.backend.prestador.dto.PrestadorResponse;

@RestController
@RequestMapping("/prestadores/me")
public class PrestadorController {

	private final PrestadorService service;

	public PrestadorController(PrestadorService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<PrestadorResponse> consultarPerfil(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultarPerfil(jwt.getSubject()));
	}
}
