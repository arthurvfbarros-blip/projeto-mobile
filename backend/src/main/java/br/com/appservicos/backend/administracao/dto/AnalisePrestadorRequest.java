package br.com.appservicos.backend.administracao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnalisePrestadorRequest(
        @NotBlank(message = "Informe a decisão")
        @Pattern(regexp = "aprovado|reprovado", message = "Selecione aprovado ou reprovado")
        String decisao,
        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres") String motivo) {
    public AnalisePrestadorRequest {
        if (motivo != null) {
            motivo = motivo.strip();
            if (motivo.isEmpty()) motivo = null;
        }
    }
}
