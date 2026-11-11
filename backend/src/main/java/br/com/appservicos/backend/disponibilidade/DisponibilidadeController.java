package br.com.appservicos.backend.disponibilidade;

import br.com.appservicos.backend.disponibilidade.dto.CadastroDisponibilidadeRequest;
import br.com.appservicos.backend.disponibilidade.dto.DisponibilidadeResponse;
import br.com.appservicos.backend.disponibilidade.dto.PaginaDisponibilidadesResponse;

import jakarta.validation.Valid;

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

import java.net.URI;

@RestController
public class DisponibilidadeController {
    private final DisponibilidadeService service;

    public DisponibilidadeController(DisponibilidadeService service) {
        this.service = service;
    }

    @PostMapping("/prestadores/me/disponibilidades")
    public ResponseEntity<DisponibilidadeResponse> criar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CadastroDisponibilidadeRequest request) {
        var data = service.criar(jwt.getSubject(), request);
        return ResponseEntity.created(URI.create("/prestadores/me/disponibilidades"))
                .cacheControl(CacheControl.noStore())
                .body(data);
    }

    @GetMapping("/prestadores/me/disponibilidades")
    public ResponseEntity<PaginaDisponibilidadesResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.listarDoPrestador(jwt.getSubject(), pagina, tamanho));
    }

    @GetMapping("/clientes/me/orcamentos/{idOrcamento}/disponibilidades")
    public ResponseEntity<PaginaDisponibilidadesResponse> listarParaCliente(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer idOrcamento,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.listarParaCliente(jwt.getSubject(), idOrcamento, pagina, tamanho));
    }
}
