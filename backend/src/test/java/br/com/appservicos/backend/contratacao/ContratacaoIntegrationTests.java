package br.com.appservicos.backend.contratacao;

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
import br.com.appservicos.backend.chat.ChatSocketSeguranca;
import br.com.appservicos.backend.cliente.Cliente;
import br.com.appservicos.backend.cliente.ClienteRepository;
import br.com.appservicos.backend.disponibilidade.Disponibilidade;
import br.com.appservicos.backend.disponibilidade.DisponibilidadeRepository;
import br.com.appservicos.backend.endereco.EnderecoCliente;
import br.com.appservicos.backend.endereco.EnderecoClienteRepository;
import br.com.appservicos.backend.notificacao.NotificacaoRepository;
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
import org.springframework.security.oauth2.jwt.JwtEncoder;
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
class ContratacaoIntegrationTests {
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

    @Autowired ContratacaoRepository contratacoes;
    @Autowired NotificacaoRepository notificacoes;

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

        jdbc.update(
                "UPDATE prestador SET status_aprovacao='aprovado' WHERE id_usuario=?",
                prestador.getId());
        jdbc.update(
                "UPDATE pedido_servico SET status='agendado' WHERE id_pedido=?",
                agendamento.getOrcamento().getPedido().getId());
        jdbc.update(
                "UPDATE orcamento SET status='aceito' WHERE id_orcamento=?",
                agendamento.getOrcamento().getId());
        jdbc.update(
                "UPDATE disponibilidade SET status='ocupado' WHERE id_disponibilidade=?",
                agendamento.getDisponibilidade().getId());
        em.clear();
    }

    @Test
    void exigeMensagemDeCadaParticipanteAntesDeConfirmar() throws Exception {
        mvc.perform(get(revisao()).header("Authorization", tc))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.podeConfirmar").value(false));
        conversar(tc, "Gostaria de confirmar o serviço.");
        mvc.perform(post(rota()).header("Authorization", tc)).andExpect(status().isConflict());
        mvc.perform(get(revisao()).header("Authorization", tc))
                .andExpect(jsonPath("$.podeConfirmar").value(false));
        conversar(tp, "Podemos confirmar o acordo.");
        mvc.perform(get(revisao()).header("Authorization", tc))
                .andExpect(jsonPath("$.podeConfirmar").value(true));
        assertThat(contratacoes.count()).isZero();
        assertThat(notificacoes.count()).isZero();
    }

    @Test
    void confirmaValorDoOrcamentoPreservaReservaENotificaSomentePrestador() throws Exception {
        conversar(tc, "Combinado");
        conversar(tp, "Combinado também");
        var body =
                mvc.perform(
                                post(rota())
                                        .header("Authorization", tc)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"valor\":1,\"idCliente\":999}"))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(jsonPath("$.valor").value(180))
                        .andExpect(jsonPath("$.idAgendamento").value(agendamento.getId()))
                        .andExpect(jsonPath("$.confirmadaEm").exists())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        int id = json.readTree(body).get("idContratacao").asInt();
        assertThat(body).doesNotContain("telefone", "email", "cpf");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM pedido_servico WHERE id_pedido=?",
                                String.class,
                                agendamento.getOrcamento().getPedido().getId()))
                .isEqualTo("contratado");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status FROM agendamento WHERE id_agendamento=?",
                                String.class,
                                agendamento.getId()))
                .isEqualTo("confirmado");
        for (String token : List.of(tc, tp))
            mvc.perform(
                            get(token.equals(tc)
                                            ? "/clientes/me/agendamentos/" + agendamento.getId()
                                            : "/prestadores/me/agendamentos/" + agendamento.getId())
                                    .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contratacao.idContratacao").value(id));
        mvc.perform(get(rota()).header("Authorization", tc))
                .andExpect(jsonPath("$.idContratacao").value(id));
        mvc.perform(
                        get("/prestadores/me/agendamentos/" + agendamento.getId() + "/contratacao")
                                .header("Authorization", tp))
                .andExpect(jsonPath("$.idContratacao").value(id));
        mvc.perform(get("/prestadores/me/notificacoes").header("Authorization", tp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].tipo").value("contratacao_confirmada"));
        assertThat(jdbc.queryForObject("SELECT id_usuario FROM notificacao", Integer.class))
                .isEqualTo(prestador.getId());
        conversar(tc, "Nos vemos no horário combinado.");
    }

    @Test
    void repetirConfirmacaoRetornaMesmoRegistroSemNovaNotificacao() throws Exception {
        conversar(tc, "Olá");
        conversar(tp, "Olá");
        String first = confirmar();
        String second = confirmar();
        assertThat(json.readTree(second)).isEqualTo(json.readTree(first));
        assertThat(contratacoes.count()).isEqualTo(1);
        assertThat(notificacoes.count()).isEqualTo(1);
        mvc.perform(get(revisao()).header("Authorization", tc))
                .andExpect(jsonPath("$.podeConfirmar").value(false))
                .andExpect(jsonPath("$.agendamento.contratacao").exists());
    }

    @Test
    void naoAceitaMensagemDeTerceiroComoConversaDoPrestador() throws Exception {
        conversar(tc, "Combinado");
        jdbc.update(
                "INSERT INTO mensagem(id_agendamento,id_remetente,id_envio,conteudo,criado_em)"
                        + " VALUES (?,?,?,?,CURRENT_TIMESTAMP)",
                agendamento.getId(),
                outro.getId(),
                UUID.randomUUID(),
                "Mensagem fora dos participantes");
        mvc.perform(post(rota()).header("Authorization", tc)).andExpect(status().isConflict());
        assertThat(contratacoes.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancelado", "concluido"})
    void bloqueiaAgendamentoIndisponivel(String estado) throws Exception {
        conversar(tc, "Olá");
        conversar(tp, "Olá");
        jdbc.update(
                "UPDATE agendamento SET status=? WHERE id_agendamento=?",
                estado,
                agendamento.getId());
        em.clear();
        mvc.perform(post(rota()).header("Authorization", tc)).andExpect(status().isConflict());
        assertThat(contratacoes.count()).isZero();
        assertThat(notificacoes.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancelado", "concluido", "aberto", "em_orcamento"})
    void exigeEstadoConsistenteDoPedido(String estado) throws Exception {
        conversar(tc, "Olá");
        conversar(tp, "Olá");
        jdbc.update(
                "UPDATE pedido_servico SET status=? WHERE id_pedido=?",
                estado,
                agendamento.getOrcamento().getPedido().getId());
        em.clear();
        mvc.perform(post(rota()).header("Authorization", tc)).andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pendente", "reprovado"})
    void prestadorPrecisaContinuarAprovado(String estado) throws Exception {
        conversar(tc, "Olá");
        conversar(tp, "Olá");
        jdbc.update(
                "UPDATE prestador SET status_aprovacao=? WHERE id_usuario=?",
                estado,
                prestador.getId());
        em.clear();
        mvc.perform(post(rota()).header("Authorization", tc)).andExpect(status().isConflict());
        mvc.perform(get(revisao()).header("Authorization", tc))
                .andExpect(jsonPath("$.podeConfirmar").value(false));
    }

    @Test
    void isolaContasEPapeisEExigeClienteAtivo() throws Exception {
        for (String route : List.of(rota(), revisao()))
            mvc.perform(get(route).header("Authorization", bearer(outro)))
                    .andExpect(status().isNotFound());
        mvc.perform(post(rota()).header("Authorization", bearer(outro)))
                .andExpect(status().isNotFound());
        mvc.perform(post(rota()).header("Authorization", tp)).andExpect(status().isForbidden());
        mvc.perform(get(revisao()).header("Authorization", tp)).andExpect(status().isForbidden());
        mvc.perform(post(rota())).andExpect(status().isUnauthorized());
        mvc.perform(get(revisao())).andExpect(status().isUnauthorized());
        jdbc.update("UPDATE usuario SET ativo=FALSE WHERE id_usuario=?", cliente.getId());
        em.clear();
        mvc.perform(post(rota()).header("Authorization", tc)).andExpect(status().isForbidden());
    }

    @Test
    void notificacaoLidaPersisteERepeticaoNaoCriaOuApagaRegistro() throws Exception {
        conversar(tc, "Olá");
        conversar(tp, "Olá");
        confirmar();
        var id = jdbc.queryForObject("SELECT id_notificacao FROM notificacao", Integer.class);
        for (int n = 0; n < 2; n++)
            mvc.perform(
                            post("/prestadores/me/notificacoes/" + id + "/lida")
                                    .header("Authorization", tp))
                    .andExpect(status().isNoContent());
        mvc.perform(get("/prestadores/me/notificacoes").header("Authorization", tp))
                .andExpect(jsonPath("$.totalItens").value(0));
        assertThat(notificacoes.count()).isEqualTo(1);
        mvc.perform(get("/prestadores/me/notificacoes").header("Authorization", tc))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/prestadores/me/notificacoes/" + id + "/lida")
                                .header("Authorization", tc))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/prestadores/me/notificacoes/2147483647/lida")
                                .header("Authorization", tp))
                .andExpect(status().isNotFound());
        for (String query : List.of("pagina=-1", "tamanho=0", "tamanho=51", "pagina=abc"))
            mvc.perform(get("/prestadores/me/notificacoes?" + query).header("Authorization", tp))
                    .andExpect(status().isBadRequest());
    }

    @Test
    void outroPrestadorNaoConsultaContratacaoOuLeNotificacaoAlheia() throws Exception {
        conversar(tc, "Olá");
        conversar(tp, "Olá");
        confirmar();
        var usuario =
                usuarios.saveAndFlush(
                        new Usuario(
                                "outro-prestador-us07@example.invalid", "hash", "prestador", true));
        prestadores.saveAndFlush(
                new br.com.appservicos.backend.prestador.Prestador(
                        usuario,
                        "Outro prestador",
                        "93541134780",
                        null,
                        "São Paulo",
                        "SP",
                        List.of(),
                        List.of()));
        var token = bearer(usuario);
        mvc.perform(
                        get("/prestadores/me/agendamentos/" + agendamento.getId() + "/contratacao")
                                .header("Authorization", token))
                .andExpect(status().isNotFound());
        mvc.perform(get("/prestadores/me/notificacoes").header("Authorization", token))
                .andExpect(jsonPath("$.totalItens").value(0));
        var id = jdbc.queryForObject("SELECT id_notificacao FROM notificacao", Integer.class);
        mvc.perform(
                        post("/prestadores/me/notificacoes/" + id + "/lida")
                                .header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void filaDeNotificacoesExigePrestadorEDestinoDaPropriaSessao() {
        socket.entrada(frame(StompCommand.CONNECT, "n-cliente", null, tc), null);
        assertThatThrownBy(() ->
                        socket.entrada(
                                frame(
                                        StompCommand.SUBSCRIBE,
                                        "n-cliente",
                                        "/user/queue/notificacoes",
                                        null),
                                null))
                .isInstanceOf(AccessDeniedException.class);
        socket.entrada(frame(StompCommand.CONNECT, "n-prestador", null, tp), null);
        assertThat(
                        socket.entrada(
                                frame(
                                        StompCommand.SUBSCRIBE,
                                        "n-prestador",
                                        "/user/queue/notificacoes",
                                        null),
                                null))
                .isNotNull();
        for (String destino :
                List.of(
                        "/queue/notificacoes-usern-cliente",
                        "/user/999/queue/notificacoes",
                        "/user/**"))
            assertThatThrownBy(() ->
                            socket.entrada(
                                    frame(StompCommand.SUBSCRIBE, "n-prestador", destino, null),
                                    null))
                    .isInstanceOf(RuntimeException.class);
        var h = StompHeaderAccessor.create(StompCommand.MESSAGE);
        h.setSessionId("n-prestador");
        h.setDestination("/queue/notificacoes-usern-prestador");
        assertThat(
                        socket.saida(
                                MessageBuilder.createMessage(new byte[0], h.getMessageHeaders()),
                                null))
                .isNotNull();
        h = StompHeaderAccessor.create(StompCommand.MESSAGE);
        h.setSessionId("n-prestador");
        h.setDestination("/queue/notificacoes-useroutro");
        assertThat(
                        socket.saida(
                                MessageBuilder.createMessage(new byte[0], h.getMessageHeaders()),
                                null))
                .isNull();
    }

    private void conversar(String token, String texto) throws Exception {
        mvc.perform(
                        post("/agendamentos/" + agendamento.getId() + "/chat/mensagens")
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"idEnvio\":\""
                                                + UUID.randomUUID()
                                                + "\",\"conteudo\":\""
                                                + texto
                                                + "\"}"))
                .andExpect(status().isOk());
    }

    private String confirmar() throws Exception {
        return mvc.perform(post(rota()).header("Authorization", tc))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String rota() {
        return "/clientes/me/agendamentos/" + agendamento.getId() + "/contratacao";
    }

    private String revisao() {
        return rota() + "/revisao";
    }

    private String bearer(Usuario u) {
        return "Bearer " + tokens.emitir(u).accessToken();
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
}
