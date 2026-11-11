package br.com.appservicos.backend.chat;

import br.com.appservicos.backend.api.CampoInvalidoException;
import br.com.appservicos.backend.api.ConflitoException;
import br.com.appservicos.backend.chat.dto.ChatResponse;
import br.com.appservicos.backend.chat.dto.EnviarMensagemRequest;
import br.com.appservicos.backend.chat.dto.MensagemResponse;
import br.com.appservicos.backend.chat.dto.PaginaMensagensResponse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;

@Service
public class ChatService {
    private final ChatAcessoService acesso;
    private final MensagemRepository mensagens;
    private final EntityManager em;
    private final Clock clock;
    private final ApplicationEventPublisher eventos;

    public ChatService(
            ChatAcessoService acesso,
            MensagemRepository mensagens,
            EntityManager em,
            Clock clock,
            ApplicationEventPublisher eventos) {
        this.acesso = acesso;
        this.mensagens = mensagens;
        this.em = em;
        this.clock = clock;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public ChatResponse contexto(String usuario, Integer id) {
        return ChatResponse.from(acesso.buscar(usuario, id), acesso.usuario(usuario));
    }

    @Transactional
    public MensagemResponse enviar(String usuario, Integer id, EnviarMensagemRequest request) {
        var a = acesso.buscar(usuario, id);
        em.refresh(a, LockModeType.PESSIMISTIC_WRITE);
        acesso.exigirConfirmado(a);
        var remetente = acesso.usuario(usuario);
        var existente =
                mensagens.findByAgendamentoIdAndRemetenteIdAndIdEnvio(
                        id, remetente.getId(), request.idEnvio());
        if (existente.isPresent()) {
            if (!existente.get().getConteudo().equals(request.conteudo()))
                throw new ConflitoException("Este envio já foi usado para outra mensagem");
            return MensagemResponse.from(existente.get());
        }
        var agora = Instant.now(clock);
        var mensagem =
                mensagens.saveAndFlush(
                        new Mensagem(a, remetente, request.idEnvio(), request.conteudo(), agora));
        a.iniciarConversa(agora);
        var resposta = MensagemResponse.from(mensagem);
        eventos.publishEvent(new MensagemPublicada(resposta));
        return resposta;
    }

    @Transactional(readOnly = true)
    public PaginaMensagensResponse listar(
            String usuario, Integer id, Integer antesDe, Integer depoisDe, int tamanho) {
        acesso.buscar(usuario, id);
        if (tamanho < 1 || tamanho > 100)
            throw new CampoInvalidoException("tamanho", "O tamanho deve estar entre 1 e 100");
        if (antesDe != null && antesDe < 1)
            throw new CampoInvalidoException("antesDe", "Informe um identificador válido");
        if (depoisDe != null && depoisDe < 0)
            throw new CampoInvalidoException("depoisDe", "Informe um identificador válido");
        if (antesDe != null && depoisDe != null)
            throw new CampoInvalidoException(
                    "antesDe", "Use apenas uma direção para consultar o histórico");
        var resultado =
                depoisDe == null
                        ? mensagens.anteriores(id, antesDe, PageRequest.of(0, tamanho + 1))
                        : mensagens.posteriores(id, depoisDe, PageRequest.of(0, tamanho + 1));
        boolean temMais = resultado.size() > tamanho;
        var itens =
                new ArrayList<>(
                        resultado.subList(0, Math.min(tamanho, resultado.size())).stream()
                                .map(MensagemResponse::from)
                                .toList());
        Integer cursor = temMais ? itens.get(itens.size() - 1).idMensagem() : null;
        if (depoisDe == null) Collections.reverse(itens);
        return new PaginaMensagensResponse(itens, temMais, cursor);
    }
}
