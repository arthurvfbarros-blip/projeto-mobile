package br.com.appservicos.backend.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.appservicos.backend.agendamento.Agendamento;
import br.com.appservicos.backend.agendamento.AgendamentoRepository;
import br.com.appservicos.backend.auth.TokenService;
import br.com.appservicos.backend.categoria.CategoriaServico;
import br.com.appservicos.backend.categoria.CategoriaServicoRepository;
import br.com.appservicos.backend.cliente.Cliente;
import br.com.appservicos.backend.cliente.ClienteRepository;
import br.com.appservicos.backend.disponibilidade.Disponibilidade;
import br.com.appservicos.backend.disponibilidade.DisponibilidadeRepository;
import br.com.appservicos.backend.endereco.EnderecoCliente;
import br.com.appservicos.backend.endereco.EnderecoClienteRepository;
import br.com.appservicos.backend.orcamento.Orcamento;
import br.com.appservicos.backend.orcamento.OrcamentoRepository;
import br.com.appservicos.backend.pedido.PedidoServico;
import br.com.appservicos.backend.pedido.PedidoServicoRepository;
import br.com.appservicos.backend.prestador.Prestador;
import br.com.appservicos.backend.prestador.PrestadorRepository;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChatIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired ObjectMapper json;
    @Autowired TokenService tokens;
    @Autowired UsuarioRepository usuarios;
    @Autowired ClienteRepository clientes;
    @Autowired PrestadorRepository prestadores;
    @Autowired CategoriaServicoRepository categorias;
    @Autowired EnderecoClienteRepository enderecos;
    @Autowired PedidoServicoRepository pedidos;
    @Autowired OrcamentoRepository orcamentos;
    @Autowired DisponibilidadeRepository disponibilidades;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired ChatSocketSeguranca socket;
    @Autowired JwtEncoder encoder;
    private Usuario cliente, prestador, outro;
    private Agendamento agendamento;
    private String tc, tp;

    @BeforeEach
    void preparar() {
        var categoria = categorias.saveAndFlush(new CategoriaServico("Elétrica chat", null));
        cliente =
                usuarios.saveAndFlush(
                        new Usuario("chat-cliente@example.invalid", "hash", "cliente", true));
        var c =
                clientes.saveAndFlush(
                        new Cliente(cliente, "Cliente Chat", "52998224725", "11911111111"));
        prestador =
                usuarios.saveAndFlush(
                        new Usuario("chat-prestador@example.invalid", "hash", "prestador", true));
        var p =
                prestadores.saveAndFlush(
                        new Prestador(
                                prestador,
                                "Prestador Chat",
                                "11144477735",
                                "11922222222",
                                "São Paulo",
                                "SP",
                                List.of(categoria),
                                List.of()));
        outro =
                usuarios.saveAndFlush(
                        new Usuario("chat-outro@example.invalid", "hash", "cliente", true));
        clientes.saveAndFlush(new Cliente(outro, "Outro cliente", "93541134780", "11933333333"));
        var e =
                enderecos.saveAndFlush(
                        new EnderecoCliente(
                                c,
                                "01310100",
                                "Rua reservada",
                                "42",
                                "Apto 12",
                                "Bela Vista",
                                "São Paulo",
                                "SP"));
        var pedido =
                pedidos.saveAndFlush(
                        new PedidoServico(
                                c,
                                categoria,
                                e,
                                "Serviço de chat",
                                "Descrição do serviço para conversa entre participantes.",
                                LocalDate.now().plusDays(7),
                                Instant.now()));
        var o = orcamentos.saveAndFlush(new Orcamento(pedido, p, new BigDecimal("180"), 2, null));
        var d =
                disponibilidades.saveAndFlush(
                        new Disponibilidade(p, LocalDate.now().plusDays(7), LocalTime.of(10, 0)));
        agendamento = agendamentos.saveAndFlush(new Agendamento(o, d, e));
        tc = bearer(cliente);
        tp = bearer(prestador);
        em.clear();
    }

    @Test
    void liberaContatosSomenteAosParticipantesEIdentificaSituacaoDaConversa() throws Exception {
        for (String token : List.of(tc, tp))
            mvc.perform(get(rota()).header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.situacaoConversa").value("aguardando_conversa"))
                    .andExpect(jsonPath("$.cliente.telefone").value("11911111111"))
                    .andExpect(jsonPath("$.prestador.telefone").value("11922222222"))
                    .andExpect(jsonPath("$.endereco.logradouro").value("Rua reservada"))
                    .andExpect(jsonPath("$.cliente.email").doesNotExist());
        mvc.perform(get(rota()).header("Authorization", bearer(outro)))
                .andExpect(status().isNotFound());
        mvc.perform(get(rota() + "/mensagens").header("Authorization", bearer(outro)))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post(rota() + "/mensagens")
                                .header("Authorization", bearer(outro))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(UUID.randomUUID(), "Olá")))
                .andExpect(status().isNotFound());
        mvc.perform(get(rota())).andExpect(status().isUnauthorized());
    }

    @Test
    void persisteRemetenteDoTokenEInicioDaConversaSemAlterarReserva() throws Exception {
        var idEnvio = UUID.randomUUID();
        var body =
                mvc.perform(
                                post(rota() + "/mensagens")
                                        .header("Authorization", tc)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"idEnvio\":\""
                                                        + idEnvio
                                                        + "\",\"conteudo\":\"  Olá, podemos"
                                                        + " combinar?  \",\"idRemetente\":"
                                                        + outro.getId()
                                                        + "}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.idRemetente").value(cliente.getId()))
                        .andExpect(jsonPath("$.tipoRemetente").value("cliente"))
                        .andExpect(jsonPath("$.conteudo").value("Olá, podemos combinar?"))
                        .andExpect(jsonPath("$.criadoEm").exists())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(body).doesNotContain("senha", "email", "telefone");
        mvc.perform(
                        post(rota() + "/mensagens")
                                .header("Authorization", tp)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(UUID.randomUUID(), "Sim!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeRemetente").value("Prestador Chat"));
        mvc.perform(get(rota()).header("Authorization", tc))
                .andExpect(jsonPath("$.situacaoConversa").value("em_conversa"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM agendamento WHERE id_agendamento=?",
                                String.class,
                                agendamento.getId()))
                .isEqualTo("confirmado");
    }

    @Test
    void repetirEnvioNaoDuplicaENaoPermiteReaproveitarIdentificadorComOutroConteudo()
            throws Exception {
        var uuid = UUID.randomUUID();
        var first = enviar(tc, uuid, "Mensagem original");
        var second = enviar(tc, uuid, "Mensagem original");
        assertThat(second).isEqualTo(first);
        mvc.perform(
                        post(rota() + "/mensagens")
                                .header("Authorization", tc)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(uuid, "Outro conteúdo")))
                .andExpect(status().isConflict());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM mensagem WHERE id_agendamento=?",
                                Integer.class,
                                agendamento.getId()))
                .isEqualTo(1);
    }

    @Test
    void cursoresRecuperamHistoricoEAtualizacoesSemDuplicarMensagens() throws Exception {
        var ids = new java.util.ArrayList<Integer>();
        for (int n = 1; n <= 5; n++) ids.add(enviar(tc, UUID.randomUUID(), "Mensagem " + n));
        var page =
                mvc.perform(
                                get(rota() + "/mensagens")
                                        .param("tamanho", "2")
                                        .header("Authorization", tp))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.itens[0].conteudo").value("Mensagem 4"))
                        .andExpect(jsonPath("$.itens[1].conteudo").value("Mensagem 5"))
                        .andExpect(jsonPath("$.temMais").value(true))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(json.readTree(page).get("proximoCursor").asInt()).isEqualTo(ids.get(3));
        mvc.perform(
                        get(rota() + "/mensagens")
                                .param("antesDe", ids.get(3).toString())
                                .param("tamanho", "2")
                                .header("Authorization", tc))
                .andExpect(jsonPath("$.itens[0].conteudo").value("Mensagem 2"))
                .andExpect(jsonPath("$.proximoCursor").value(ids.get(1)));
        mvc.perform(
                        get(rota() + "/mensagens")
                                .param("depoisDe", ids.get(1).toString())
                                .param("tamanho", "2")
                                .header("Authorization", tc))
                .andExpect(jsonPath("$.itens[0].conteudo").value("Mensagem 3"))
                .andExpect(jsonPath("$.proximoCursor").value(ids.get(3)));
        mvc.perform(
                        get(rota() + "/mensagens")
                                .param("depoisDe", ids.get(4).toString())
                                .header("Authorization", tc))
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.temMais").value(false));
    }

    @Test
    void validaMensagemCursorEIdentificador() throws Exception {
        for (String body :
                List.of(
                        "{}",
                        "{\"idEnvio\":\"invalido\",\"conteudo\":\"Oi\"}",
                        corpo(UUID.randomUUID(), "   "),
                        corpo(UUID.randomUUID(), "a".repeat(2001)),
                        "{\"idEnvio\":\"" + UUID.randomUUID() + "\",\"conteudo\":\"\\u0000\"}")) {
            mvc.perform(
                            post(rota() + "/mensagens")
                                    .header("Authorization", tc)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isBadRequest());
        }
        for (String query :
                List.of(
                        "tamanho=0",
                        "tamanho=101",
                        "antesDe=0",
                        "depoisDe=-1",
                        "antesDe=1&depoisDe=1",
                        "antesDe=abc"))
            mvc.perform(get(rota() + "/mensagens?" + query).header("Authorization", tc))
                    .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancelado", "concluido"})
    void bloqueiaConversaDeAgendamentoNaoConfirmado(String status) throws Exception {
        jdbc.update(
                "UPDATE agendamento SET status=? WHERE id_agendamento=?",
                status,
                agendamento.getId());
        em.clear();
        mvc.perform(get(rota()).header("Authorization", tc))
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.mensagem")
                                .value("Defina a data e horário do serviço para liberar o chat"));
        mvc.perform(get(rota() + "/mensagens").header("Authorization", tp))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post(rota() + "/mensagens")
                                .header("Authorization", tc)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(UUID.randomUUID(), "Oi")))
                .andExpect(status().isForbidden());
    }

    @Test
    void verificaContaAtivaETipoAtualNoBanco() throws Exception {
        jdbc.update("UPDATE usuario SET ativo=FALSE WHERE id_usuario=?", cliente.getId());
        em.clear();
        mvc.perform(get(rota()).header("Authorization", tc)).andExpect(status().isForbidden());
        jdbc.update(
                "UPDATE usuario SET ativo=TRUE,tipo_usuario='prestador' WHERE id_usuario=?",
                cliente.getId());
        em.clear();
        mvc.perform(get(rota()).header("Authorization", tc)).andExpect(status().isNotFound());
    }

    @Test
    void websocketExigeJwtEAutorizaCadaAssinaturaSemAceitarEnvioAoBroker() {
        var connect = frame(StompCommand.CONNECT, "valida", null, tc);
        assertThat(socket.entrada(connect, null)).isSameAs(connect);
        assertThat(socket.entrada(frame(StompCommand.SUBSCRIBE, "valida", topico(), null), null))
                .isNotNull();
        assertThatThrownBy(() ->
                        socket.entrada(
                                frame(StompCommand.SUBSCRIBE, "sem-token", topico(), null), null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() ->
                        socket.entrada(
                                frame(StompCommand.CONNECT, "invalida", null, "Bearer invalido"),
                                null))
                .isInstanceOf(JwtException.class);
        for (String destino :
                List.of(
                        "/topic/**",
                        "/topic/agendamentos/*/mensagens",
                        "/topic/agendamentos/2147483647/mensagens",
                        "/topic/agendamentos/9999999999/mensagens"))
            assertThatThrownBy(() ->
                            socket.entrada(
                                    frame(StompCommand.SUBSCRIBE, "valida", destino, null), null))
                    .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() ->
                        socket.entrada(frame(StompCommand.SEND, "valida", topico(), null), null))
                .isInstanceOf(AccessDeniedException.class);
        var outroConnect = frame(StompCommand.CONNECT, "outro", null, bearer(outro));
        socket.entrada(outroConnect, null);
        assertThatThrownBy(() ->
                        socket.entrada(
                                frame(StompCommand.SUBSCRIBE, "outro", topico(), null), null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void websocketRevalidaSaidaERevogacaoAposAssinatura() {
        socket.entrada(frame(StompCommand.CONNECT, "saida", null, tp), null);
        var h = StompHeaderAccessor.create(StompCommand.MESSAGE);
        h.setSessionId("saida");
        h.setDestination(topico());
        Message<byte[]> m = MessageBuilder.createMessage(new byte[0], h.getMessageHeaders());
        assertThat(socket.saida(m, null)).isNotNull();
        jdbc.update(
                "UPDATE agendamento SET status='cancelado' WHERE id_agendamento=?",
                agendamento.getId());
        em.clear();
        assertThat(socket.saida(m, null)).isNull();
        jdbc.update(
                "UPDATE agendamento SET status='confirmado' WHERE id_agendamento=?",
                agendamento.getId());
        jdbc.update("UPDATE usuario SET ativo=FALSE WHERE id_usuario=?", prestador.getId());
        em.clear();
        assertThat(socket.saida(m, null)).isNull();
    }

    @Test
    void websocketRejeitaTokenExpirado() {
        var claims =
                JwtClaimsSet.builder()
                        .issuer("appservicos-test")
                        .subject(cliente.getId().toString())
                        .claim("tipoUsuario", "cliente")
                        .issuedAt(Instant.now().minusSeconds(300))
                        .expiresAt(Instant.now().minusSeconds(120))
                        .build();
        var token =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        assertThatThrownBy(() ->
                        socket.entrada(
                                frame(StompCommand.CONNECT, "expirado", null, "Bearer " + token),
                                null))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void agendamentoSemLocalCompletoNaoLiberaContatos() throws Exception {
        jdbc.update(
                "UPDATE agendamento SET endereco_cidade='' WHERE id_agendamento=?",
                agendamento.getId());
        em.clear();
        mvc.perform(get(rota()).header("Authorization", tc)).andExpect(status().isForbidden());
    }

    private Message<byte[]> frame(
            StompCommand command, String session, String destino, String token) {
        var h = StompHeaderAccessor.create(command);
        h.setSessionId(session);
        if (destino != null) h.setDestination(destino);
        if (token != null) h.setNativeHeader("Authorization", token);
        h.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], h.getMessageHeaders());
    }

    private Integer enviar(String token, UUID id, String conteudo) throws Exception {
        var body =
                mvc.perform(
                                post(rota() + "/mensagens")
                                        .header("Authorization", token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(id, conteudo)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return json.readTree(body).get("idMensagem").asInt();
    }

    private String corpo(UUID id, String conteudo) {
        return "{\"idEnvio\":\"" + id + "\",\"conteudo\":\"" + conteudo + "\"}";
    }

    private String rota() {
        return "/agendamentos/" + agendamento.getId() + "/chat";
    }

    private String topico() {
        return "/topic/agendamentos/" + agendamento.getId() + "/mensagens";
    }

    private String bearer(Usuario u) {
        return "Bearer " + tokens.emitir(u).accessToken();
    }
}
