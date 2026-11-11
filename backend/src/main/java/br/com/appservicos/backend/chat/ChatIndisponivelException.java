package br.com.appservicos.backend.chat;

import org.springframework.security.access.AccessDeniedException;

public class ChatIndisponivelException extends AccessDeniedException {
    public ChatIndisponivelException() {
        super("Defina a data e horário do serviço para liberar o chat");
    }
}
