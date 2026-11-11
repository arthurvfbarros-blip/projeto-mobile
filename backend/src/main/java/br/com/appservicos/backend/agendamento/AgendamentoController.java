package br.com.appservicos.backend.agendamento;

import br.com.appservicos.backend.agendamento.dto.AgendamentoResponse;
import br.com.appservicos.backend.agendamento.dto.CadastroAgendamentoRequest;
import br.com.appservicos.backend.agendamento.dto.PaginaAgendamentosResponse;

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
public class AgendamentoController {
    private final AgendamentoService service;

    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    @PostMapping("/clientes/me/agendamentos")
    public ResponseEntity<AgendamentoResponse> criar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CadastroAgendamentoRequest request) {
        var data = service.criar(jwt.getSubject(), request);
        return ResponseEntity.created(
                        URI.create("/clientes/me/agendamentos/" + data.idAgendamento()))
                .cacheControl(CacheControl.noStore())
                .body(data);
    }

    @GetMapping("/clientes/me/agendamentos")
    public ResponseEntity<PaginaAgendamentosResponse> listarCliente(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "proximos") String periodo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.listar(jwt.getSubject(), false, periodo, pagina, tamanho));
    }

    @GetMapping("/prestadores/me/agendamentos")
    public ResponseEntity<PaginaAgendamentosResponse> listarPrestador(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "proximos") String periodo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.listar(jwt.getSubject(), true, periodo, pagina, tamanho));
    }

    @GetMapping("/clientes/me/agendamentos/{id}")
    public ResponseEntity<AgendamentoResponse> buscarCliente(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.buscar(jwt.getSubject(), id, false));
    }

    @GetMapping("/prestadores/me/agendamentos/{id}")
    public ResponseEntity<AgendamentoResponse> buscarPrestador(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Integer id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.buscar(jwt.getSubject(), id, true));
    }
}
