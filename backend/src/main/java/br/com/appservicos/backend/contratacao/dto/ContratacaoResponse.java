package br.com.appservicos.backend.contratacao.dto;

import br.com.appservicos.backend.contratacao.Contratacao;

import java.math.BigDecimal;
import java.time.Instant;

public record ContratacaoResponse(
        Integer idContratacao,
        Integer idAgendamento,
        Integer idPedido,
        BigDecimal valor,
        Instant confirmadaEm) {
    public static ContratacaoResponse from(Contratacao c) {
        return c == null
                ? null
                : new ContratacaoResponse(
                        c.getId(),
                        c.getAgendamento().getId(),
                        c.getAgendamento().getOrcamento().getPedido().getId(),
                        c.getValor(),
                        c.getConfirmadaEm());
    }
}
