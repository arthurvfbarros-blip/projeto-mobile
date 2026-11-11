package br.com.appservicos.backend.agendamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(AgendamentoIntegrationTests.Tempo.class)
class AgendamentoIntegrationTests {
    @TestConfiguration
    static class Tempo {
        @Bean
        @Primary
        Clock relogioTeste() {
            return Clock.fixed(
                    Instant.parse("2030-01-15T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        }
    }

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager em;
    @Autowired private ObjectMapper json;
    @Autowired private TokenService tokens;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ClienteRepository clientes;
    @Autowired private PrestadorRepository prestadores;
    @Autowired private CategoriaServicoRepository categorias;
    @Autowired private EnderecoClienteRepository enderecos;
    @Autowired private PedidoServicoRepository pedidos;
    @Autowired private OrcamentoRepository orcamentos;
    @Autowired private DisponibilidadeRepository disponibilidades;
    private Cliente cliente, outroCliente;
    private Prestador prestador, outroPrestador;
    private EnderecoCliente endereco;
    private PedidoServico pedido;
    private Orcamento orcamento;
    private Disponibilidade horario;
    private String tokenCliente, tokenPrestador;

    @BeforeEach
    void preparar() {
        var categoria = categorias.saveAndFlush(new CategoriaServico("Elétrica US05", null));
        var uc =
                usuarios.saveAndFlush(
                        new Usuario("us05-cliente@example.invalid", "hash", "cliente", true));
        cliente =
                clientes.saveAndFlush(
                        new Cliente(uc, "Cliente US05", "52998224725", "11999999999"));
        var outro =
                usuarios.saveAndFlush(
                        new Usuario("us05-outro-cliente@example.invalid", "hash", "cliente", true));
        outroCliente =
                clientes.saveAndFlush(new Cliente(outro, "Outro cliente", "93541134780", null));
        endereco =
                enderecos.saveAndFlush(
                        new EnderecoCliente(
                                cliente,
                                "01310100",
                                "Rua do pedido",
                                "100",
                                null,
                                "Bela Vista",
                                "São Paulo",
                                "SP"));
        pedido =
                pedidos.saveAndFlush(
                        new PedidoServico(
                                cliente,
                                categoria,
                                endereco,
                                "Pedido US05",
                                "Descrição do serviço para agendar atendimento.",
                                LocalDate.of(2030, 1, 16),
                                Instant.now()));
        prestador = prestador("principal", "11144477735", categoria);
        outroPrestador = prestador("outro", "12345678909", categoria);
        orcamento =
                orcamentos.saveAndFlush(
                        new Orcamento(
                                pedido,
                                prestador,
                                new BigDecimal("180.50"),
                                2,
                                "Inclui instalação"));
        horario =
                disponibilidades.saveAndFlush(
                        new Disponibilidade(
                                prestador, LocalDate.of(2030, 1, 16), LocalTime.of(10, 0)));
        tokenCliente = bearer(uc);
        tokenPrestador = bearer(prestador.getUsuario());
        em.clear();
    }

    @Test
    void agendaAtomicamentePreservaEnderecoEAtualizaEstados() throws Exception {
        var concorrente =
                orcamentos.saveAndFlush(
                        new Orcamento(pedido, outroPrestador, new BigDecimal("220"), 1, null));
        var escolhido =
                enderecos.saveAndFlush(
                        new EnderecoCliente(
                                cliente,
                                "01310999",
                                "Rua combinada",
                                "55",
                                "Apto 12",
                                "BÉLA  VÍSTA",
                                "Sao  Paulo",
                                "SP"));
        String body =
                mvc.perform(
                                post("/clientes/me/agendamentos")
                                        .header("Authorization", tokenCliente)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(orcamento, horario, escolhido)))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(jsonPath("$.status").value("confirmado"))
                        .andExpect(jsonPath("$.endereco.logradouro").value("Rua combinada"))
                        .andExpect(jsonPath("$.nomePrestador").value("Prestador principal"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        int id = json.readTree(body).get("idAgendamento").asInt();
        assertThat(body).doesNotContain("senha", "email", "telefone", "cpf");
        assertThat(estado("pedido_servico", "status", "id_pedido", pedido.getId()))
                .isEqualTo("agendado");
        assertThat(estado("orcamento", "status", "id_orcamento", orcamento.getId()))
                .isEqualTo("aceito");
        assertThat(estado("orcamento", "status", "id_orcamento", concorrente.getId()))
                .isEqualTo("recusado");
        assertThat(estado("disponibilidade", "status", "id_disponibilidade", horario.getId()))
                .isEqualTo("ocupado");
        jdbc.update(
                "UPDATE endereco_cliente SET logradouro='Endereço alterado' WHERE id_endereco=?",
                escolhido.getId());
        em.clear();
        mvc.perform(get("/clientes/me/agendamentos/" + id).header("Authorization", tokenCliente))
                .andExpect(jsonPath("$.endereco.logradouro").value("Rua combinada"));
        mvc.perform(
                        get("/prestadores/me/agendamentos/" + id)
                                .header("Authorization", tokenPrestador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco.numero").value("55"));
        mvc.perform(
                        get("/clientes/me/pedidos/" + pedido.getId())
                                .header("Authorization", tokenCliente))
                .andExpect(jsonPath("$.endereco.logradouro").value("Rua do pedido"));
    }

    @Test
    void impedeOutroAgendamentoDoMesmoPedidoMesmoComOutroOrcamentoEHorario() throws Exception {
        var outra =
                orcamentos.saveAndFlush(
                        new Orcamento(pedido, outroPrestador, new BigDecimal("220"), 1, null));
        var slot =
                disponibilidades.saveAndFlush(
                        new Disponibilidade(outroPrestador, horario.getData(), horario.getHora()));
        agendar();
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .header("Authorization", tokenCliente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(outra, slot, endereco)))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendamento", Integer.class))
                .isEqualTo(1);
        assertThat(estado("disponibilidade", "status", "id_disponibilidade", slot.getId()))
                .isEqualTo("livre");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"Campinas|SP|Bela Vista", "São Paulo|RJ|Bela Vista", "São Paulo|SP|Centro"})
    void rejeitaEnderecoForaDaAreaSemConsumirHorarioOuRecusarOutros(String local) throws Exception {
        var partes = local.split("\\|");
        var fora =
                enderecos.saveAndFlush(
                        new EnderecoCliente(
                                cliente,
                                "01310100",
                                "Rua fora",
                                "20",
                                null,
                                partes[2],
                                partes[0],
                                partes[1]));
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .header("Authorization", tokenCliente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(orcamento, horario, fora)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.idEndereco").exists());
        assertThat(estado("disponibilidade", "status", "id_disponibilidade", horario.getId()))
                .isEqualTo("livre");
        assertThat(estado("orcamento", "status", "id_orcamento", orcamento.getId()))
                .isEqualTo("enviado");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM agendamento", Integer.class)).isZero();
    }

    @Test
    void enderecoAlheioEHorarioDeOutroPrestadorSaoBloqueados() throws Exception {
        var alheio =
                enderecos.saveAndFlush(
                        new EnderecoCliente(
                                outroCliente,
                                "01310100",
                                "Rua alheia",
                                "20",
                                null,
                                "Bela Vista",
                                "São Paulo",
                                "SP"));
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .header("Authorization", tokenCliente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(orcamento, horario, alheio)))
                .andExpect(status().isBadRequest());
        var slot =
                disponibilidades.saveAndFlush(
                        new Disponibilidade(outroPrestador, horario.getData(), horario.getHora()));
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .header("Authorization", tokenCliente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(orcamento, slot, endereco)))
                .andExpect(status().isConflict());
    }

    @Test
    void isolaContasOrcamentosEAgendas() throws Exception {
        mvc.perform(
                        get("/clientes/me/orcamentos/" + orcamento.getId())
                                .header("Authorization", bearer(outroCliente.getUsuario())))
                .andExpect(status().isNotFound());
        mvc.perform(get(slots()).header("Authorization", bearer(outroCliente.getUsuario())))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .header("Authorization", bearer(outroCliente.getUsuario()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(orcamento, horario, endereco)))
                .andExpect(status().isNotFound());
        int id = agendar();
        mvc.perform(
                        get("/clientes/me/agendamentos/" + id)
                                .header("Authorization", bearer(outroCliente.getUsuario())))
                .andExpect(status().isNotFound());
        mvc.perform(
                        get("/prestadores/me/agendamentos/" + id)
                                .header("Authorization", bearer(outroPrestador.getUsuario())))
                .andExpect(status().isNotFound());
        mvc.perform(
                        get("/clientes/me/agendamentos")
                                .header("Authorization", bearer(outroCliente.getUsuario())))
                .andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(
                        get("/prestadores/me/agendamentos")
                                .header("Authorization", bearer(outroPrestador.getUsuario())))
                .andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(
                        get("/prestadores/me/disponibilidades")
                                .header("Authorization", bearer(outroPrestador.getUsuario())))
                .andExpect(jsonPath("$.totalItens").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"aguardando_confirmacao", "agendado", "concluido", "cancelado"})
    void bloqueiaPedidoEncerrado(String estado) throws Exception {
        jdbc.update("UPDATE pedido_servico SET status=? WHERE id_pedido=?", estado, pedido.getId());
        em.clear();
        mvc.perform(get(slots()).header("Authorization", tokenCliente))
                .andExpect(status().isConflict());
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .header("Authorization", tokenCliente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(orcamento, horario, endereco)))
                .andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"aceito", "recusado", "expirado"})
    void bloqueiaOrcamentoNaoEnviado(String estado) throws Exception {
        jdbc.update(
                "UPDATE orcamento SET status=? WHERE id_orcamento=?", estado, orcamento.getId());
        em.clear();
        mvc.perform(get(slots()).header("Authorization", tokenCliente))
                .andExpect(status().isConflict());
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .header("Authorization", tokenCliente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(orcamento, horario, endereco)))
                .andExpect(status().isConflict());
    }

    @Test
    void exigePrestadorAprovadoEContaAtiva() throws Exception {
        for (String estado : List.of("pendente", "reprovado")) {
            jdbc.update(
                    "UPDATE prestador SET status_aprovacao=? WHERE id_prestador=?",
                    estado,
                    prestador.getId());
            em.clear();
            mvc.perform(get(slots()).header("Authorization", tokenCliente))
                    .andExpect(status().isForbidden());
            mvc.perform(
                            post("/clientes/me/agendamentos")
                                    .header("Authorization", tokenCliente)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(corpo(orcamento, horario, endereco)))
                    .andExpect(status().isForbidden());
            mvc.perform(
                            post("/prestadores/me/disponibilidades")
                                    .header("Authorization", tokenPrestador)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"data\":\"2030-01-17\",\"hora\":\"10:00\"}"))
                    .andExpect(status().isForbidden());
        }
        jdbc.update(
                "UPDATE prestador SET status_aprovacao='aprovado' WHERE id_prestador=?",
                prestador.getId());
        jdbc.update(
                "UPDATE usuario SET ativo=FALSE WHERE id_usuario=?",
                prestador.getUsuario().getId());
        em.clear();
        mvc.perform(get(slots()).header("Authorization", tokenCliente))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "2030-01-14|10:00",
                "2030-01-15|08:59",
                "2030-01-15|09:00",
                "2030-01-17|10:00:30"
            })
    void validaHorarioFuturoEmBrasiliaEPrecisaoDeMinutos(String dataHora) throws Exception {
        var p = dataHora.split("\\|");
        mvc.perform(
                        post("/prestadores/me/disponibilidades")
                                .header("Authorization", tokenPrestador)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"data\":\"" + p[0] + "\",\"hora\":\"" + p[1] + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastraDisponibilidadeListaOrdenaPaginaEOmiteOcupadosDoCliente() throws Exception {
        String body = "{\"data\":\"2030-01-15\",\"hora\":\"09:01\"}";
        mvc.perform(
                        post("/prestadores/me/disponibilidades")
                                .header("Authorization", tokenPrestador)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated());
        mvc.perform(get(slots()).param("tamanho", "1").header("Authorization", tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.itens[0].hora").value("09:01:00"));
        jdbc.update(
                "UPDATE disponibilidade SET status='ocupado' WHERE id_disponibilidade=?",
                horario.getId());
        em.clear();
        mvc.perform(get(slots()).header("Authorization", tokenCliente))
                .andExpect(jsonPath("$.totalItens").value(1));
        mvc.perform(get("/prestadores/me/disponibilidades").header("Authorization", tokenPrestador))
                .andExpect(jsonPath("$.totalItens").value(2));
        mvc.perform(
                        post("/prestadores/me/disponibilidades")
                                .header("Authorization", tokenPrestador)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void horarioOcupadoOuPassadoNaoPodeSerReservado() throws Exception {
        for (String sql :
                List.of(
                        "UPDATE disponibilidade SET status='ocupado' WHERE id_disponibilidade=?",
                        "UPDATE disponibilidade SET status='livre',data=DATE '2030-01-14' WHERE"
                            + " id_disponibilidade=?")) {
            jdbc.update(sql, horario.getId());
            em.clear();
            mvc.perform(
                            post("/clientes/me/agendamentos")
                                    .header("Authorization", tokenCliente)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(corpo(orcamento, horario, endereco)))
                    .andExpect(status().isConflict())
                    .andExpect(
                            jsonPath("$.mensagem").value("Data indisponível, escolha outra data"));
        }
    }

    @Test
    void filtrosDaAgendaPreservamEstadosReaisSemConcluirAutomaticamente() throws Exception {
        int id = agendar();
        mvc.perform(get("/clientes/me/agendamentos").header("Authorization", tokenCliente))
                .andExpect(jsonPath("$.totalItens").value(1));
        jdbc.update(
                "UPDATE disponibilidade SET data=DATE '2030-01-14' WHERE id_disponibilidade=?",
                horario.getId());
        em.clear();
        mvc.perform(get("/clientes/me/agendamentos").header("Authorization", tokenCliente))
                .andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(
                        get("/clientes/me/agendamentos")
                                .param("periodo", "historico")
                                .header("Authorization", tokenCliente))
                .andExpect(jsonPath("$.itens[0].status").value("confirmado"));
        jdbc.update("UPDATE agendamento SET status='cancelado' WHERE id_agendamento=?", id);
        em.clear();
        mvc.perform(
                        get("/prestadores/me/agendamentos")
                                .param("periodo", "cancelados")
                                .header("Authorization", tokenPrestador))
                .andExpect(jsonPath("$.totalItens").value(1));
        mvc.perform(
                        get("/clientes/me/agendamentos")
                                .param("periodo", "historico")
                                .header("Authorization", tokenCliente))
                .andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test
    void rejeitaVisitantesPapelIncorretoCorposEPaginacaoInvalidos() throws Exception {
        for (String route :
                List.of(
                        "/clientes/me/agendamentos",
                        "/prestadores/me/agendamentos",
                        "/prestadores/me/disponibilidades",
                        slots())) mvc.perform(get(route)).andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .header("Authorization", tokenPrestador)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(corpo(orcamento, horario, endereco)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/prestadores/me/disponibilidades")
                                .header("Authorization", tokenCliente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .header("Authorization", tokenCliente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/clientes/me/agendamentos")
                                .header("Authorization", tokenCliente)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"idOrcamento\":-1,\"idDisponibilidade\":0,\"idEndereco\":0}"))
                .andExpect(status().isBadRequest());
        for (String query :
                List.of("pagina=-1", "tamanho=0", "tamanho=51", "periodo=outro", "pagina=abc"))
            mvc.perform(
                            get("/clientes/me/agendamentos?" + query)
                                    .header("Authorization", tokenCliente))
                    .andExpect(status().isBadRequest());
    }

    private Prestador prestador(String nome, String cpf, CategoriaServico categoria) {
        var u =
                usuarios.saveAndFlush(
                        new Usuario(
                                "us05-" + nome + "@example.invalid", "hash", "prestador", true));
        var p =
                prestadores.saveAndFlush(
                        new Prestador(
                                u,
                                "Prestador " + nome,
                                cpf,
                                null,
                                "São Paulo",
                                "SP",
                                List.of(categoria),
                                List.of("Bela Vista")));
        jdbc.update(
                "UPDATE prestador SET status_aprovacao='aprovado' WHERE id_prestador=?", p.getId());
        return p;
    }

    private int agendar() throws Exception {
        var body =
                mvc.perform(
                                post("/clientes/me/agendamentos")
                                        .header("Authorization", tokenCliente)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(corpo(orcamento, horario, endereco)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return json.readTree(body).get("idAgendamento").asInt();
    }

    private String corpo(Orcamento o, Disponibilidade d, EnderecoCliente e) {
        return "{\"idOrcamento\":"
                + o.getId()
                + ",\"idDisponibilidade\":"
                + d.getId()
                + ",\"idEndereco\":"
                + e.getId()
                + "}";
    }

    private String slots() {
        return "/clientes/me/orcamentos/" + orcamento.getId() + "/disponibilidades";
    }

    private String bearer(Usuario u) {
        return "Bearer " + tokens.emitir(u).accessToken();
    }

    private String estado(String tabela, String coluna, String pk, Integer id) {
        return jdbc.queryForObject(
                "SELECT " + coluna + " FROM " + tabela + " WHERE " + pk + "=?", String.class, id);
    }
}
