package br.com.appservicos.backend.chat;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class ChatSocketSeguranca {
    private static final Pattern DESTINO =
            Pattern.compile("^/topic/agendamentos/([1-9][0-9]{0,9})/mensagens$");
    private final Map<String, Jwt> sessoes = new ConcurrentHashMap<>();
    private final JwtDecoder decoder;
    private final ChatAcessoService acesso;

    public ChatSocketSeguranca(JwtDecoder decoder, ChatAcessoService acesso) {
        this.decoder = decoder;
        this.acesso = acesso;
    }

    public Message<?> entrada(Message<?> message, MessageChannel channel) {
        var headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null) throw negado();
        var authorization = headers.getFirstNativeHeader("Authorization");
        new java.util.ArrayList<>(headers.toNativeHeaderMap().keySet())
                .stream()
                        .filter(name -> name.equalsIgnoreCase("Authorization"))
                        .forEach(headers::removeNativeHeader);
        var command = headers.getCommand();
        var session = headers.getSessionId();
        if (session == null) throw negado();
        if (command == StompCommand.DISCONNECT) {
            sessoes.remove(session);
            return message;
        }
        Jwt jwt;
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            if (authorization == null || !authorization.startsWith("Bearer ")) throw negado();
            jwt = decoder.decode(authorization.substring(7));
            validarConta(jwt);
            headers.setUser(
                    new JwtAuthenticationToken(
                            jwt,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + jwt.getClaimAsString("tipoUsuario")))));
            sessoes.put(session, jwt);
            return message;
        }
        jwt = token(session);
        if (command == StompCommand.SUBSCRIBE) {
            if ("/user/queue/notificacoes".equals(headers.getDestination())) {
                if (!"prestador".equals(jwt.getClaimAsString("tipoUsuario"))) throw negado();
            } else acesso.buscar(jwt.getSubject(), agendamento(headers.getDestination()));
            if (headers.getFirstNativeHeader("selector") != null) throw negado();
        } else if (command != StompCommand.UNSUBSCRIBE && command != null) {
            throw negado();
        }
        return message;
    }

    public Message<?> saida(Message<?> message, MessageChannel channel) {
        var headers = StompHeaderAccessor.wrap(message);
        if (headers.getMessageType() != SimpMessageType.MESSAGE) return message;
        try {
            var jwt = token(headers.getSessionId());
            if (("/queue/notificacoes-user" + headers.getSessionId())
                    .equals(headers.getDestination())) {
                if (!"prestador".equals(jwt.getClaimAsString("tipoUsuario"))) return null;
            } else acesso.buscar(jwt.getSubject(), agendamento(headers.getDestination()));
            return message;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Jwt token(String session) {
        var stored = session == null ? null : sessoes.get(session);
        if (stored == null) throw negado();
        var jwt = decoder.decode(stored.getTokenValue());
        validarConta(jwt);
        return jwt;
    }

    private void validarConta(Jwt jwt) {
        var usuario = acesso.usuario(jwt.getSubject());
        if (!usuario.getTipoUsuario().equals(jwt.getClaimAsString("tipoUsuario"))) throw negado();
    }

    private Integer agendamento(String destino) {
        if (destino == null) throw negado();
        var match = DESTINO.matcher(destino);
        if (!match.matches()) throw negado();
        try {
            return Integer.valueOf(match.group(1));
        } catch (NumberFormatException ex) {
            throw negado();
        }
    }

    private AccessDeniedException negado() {
        return new AccessDeniedException("Conexão ou conversa não autorizada");
    }

    @EventListener
    public void desconectado(SessionDisconnectEvent evento) {
        sessoes.remove(evento.getSessionId());
    }
}
