package br.com.appservicos.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EnviarMensagemRequest(
        @NotNull(message = "Identifique o envio da mensagem") UUID idEnvio,
        @Pattern(regexp = "[^\\x00]*", message = "A mensagem contém caracteres inválidos")
                @NotBlank(message = "Digite uma mensagem")
                @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres")
                String conteudo) {
    public EnviarMensagemRequest {
        if (conteudo != null) conteudo = conteudo.strip();
    }
}
