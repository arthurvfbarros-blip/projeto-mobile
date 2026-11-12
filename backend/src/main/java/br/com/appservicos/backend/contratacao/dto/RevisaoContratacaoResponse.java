package br.com.appservicos.backend.contratacao.dto;

import br.com.appservicos.backend.agendamento.dto.AgendamentoResponse;

public record RevisaoContratacaoResponse(
        AgendamentoResponse agendamento, boolean podeConfirmar, String motivoBloqueio) {}
