package br.com.appservicos.backend.administracao.dto;

import br.com.appservicos.backend.prestador.Prestador;
import br.com.appservicos.backend.prestador.dto.PrestadorResponse;
import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaPrestadoresResponse(List<ResumoPrestador> itens, int pagina, int tamanho,
        long totalItens, int totalPaginas) {
    public record ResumoPrestador(PrestadorResponse perfil, boolean ativo) {}

    public static PaginaPrestadoresResponse from(Page<Prestador> pagina) {
        return new PaginaPrestadoresResponse(pagina.map(p ->
                new ResumoPrestador(PrestadorResponse.from(p), p.getUsuario().isAtivo())).getContent(),
                pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages());
    }
}
