package br.com.appservicos.backend.agendamento.dto;

import br.com.appservicos.backend.agendamento.Agendamento;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaAgendamentosResponse(
        List<AgendamentoResponse> itens,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas) {
    public static PaginaAgendamentosResponse from(Page<Agendamento> page) {
        return new PaginaAgendamentosResponse(
                page.getContent().stream().map(AgendamentoResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
