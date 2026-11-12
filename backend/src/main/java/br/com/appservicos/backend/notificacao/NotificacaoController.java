package br.com.appservicos.backend.notificacao;

import br.com.appservicos.backend.notificacao.dto.PaginaNotificacoesResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/prestadores/me/notificacoes")
public class NotificacaoController {
    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<PaginaNotificacoesResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "5") int tamanho) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.listar(jwt.getSubject(), pagina, tamanho));
    }

    @PostMapping("/{id}/lida")
    public ResponseEntity<Void> ler(@AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        service.marcarLida(jwt.getSubject(), id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
