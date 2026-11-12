package br.com.appservicos.backend.contratacao;

import br.com.appservicos.backend.contratacao.dto.ContratacaoResponse;
import br.com.appservicos.backend.contratacao.dto.RevisaoContratacaoResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContratacaoController {
    private final ContratacaoService service;

    public ContratacaoController(ContratacaoService service) {
        this.service = service;
    }

    @GetMapping("/clientes/me/agendamentos/{id}/contratacao/revisao")
    public ResponseEntity<RevisaoContratacaoResponse> revisar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.revisar(jwt.getSubject(), id));
    }

    @PostMapping("/clientes/me/agendamentos/{id}/contratacao")
    public ResponseEntity<ContratacaoResponse> confirmar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.confirmar(jwt.getSubject(), id));
    }

    @GetMapping("/clientes/me/agendamentos/{id}/contratacao")
    public ResponseEntity<ContratacaoResponse> buscarCliente(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.buscar(jwt.getSubject(), id, false));
    }

    @GetMapping("/prestadores/me/agendamentos/{id}/contratacao")
    public ResponseEntity<ContratacaoResponse> buscarPrestador(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.buscar(jwt.getSubject(), id, true));
    }
}
