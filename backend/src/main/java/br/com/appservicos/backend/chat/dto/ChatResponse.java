package br.com.appservicos.backend.chat.dto;

import br.com.appservicos.backend.agendamento.Agendamento;
import br.com.appservicos.backend.contratacao.dto.ContratacaoResponse;
import br.com.appservicos.backend.pedido.dto.EnderecoPedidoResponse;
import br.com.appservicos.backend.usuario.Usuario;

public record ChatResponse(
        Integer idAgendamento,
        String tituloPedido,
        String situacaoConversa,
        Contato cliente,
        Contato prestador,
        EnderecoPedidoResponse endereco,
        Integer idUsuarioAtual,
        ContratacaoResponse contratacao) {
    public record Contato(String nome, String telefone) {}

    public static ChatResponse from(Agendamento a, Usuario usuario) {
        var o = a.getOrcamento();
        var c = o.getPedido().getCliente();
        var p = o.getPrestador();
        return new ChatResponse(
                a.getId(),
                o.getPedido().getTitulo(),
                a.getSituacaoConversa(),
                new Contato(c.getNome(), c.getTelefone()),
                new Contato(p.getNome(), p.getTelefone()),
                EnderecoPedidoResponse.from(a.getEndereco()),
                usuario.getId(),
                ContratacaoResponse.from(a.getContratacao()));
    }
}
