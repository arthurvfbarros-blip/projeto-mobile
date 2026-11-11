package br.com.appservicos.backend.chat;

import br.com.appservicos.backend.chat.dto.ChatResponse;
import br.com.appservicos.backend.chat.dto.EnviarMensagemRequest;
import br.com.appservicos.backend.chat.dto.MensagemResponse;
import br.com.appservicos.backend.chat.dto.PaginaMensagensResponse;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agendamentos/{id}/chat")
public class ChatController {
    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @ExceptionHandler(ChatIndisponivelException.class)
    public ResponseEntity<br.com.appservicos.backend.api.ApiExceptionHandler.ErroResponse>
            indisponivel(ChatIndisponivelException exception) {
        return ResponseEntity.status(403)
                .body(
                        new br.com.appservicos.backend.api.ApiExceptionHandler.ErroResponse(
                                exception.getMessage(), java.util.Map.of()));
    }

    @GetMapping
    public ResponseEntity<ChatResponse> contexto(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.contexto(jwt.getSubject(), id));
    }

    @GetMapping("/mensagens")
    public ResponseEntity<PaginaMensagensResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @RequestParam(required = false) Integer antesDe,
            @RequestParam(required = false) Integer depoisDe,
            @RequestParam(defaultValue = "30") int tamanho) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.listar(jwt.getSubject(), id, antesDe, depoisDe, tamanho));
    }

    @PostMapping("/mensagens")
    public ResponseEntity<MensagemResponse> enviar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody EnviarMensagemRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.enviar(jwt.getSubject(), id, request));
    }
}
