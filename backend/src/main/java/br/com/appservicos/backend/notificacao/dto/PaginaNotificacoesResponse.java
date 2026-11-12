package br.com.appservicos.backend.notificacao.dto;

import br.com.appservicos.backend.notificacao.Notificacao;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaNotificacoesResponse(
        List<NotificacaoResponse> itens,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas) {
    public static PaginaNotificacoesResponse from(Page<Notificacao> page) {
        return new PaginaNotificacoesResponse(
                page.getContent().stream().map(NotificacaoResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
