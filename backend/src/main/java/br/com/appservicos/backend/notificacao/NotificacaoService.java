package br.com.appservicos.backend.notificacao;

import br.com.appservicos.backend.agendamento.Agendamento;
import br.com.appservicos.backend.api.CampoInvalidoException;
import br.com.appservicos.backend.api.RecursoNaoEncontradoException;
import br.com.appservicos.backend.notificacao.dto.NotificacaoResponse;
import br.com.appservicos.backend.notificacao.dto.PaginaNotificacoesResponse;
import br.com.appservicos.backend.prestador.PrestadorAutenticadoService;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class NotificacaoService {
    private final NotificacaoRepository notificacoes;
    private final PrestadorAutenticadoService prestadores;
    private final ApplicationEventPublisher eventos;
    private final Clock clock;

    public NotificacaoService(
            NotificacaoRepository notificacoes,
            PrestadorAutenticadoService prestadores,
            ApplicationEventPublisher eventos,
            Clock clock) {
        this.notificacoes = notificacoes;
        this.prestadores = prestadores;
        this.eventos = eventos;
        this.clock = clock;
    }

    @Transactional
    public void contratacaoConfirmada(Agendamento a, Instant agora) {
        var n = notificacoes.saveAndFlush(new Notificacao(a, agora));
        eventos.publishEvent(
                new NotificacaoPublicada(
                        n.getUsuario().getId().toString(), NotificacaoResponse.from(n)));
    }

    @Transactional(readOnly = true)
    public PaginaNotificacoesResponse listar(String usuario, int pagina, int tamanho) {
        var p = prestadores.buscar(usuario);
        if (pagina < 0)
            throw new CampoInvalidoException("pagina", "A página deve ser maior ou igual a zero");
        if (tamanho < 1 || tamanho > 50)
            throw new CampoInvalidoException("tamanho", "O tamanho deve estar entre 1 e 50");
        return PaginaNotificacoesResponse.from(
                notificacoes.findByUsuarioIdAndLidaEmIsNull(
                        p.getUsuario().getId(),
                        PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id"))));
    }

    @Transactional
    public void marcarLida(String usuario, Integer id) {
        var p = prestadores.buscar(usuario);
        notificacoes
                .findByIdAndUsuarioId(id, p.getUsuario().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Notificação não encontrada"));
        notificacoes.marcarLida(id, p.getUsuario().getId(), Instant.now(clock));
    }
}
