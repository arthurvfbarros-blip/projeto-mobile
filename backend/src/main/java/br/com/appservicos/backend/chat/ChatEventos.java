package br.com.appservicos.backend.chat;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ChatEventos {
    private final SimpMessagingTemplate broker;

    public ChatEventos(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    @Async("chatPublicacaoExecutor")
    @TransactionalEventListener
    public void publicar(MensagemPublicada evento) {
        var m = evento.mensagem();
        broker.convertAndSend("/topic/agendamentos/" + m.idAgendamento() + "/mensagens", m);
    }
}
