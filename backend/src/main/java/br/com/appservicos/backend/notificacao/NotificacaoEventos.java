package br.com.appservicos.backend.notificacao;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificacaoEventos {
    private final SimpMessagingTemplate broker;

    public NotificacaoEventos(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    @Async("chatPublicacaoExecutor")
    @TransactionalEventListener
    public void publicar(NotificacaoPublicada evento) {
        broker.convertAndSendToUser(
                evento.idUsuario(), "/queue/notificacoes", evento.notificacao());
    }
}
