package br.com.appservicos.backend.notificacao;

import br.com.appservicos.backend.notificacao.dto.NotificacaoResponse;

public record NotificacaoPublicada(String idUsuario, NotificacaoResponse notificacao) {}
