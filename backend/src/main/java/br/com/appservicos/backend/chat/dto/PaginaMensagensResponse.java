package br.com.appservicos.backend.chat.dto;

import java.util.List;

public record PaginaMensagensResponse(
        List<MensagemResponse> itens, boolean temMais, Integer proximoCursor) {}
