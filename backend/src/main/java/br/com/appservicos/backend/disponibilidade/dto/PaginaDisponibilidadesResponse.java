package br.com.appservicos.backend.disponibilidade.dto;

import br.com.appservicos.backend.disponibilidade.Disponibilidade;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaDisponibilidadesResponse(
        List<DisponibilidadeResponse> itens,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas) {
    public static PaginaDisponibilidadesResponse from(Page<Disponibilidade> page) {
        return new PaginaDisponibilidadesResponse(
                page.getContent().stream().map(DisponibilidadeResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
