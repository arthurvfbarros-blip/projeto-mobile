package br.com.appservicos.backend.agendamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(
        properties =
                "spring.datasource.url=jdbc:h2:mem:us05-concorrencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgendamentoConcorrenciaTests {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private TokenService tokens;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ClienteRepository clientes;
    @Autowired private PrestadorRepository prestadores;
    @Autowired private CategoriaServicoRepository categorias;
    @Autowired private EnderecoClienteRepository enderecos;
    @Autowired private PedidoServicoRepository pedidos;
    @Autowired private OrcamentoRepository orcamentos;
    @Autowired private DisponibilidadeRepository disponibilidades;
    private static final AtomicInteger sequencia = new AtomicInteger();

    @Test
    void duasPessoasDisputandoOMesmoHorarioGeramUmaUnicaReserva() throws Exception {
        disputar(true);
    }

    @Test
    void duasPropostasDoMesmoPedidoNaoGeramDoisAgendamentos() throws Exception {
        disputar(false);
    }

    private void disputar(boolean mesmoHorario) throws Exception {
        var tentativas =
                new TransactionTemplate(transactions)
                        .execute(transaction -> {
                            var categoria =
                                    categorias.saveAndFlush(
                                            new CategoriaServico(
                                                    "Categoria corrida "
                                                            + sequencia.incrementAndGet(),
                                                    null));
                            var c1 = cliente();
                            var c2 = mesmoHorario ? cliente() : c1;
                            var e1 = endereco(c1);
                            var e2 = mesmoHorario ? endereco(c2) : e1;
                            var p1 = prestador(categoria);
                            var p2 = mesmoHorario ? p1 : prestador(categoria);
                            var pedido1 =
                                    pedidos.saveAndFlush(
                                            new PedidoServico(
                                                    c1,
                                                    categoria,
                                                    e1,
                                                    "Serviço 1",
                                                    "Descrição de serviço para concorrência.",
                                                    LocalDate.now().plusDays(7),
                                                    Instant.now()));
                            var pedido2 =
                                    mesmoHorario
                                            ? pedidos.saveAndFlush(
                                                    new PedidoServico(
                                                            c2,
                                                            categoria,
                                                            e2,
                                                            "Serviço 2",
                                                            "Descrição de serviço para"
                                                                + " concorrência.",
                                                            LocalDate.now().plusDays(7),
                                                            Instant.now()))
                                            : pedido1;
                            var o1 =
                                    orcamentos.saveAndFlush(
                                            new Orcamento(
                                                    pedido1, p1, new BigDecimal("180"), 2, null));
                            var o2 =
                                    orcamentos.saveAndFlush(
                                            new Orcamento(
                                                    pedido2, p2, new BigDecimal("220"), 2, null));
                            var d1 =
                                    disponibilidades.saveAndFlush(
                                            new Disponibilidade(
                                                    p1,
                                                    LocalDate.now().plusDays(7),
                                                    LocalTime.of(10, 0)));
                            var d2 =
                                    mesmoHorario
                                            ? d1
                                            : disponibilidades.saveAndFlush(
                                                    new Disponibilidade(
                                                            p2, d1.getData(), d1.getHora()));
                            return List.of(
                                    new Tentativa(
                                            bearer(c1.getUsuario()),
                                            o1.getId(),
                                            pedido1.getId(),
                                            d1.getId(),
                                            e1.getId()),
                                    new Tentativa(
                                            bearer(c2.getUsuario()),
                                            o2.getId(),
                                            pedido2.getId(),
                                            d2.getId(),
                                            e2.getId()));
                        });
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks =
                    tentativas.stream()
                            .map(t ->
                                    executor.submit(() -> {
                                        ready.countDown();
                                        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                                        return mvc.perform(
                                                        post("/clientes/me/agendamentos")
                                                                .header("Authorization", t.token())
                                                                .contentType(
                                                                        MediaType.APPLICATION_JSON)
                                                                .content(
                                                                        "{\"idOrcamento\":"
                                                                                + t.orcamento()
                                                                                + ",\"idDisponibilidade\":"
                                                                                + t.horario()
                                                                                + ",\"idEndereco\":"
                                                                                + t.endereco()
                                                                                + "}"))
                                                .andReturn()
                                                .getResponse()
                                                .getStatus();
                                    }))
                            .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(
                            List.of(
                                    tasks.get(0).get(10, TimeUnit.SECONDS),
                                    tasks.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        var t1 = tentativas.get(0);
        var t2 = tentativas.get(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM agendamento a JOIN orcamento o ON"
                                    + " a.id_orcamento=o.id_orcamento WHERE o.id_pedido IN (?,?)",
                                Integer.class,
                                t1.pedido(),
                                t2.pedido()))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT count(*) FROM disponibilidade WHERE id_disponibilidade IN"
                                    + " (?,?) AND status='ocupado'",
                                Integer.class,
                                t1.horario(),
                                t2.horario()))
                .isEqualTo(1);
        if (!mesmoHorario)
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT count(*) FROM orcamento WHERE id_pedido=? AND"
                                        + " status='recusado'",
                                    Integer.class,
                                    t1.pedido()))
                    .isEqualTo(1);
    }

    private Cliente cliente() {
        int n = sequencia.incrementAndGet();
        return clientes.saveAndFlush(
                new Cliente(
                        usuarios.saveAndFlush(
                                new Usuario(
                                        "corrida-c" + n + "@example.invalid",
                                        "hash",
                                        "cliente",
                                        true)),
                        "Cliente " + n,
                        String.format("%011d", n),
                        null));
    }

    private EnderecoCliente endereco(Cliente c) {
        return enderecos.saveAndFlush(
                new EnderecoCliente(
                        c, "01310100", "Rua teste", "42", null, "Bela Vista", "São Paulo", "SP"));
    }

    private Prestador prestador(CategoriaServico categoria) {
        int n = sequencia.incrementAndGet();
        var p =
                prestadores.saveAndFlush(
                        new Prestador(
                                usuarios.saveAndFlush(
                                        new Usuario(
                                                "corrida-p" + n + "@example.invalid",
                                                "hash",
                                                "prestador",
                                                true)),
                                "Prestador " + n,
                                String.format("%011d", n),
                                null,
                                "São Paulo",
                                "SP",
                                List.of(categoria),
                                List.of()));
        jdbc.update(
                "UPDATE prestador SET status_aprovacao='aprovado' WHERE id_prestador=?", p.getId());
        return p;
    }

    private String bearer(Usuario u) {
        return "Bearer " + tokens.emitir(u).accessToken();
    }

    private record Tentativa(
            String token, Integer orcamento, Integer pedido, Integer horario, Integer endereco) {}
}
