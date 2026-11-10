package br.com.appservicos.backend.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.appservicos.backend.auth.TokenService;
import br.com.appservicos.backend.categoria.CategoriaServico;
import br.com.appservicos.backend.categoria.CategoriaServicoRepository;
import br.com.appservicos.backend.cliente.Cliente;
import br.com.appservicos.backend.cliente.ClienteRepository;
import br.com.appservicos.backend.endereco.EnderecoCliente;
import br.com.appservicos.backend.endereco.EnderecoClienteRepository;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoServicoIntegrationTests {

	private static final String ROTA = "/clientes/me/pedidos";
	private static final Instant AGORA = Instant.parse("2026-11-11T01:00:00Z");

	@Autowired private MockMvc mvc;
	@Autowired private ObjectMapper json;
	@Autowired private JdbcTemplate jdbc;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private ClienteRepository clientes;
	@Autowired private EnderecoClienteRepository enderecos;
	@Autowired private CategoriaServicoRepository categorias;
	@Autowired private PedidoServicoRepository pedidos;
	@Autowired private PasswordEncoder encoder;
	@Autowired private TokenService tokens;
	@MockitoBean private Clock clock;

	private Cliente cliente;
	private EnderecoCliente endereco;
	private CategoriaServico categoria;
	private String token;

	@BeforeEach
	void prepararDados() {
		when(clock.instant()).thenReturn(AGORA);
		when(clock.getZone()).thenReturn(ZoneId.of("America/Sao_Paulo"));
		limparBancoTemporario();
		Usuario usuario = usuarios.save(new Usuario("pedido@example.com", encoder.encode("SenhaTeste123!"), "cliente", true));
		cliente = clientes.saveAndFlush(new Cliente(usuario, "Cliente pedido", "52998224725", "11999999999"));
		endereco = enderecos.saveAndFlush(new EnderecoCliente(cliente, "01310100", "Avenida Paulista", "1000",
				"Apto 12", "Bela Vista", "São Paulo", "SP"));
		categorias.save(new CategoriaServico("Hidráulica", null));
		categoria = categorias.saveAndFlush(new CategoriaServico("Elétrica", "Instalações elétricas"));
		token = tokens.emitir(usuario).accessToken();
	}

	@AfterEach
	void limparBancoTemporario() {
		pedidos.deleteAll();
		enderecos.deleteAll();
		clientes.deleteAll();
		usuarios.deleteAll();
		categorias.deleteAll();
	}

	@Test
	void listaCatalogoPublicoOrdenadoParaPedidosECadastroProfissional() throws Exception {
		mvc.perform(get("/categorias")).andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].idCategoria").value(categoria.getId()))
				.andExpect(jsonPath("$[0].nome").value("Elétrica"))
				.andExpect(jsonPath("$[1].nome").value("Hidráulica"));
		mvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void catalogoVazioRetornaArrayVazio() throws Exception {
		categorias.deleteAll();
		mvc.perform(get("/categorias")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void criaPedidoAbertoNormalizadoEPermiteConsultarDetalhes() throws Exception {
		Map<String, Object> request = dadosValidos();
		request.put("titulo", "  Instalar chuveiro  ");
		request.put("descricao", "  Preciso instalar um chuveiro elétrico no banheiro.  ");
		JsonNode body = criar(request);
		assertThat(body.get("status").asText()).isEqualTo("aberto");
		assertThat(body.get("titulo").asText()).isEqualTo("Instalar chuveiro");
		assertThat(body.get("descricao").asText()).isEqualTo("Preciso instalar um chuveiro elétrico no banheiro.");
		assertThat(Instant.parse(body.get("criadoEm").asText())).isEqualTo(AGORA);
		assertThat(body.get("endereco").get("logradouro").asText()).isEqualTo("Avenida Paulista");
		assertThat(body.has("cliente")).isFalse();
		assertThat(body.has("usuario")).isFalse();
		int id = body.get("idPedido").asInt();
		assertThat(jdbc.queryForObject("SELECT id_cliente FROM pedido_servico WHERE id_pedido = ?", Integer.class, id))
				.isEqualTo(cliente.getId());
		mvc.perform(get(ROTA + "/" + id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.idPedido").value(id))
				.andExpect(jsonPath("$.categoria.idCategoria").value(categoria.getId()))
				.andExpect(jsonPath("$.endereco.numero").value("1000"));
	}

	@Test
	void naoPermiteEscolherDonoEstadoDataDeCriacaoOuEnderecoHistorico() throws Exception {
		Cliente outro = outroCliente();
		Map<String, Object> request = dadosValidos();
		request.put("idCliente", outro.getId());
		request.put("idUsuario", outro.getUsuario().getId());
		request.put("status", "concluido");
		request.put("criadoEm", "2000-01-01T00:00:00Z");
		request.put("endereco", Map.of("logradouro", "Endereço adulterado"));
		int id = criar(request).get("idPedido").asInt();
		assertThat(jdbc.queryForObject("SELECT id_cliente FROM pedido_servico WHERE id_pedido = ?", Integer.class, id))
				.isEqualTo(cliente.getId());
		assertThat(jdbc.queryForObject("SELECT status FROM pedido_servico WHERE id_pedido = ?", String.class, id)).isEqualTo("aberto");
		assertThat(jdbc.queryForObject("SELECT endereco_logradouro FROM pedido_servico WHERE id_pedido = ?", String.class, id))
				.isEqualTo("Avenida Paulista");
	}

	@Test
	void enderecoDeOutraContaNaoPodeSerUsado() throws Exception {
		Cliente outro = outroCliente();
		EnderecoCliente alheio = enderecos.saveAndFlush(new EnderecoCliente(outro, "01310100", "Rua da outra conta", "1",
				null, "Bela Vista", "São Paulo", "SP"));
		Map<String, Object> request = dadosValidos();
		request.put("idEndereco", alheio.getId());
		esperaCampoInvalido(request, "idEndereco");
		assertThat(pedidos.count()).isZero();
	}

	@Test
	void clienteSemEnderecoNaoPublicaPedido() throws Exception {
		enderecos.deleteAll();
		esperaCampoInvalido(dadosValidos(), "idEndereco");
		assertThat(pedidos.count()).isZero();
	}

	@Test
	void categoriaRemovidaNaoPodeSerUsada() throws Exception {
		categorias.deleteById(categoria.getId());
		esperaCampoInvalido(dadosValidos(), "idCategoria");
		assertThat(pedidos.count()).isZero();
	}

	@Test
	void listaSomentePedidosDaContaEmOrdemDecrescenteEPaginados() throws Exception {
		int primeiro = criar(dadosValidos()).get("idPedido").asInt();
		int segundo = criar(dadosValidos()).get("idPedido").asInt();
		Cliente outro = outroCliente();
		pedidos.saveAndFlush(new PedidoServico(outro, categoria, endereco, "Pedido de outra conta",
				"Um serviço de outra conta para não aparecer na listagem.", LocalDate.now(clock), AGORA));
		mvc.perform(get(ROTA).param("tamanho", "1").param("idCliente", outro.getId().toString())
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(2))
				.andExpect(jsonPath("$.totalPaginas").value(2)).andExpect(jsonPath("$.pagina").value(0))
				.andExpect(jsonPath("$.itens.length()").value(1))
				.andExpect(jsonPath("$.itens[0].idPedido").value(segundo))
				.andExpect(jsonPath("$.itens[0].descricao").doesNotExist())
				.andExpect(jsonPath("$.itens[0].endereco").doesNotExist());
		mvc.perform(get(ROTA).param("tamanho", "1").param("pagina", "1").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].idPedido").value(primeiro));
	}

	@Test
	void detalheDeOutraContaEIdInexistenteRetornamMesmo404() throws Exception {
		int id = criar(dadosValidos()).get("idPedido").asInt();
		String outroToken = tokens.emitir(outroCliente().getUsuario()).accessToken();
		for (int pedido : new int[] { id, Integer.MAX_VALUE }) {
			mvc.perform(get(ROTA + "/" + pedido).header("Authorization", "Bearer " + outroToken))
					.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensagem").value("Pedido não encontrado"));
		}
	}

	@Test
	void copiaDoEnderecoPermaneceAposAlterarOuExcluirEnderecoOriginal() throws Exception {
		int id = criar(dadosValidos()).get("idPedido").asInt();
		jdbc.update("UPDATE endereco_cliente SET logradouro = 'Rua alterada', numero = '99' WHERE id_endereco = ?", endereco.getId());
		enderecos.deleteById(endereco.getId());
		mvc.perform(get(ROTA + "/" + id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.endereco.logradouro").value("Avenida Paulista"))
				.andExpect(jsonPath("$.endereco.numero").value("1000"));
		assertThat(jdbc.queryForObject("SELECT id_endereco FROM pedido_servico WHERE id_pedido = ?", Integer.class, id)).isNull();
	}

	@Test
	void pedidosAntigosSemNovosCamposContinuamConsultaveisSemInventarDados() throws Exception {
		jdbc.update("INSERT INTO pedido_servico (id_cliente, id_categoria, titulo, descricao, status) VALUES (?, ?, ?, ?, 'aberto')",
				cliente.getId(), categoria.getId(), "Pedido antigo", "Descrição cadastrada antes da integração da US02.");
		int id = jdbc.queryForObject("SELECT id_pedido FROM pedido_servico", Integer.class);
		mvc.perform(get(ROTA + "/" + id).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.endereco").isEmpty())
				.andExpect(jsonPath("$.dataDesejada").isEmpty()).andExpect(jsonPath("$.criadoEm").isEmpty());
	}

	@Test
	void aceitaHojeNoFusoDoBrasilMesmoQuandoUtcJaEstaNoDiaSeguinte() throws Exception {
		Map<String, Object> request = dadosValidos();
		request.put("dataDesejada", "2026-11-10");
		criar(request);
	}

	@ParameterizedTest
	@MethodSource("camposInvalidos")
	void rejeitaCamposInvalidosSemGravar(String campo, Object valor) throws Exception {
		Map<String, Object> request = dadosValidos();
		request.put(campo, valor);
		esperaCampoInvalido(request, campo);
		assertThat(pedidos.count()).isZero();
	}

	static Stream<Arguments> camposInvalidos() {
		return Stream.of(Arguments.of("titulo", "  "), Arguments.of("titulo", "a".repeat(151)),
				Arguments.of("descricao", "curta"), Arguments.of("descricao", " ".repeat(20)),
				Arguments.of("descricao", "a".repeat(5001)), Arguments.of("idCategoria", null),
				Arguments.of("idCategoria", 0), Arguments.of("idEndereco", null), Arguments.of("idEndereco", -1),
				Arguments.of("dataDesejada", null), Arguments.of("dataDesejada", "2026-11-09"),
				Arguments.of("dataDesejada", "+10000-01-01"));
	}

	@Test
	void aceitaLimitesDeTituloEDescricao() throws Exception {
		Map<String, Object> request = dadosValidos();
		request.put("titulo", "a".repeat(150));
		request.put("descricao", "a".repeat(20));
		criar(request);
		request.put("descricao", "a".repeat(5000));
		criar(request);
	}

	@Test
	void rejeitaCamposAusentesEDatasInvalidas() throws Exception {
		mvc.perform(post(ROTA).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.idEndereco").exists())
				.andExpect(jsonPath("$.campos.idCategoria").exists()).andExpect(jsonPath("$.campos.titulo").exists())
				.andExpect(jsonPath("$.campos.descricao").exists()).andExpect(jsonPath("$.campos.dataDesejada").exists());
		Map<String, Object> request = dadosValidos();
		request.put("dataDesejada", "2026-02-31");
		mvc.perform(post(ROTA).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
		assertThat(pedidos.count()).isZero();
	}

	@ParameterizedTest
	@MethodSource("paginacoesInvalidas")
	void rejeitaPaginacaoInvalida(String parametro, String valor) throws Exception {
		mvc.perform(get(ROTA).param(parametro, valor).header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos." + parametro).exists());
	}

	static Stream<Arguments> paginacoesInvalidas() {
		return Stream.of(Arguments.of("pagina", "-1"), Arguments.of("tamanho", "0"),
				Arguments.of("tamanho", "51"), Arguments.of("pagina", "abc"));
	}

	@Test
	void bloqueiaAcessoSemTokenPrestadorOuClienteInativo() throws Exception {
		mvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
		mvc.perform(get(ROTA + "/1")).andExpect(status().isUnauthorized());
		mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dadosValidos())))
				.andExpect(status().isUnauthorized());
		Usuario prestador = usuarios.save(new Usuario("prestador@example.com", encoder.encode("SenhaTeste123!"), "prestador", true));
		esperaAcessoNegado(tokens.emitir(prestador).accessToken());
		jdbc.update("UPDATE usuario SET ativo = FALSE WHERE id_usuario = ?", cliente.getUsuario().getId());
		esperaAcessoNegado(token);
	}

	@Test
	void clienteSemPerfilOuComTipoAlteradoNaoAcessaPedidos() throws Exception {
		Usuario semPerfil = usuarios.save(new Usuario("sem-perfil@example.com", encoder.encode("SenhaTeste123!"), "cliente", true));
		esperaAcessoNegado(tokens.emitir(semPerfil).accessToken());
		jdbc.update("UPDATE usuario SET tipo_usuario = 'prestador' WHERE id_usuario = ?", cliente.getUsuario().getId());
		esperaAcessoNegado(token);
	}

	@Test
	void permitePreflightAutenticadoSemExigirTokenNoOptions() throws Exception {
		mvc.perform(options(ROTA).header("Origin", "http://localhost:8081").header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "authorization,content-type"))
				.andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
	}

	private JsonNode criar(Map<String, Object> request) throws Exception {
		String body = mvc.perform(post(ROTA).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
				.andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
				.andReturn().getResponse().getContentAsString();
		return json.readTree(body);
	}

	private void esperaCampoInvalido(Map<String, Object> request, String campo) throws Exception {
		mvc.perform(post(ROTA).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(request))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos." + campo).exists());
	}

	private void esperaAcessoNegado(String bearer) throws Exception {
		mvc.perform(get(ROTA).header("Authorization", "Bearer " + bearer)).andExpect(status().isForbidden());
		mvc.perform(get(ROTA + "/1").header("Authorization", "Bearer " + bearer)).andExpect(status().isForbidden());
		mvc.perform(post(ROTA).header("Authorization", "Bearer " + bearer).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(dadosValidos()))).andExpect(status().isForbidden());
	}

	private Cliente outroCliente() {
		Usuario usuario = usuarios.save(new Usuario("outro@example.com", encoder.encode("SenhaTeste123!"), "cliente", true));
		return clientes.saveAndFlush(new Cliente(usuario, "Outro cliente", "11144477735", "11999999998"));
	}

	private Map<String, Object> dadosValidos() {
		return new LinkedHashMap<>(Map.of("idCategoria", categoria.getId(), "idEndereco", endereco.getId(),
				"titulo", "Instalar chuveiro", "descricao", "Preciso instalar um chuveiro elétrico no banheiro.",
				"dataDesejada", LocalDate.now(clock).plusDays(2).toString()));
	}
}
