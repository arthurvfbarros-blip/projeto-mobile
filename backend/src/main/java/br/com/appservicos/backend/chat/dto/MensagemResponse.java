package br.com.appservicos.backend.chat.dto;

import br.com.appservicos.backend.chat.Mensagem;

import java.time.Instant;
import java.util.UUID;

public record MensagemResponse(
        Integer idMensagem,
        Integer idAgendamento,
        UUID idEnvio,
        Integer idRemetente,
        String tipoRemetente,
        String nomeRemetente,
        String conteudo,
        Instant criadoEm) {
    public static MensagemResponse from(Mensagem m) {
        var a = m.getAgendamento();
        var c = a.getOrcamento().getPedido().getCliente();
        var p = a.getOrcamento().getPrestador();
        boolean cliente = c.getUsuario().getId().equals(m.getRemetente().getId());
        return new MensagemResponse(
                m.getId(),
                a.getId(),
                m.getIdEnvio(),
                m.getRemetente().getId(),
                cliente ? "cliente" : "prestador",
                cliente ? c.getNome() : p.getNome(),
                m.getConteudo(),
                m.getCriadoEm());
    }
}
