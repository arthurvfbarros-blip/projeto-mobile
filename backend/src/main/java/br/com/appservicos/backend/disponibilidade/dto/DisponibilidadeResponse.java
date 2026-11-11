package br.com.appservicos.backend.disponibilidade.dto;

import br.com.appservicos.backend.disponibilidade.Disponibilidade;

import java.time.LocalDate;
import java.time.LocalTime;

public record DisponibilidadeResponse(
        Integer idDisponibilidade, LocalDate data, LocalTime hora, String status) {
    public static DisponibilidadeResponse from(Disponibilidade d) {
        return new DisponibilidadeResponse(d.getId(), d.getData(), d.getHora(), d.getStatus());
    }
}
