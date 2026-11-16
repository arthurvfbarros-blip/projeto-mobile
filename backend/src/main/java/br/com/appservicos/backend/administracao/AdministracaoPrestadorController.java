package br.com.appservicos.backend.administracao;

import br.com.appservicos.backend.administracao.dto.AnalisePrestadorRequest;
import br.com.appservicos.backend.administracao.dto.PaginaPrestadoresResponse;
import br.com.appservicos.backend.administracao.dto.PrestadorAdministrativoResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
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

@RestController
@RequestMapping("/admin/prestadores")
public class AdministracaoPrestadorController {
    private final AdministracaoPrestadorService service;

    public AdministracaoPrestadorController(AdministracaoPrestadorService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PaginaPrestadoresResponse> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "pendente") String status,
            @RequestParam(defaultValue = "0") int pagina, @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.listar(jwt.getSubject(), status, pagina, tamanho));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrestadorAdministrativoResponse> buscar(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.buscar(jwt.getSubject(), id));
    }

    @PostMapping("/{id}/analise")
    public ResponseEntity<PrestadorAdministrativoResponse> analisar(@AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id, @Valid @RequestBody AnalisePrestadorRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.analisar(jwt.getSubject(), id, request));
    }
}
