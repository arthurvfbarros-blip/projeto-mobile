package br.com.appservicos.backend.agendamento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CadastroAgendamentoRequest(
        @NotNull(message = "Selecione um orçamento")
                @Positive(message = "Selecione um orçamento válido")
                Integer idOrcamento,
        @NotNull(message = "Selecione um horário")
                @Positive(message = "Selecione um horário válido")
                Integer idDisponibilidade,
        @NotNull(message = "Confirme o endereço do serviço")
                @Positive(message = "Selecione um endereço válido")
                Integer idEndereco) {}
