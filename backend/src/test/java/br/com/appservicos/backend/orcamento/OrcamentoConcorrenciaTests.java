package br.com.appservicos.backend.orcamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

import br.com.appservicos.backend.auth.TokenService;
import br.com.appservicos.backend.categoria.CategoriaServico;
import br.com.appservicos.backend.categoria.CategoriaServicoRepository;
import br.com.appservicos.backend.cliente.Cliente;
import br.com.appservicos.backend.cliente.ClienteRepository;
import br.com.appservicos.backend.endereco.EnderecoCliente;
import br.com.appservicos.backend.endereco.EnderecoClienteRepository;
import br.com.appservicos.backend.pedido.PedidoServico;
import br.com.appservicos.backend.pedido.PedidoServicoRepository;
import br.com.appservicos.backend.prestador.Prestador;
import br.com.appservicos.backend.prestador.PrestadorRepository;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:us15-concorrencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrcamentoConcorrenciaTests {
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

	@Test
	void doisEnviosSimultaneosGeramSomenteUmOrcamento() throws Exception {
		var fixture = new TransactionTemplate(transactions).execute(transaction -> {
			var categoria = categorias.saveAndFlush(new CategoriaServico("Elétrica concorrência", null));
			var contaCliente = usuarios.saveAndFlush(new Usuario("corrida-cliente@example.invalid", "hash", "cliente", true));
			var cliente = clientes.saveAndFlush(new Cliente(contaCliente, "Cliente", "52998224725", null));
			var endereco = enderecos.saveAndFlush(new EnderecoCliente(cliente, "01310100", "Rua Teste", "42", null, "Centro", "São Paulo", "SP"));
			var pedido = pedidos.saveAndFlush(new PedidoServico(cliente, categoria, endereco, "Pedido concorrente", "Descrição do serviço para teste concorrente.", LocalDate.now(), Instant.now()));
			var conta = usuarios.saveAndFlush(new Usuario("corrida-prestador@example.invalid", "hash", "prestador", true));
			var prestador = prestadores.saveAndFlush(new Prestador(conta, "Prestador", "11144477735", null, "São Paulo", "SP", List.of(categoria), List.of()));
			jdbc.update("UPDATE prestador SET status_aprovacao='aprovado' WHERE id_prestador=?", prestador.getId());
			return new Fixture(pedido.getId(), "Bearer " + tokens.emitir(conta).accessToken());
		});
		var ready = new CountDownLatch(2);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var tasks = java.util.stream.IntStream.range(0, 2).mapToObj(index -> executor.submit(() -> {
				ready.countDown();
				assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
				return mvc.perform(post("/prestadores/me/oportunidades/" + fixture.idPedido() + "/orcamento")
						.header("Authorization", fixture.token()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"valor\":180,\"prazoDias\":2}"))
						.andReturn().getResponse().getStatus();
			})).toList();
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			assertThat(List.of(tasks.get(0).get(10, TimeUnit.SECONDS), tasks.get(1).get(10, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder(201, 409);
		}
		assertThat(jdbc.queryForObject("SELECT count(*) FROM orcamento WHERE id_pedido=?", Integer.class, fixture.idPedido())).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT status FROM pedido_servico WHERE id_pedido=?", String.class, fixture.idPedido())).isEqualTo("em_orcamento");
	}

	private record Fixture(Integer idPedido, String token) { }
}
