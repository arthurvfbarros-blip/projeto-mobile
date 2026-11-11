package br.com.appservicos.backend.disponibilidade.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CadastroDisponibilidadeRequest(
        @NotNull(message = "Informe a data disponível") LocalDate data,
        @NotNull(message = "Informe o horário disponível") LocalTime hora) {}
