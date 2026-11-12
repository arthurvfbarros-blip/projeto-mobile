package br.com.appservicos.backend.notificacao.dto;

import br.com.appservicos.backend.notificacao.Notificacao;

import java.time.Instant;

public record NotificacaoResponse(
        Integer idNotificacao,
        Integer idAgendamento,
        String tipo,
        String mensagem,
        Instant criadaEm,
        Instant lidaEm) {
    public static NotificacaoResponse from(Notificacao n) {
        return new NotificacaoResponse(
                n.getId(),
                n.getAgendamento().getId(),
                n.getTipo(),
                n.getMensagem(),
                n.getCriadaEm(),
                n.getLidaEm());
    }
}
