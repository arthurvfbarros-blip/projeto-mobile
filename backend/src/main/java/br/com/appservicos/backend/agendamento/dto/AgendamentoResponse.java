package br.com.appservicos.backend.agendamento.dto;

import br.com.appservicos.backend.agendamento.Agendamento;
import br.com.appservicos.backend.pedido.dto.EnderecoPedidoResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record AgendamentoResponse(
        Integer idAgendamento,
        Integer idPedido,
        Integer idOrcamento,
        String tituloPedido,
        String nomeCliente,
        String nomePrestador,
        BigDecimal valor,
        LocalDate data,
        LocalTime hora,
        String status,
        EnderecoPedidoResponse endereco,
        String situacaoConversa) {
    public static AgendamentoResponse from(Agendamento a) {
        var o = a.getOrcamento();
        var d = a.getDisponibilidade();
        return new AgendamentoResponse(
                a.getId(),
                o.getPedido().getId(),
                o.getId(),
                o.getPedido().getTitulo(),
                o.getPedido().getCliente().getNome(),
                o.getPrestador().getNome(),
                o.getValor(),
                d.getData(),
                d.getHora(),
                a.getStatus(),
                EnderecoPedidoResponse.from(a.getEndereco()),
                a.getSituacaoConversa());
    }
}
