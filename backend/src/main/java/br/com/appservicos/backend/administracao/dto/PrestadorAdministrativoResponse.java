package br.com.appservicos.backend.administracao.dto;

import br.com.appservicos.backend.administracao.AnalisePrestador;
import br.com.appservicos.backend.prestador.Prestador;
import br.com.appservicos.backend.prestador.dto.PrestadorResponse;
import java.time.Instant;

public record PrestadorAdministrativoResponse(PrestadorResponse perfil, String email,
        String telefone, boolean ativo, AnaliseResponse analise) {
    public record AnaliseResponse(String decisao, String motivo, Instant analisadaEm,
            String responsavel) {}

    public static PrestadorAdministrativoResponse from(Prestador p, AnalisePrestador a) {
        return new PrestadorAdministrativoResponse(PrestadorResponse.from(p),
                p.getUsuario().getEmail(), p.getTelefone(), p.getUsuario().isAtivo(),
                a == null ? null : new AnaliseResponse(a.getDecisao(), a.getMotivo(),
                        a.getAnalisadaEm(), a.getAdministrador().getEmail()));
    }
}
