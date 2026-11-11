package br.com.appservicos.backend.oportunidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OportunidadeIntegrationTests {

	private static final String ROTA = "/prestadores/me/oportunidades";

	@Autowired private MockMvc mvc;
	@Autowired private JdbcTemplate jdbc;
	@Autowired private EntityManager entityManager;
	@Autowired private ObjectMapper json;
	@Autowired private TokenService tokens;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private ClienteRepository clientes;
	@Autowired private PrestadorRepository prestadores;
	@Autowired private CategoriaServicoRepository categorias;
	@Autowired private EnderecoClienteRepository enderecos;
	@Autowired private PedidoServicoRepository pedidos;

	private CategoriaServico eletrica;
	private CategoriaServico hidraulica;
	private Cliente cliente;
	private Prestador prestador;
	private String token;

	@BeforeEach
	void prepararPerfis() {
		eletrica = categorias.saveAndFlush(new CategoriaServico("Elétrica US14", null));
		hidraulica = categorias.saveAndFlush(new CategoriaServico("Hidráulica US14", null));
		var contaCliente = usuarios.saveAndFlush(new Usuario("us14-cliente@example.invalid", "hash-de-teste", "cliente", true));
		cliente = clientes.saveAndFlush(new Cliente(contaCliente, "Cliente Privado", "52998224725", "11999999999"));
		var contaPrestador = usuarios.saveAndFlush(new Usuario("us14-prestador@example.invalid", "hash-de-teste", "prestador", true));
		prestador = prestadores.saveAndFlush(new Prestador(contaPrestador, "Prestador US14", "11144477735", "11999999999",
				" São Paulo ", "SP", List.of(eletrica), List.of(" Bela Vista ")));
		jdbc.update("UPDATE prestador SET status_aprovacao = 'aprovado' WHERE id_prestador = ?", prestador.getId());
		token = "Bearer " + tokens.emitir(contaPrestador).accessToken();
		entityManager.clear();
	}

	@Test
	void filtraNoBancoAntesDePaginarEContaSomenteCompativeis() throws Exception {
		int primeiro = pedido("Primeiro compatível", eletrica, "São Paulo", "SP", "Bela Vista").getId();
		int segundo = pedido("Segundo compatível", eletrica, "São Paulo", "SP", "Bela Vista").getId();
		pedido("Categoria incompatível", hidraulica, "São Paulo", "SP", "Bela Vista");
		pedido("Cidade incompatível", eletrica, "Campinas", "SP", "Bela Vista");
		pedido("UF incompatível", eletrica, "São Paulo", "RJ", "Bela Vista");
		pedido("Bairro incompatível", eletrica, "São Paulo", "SP", "Centro");

		mvc.perform(get(ROTA).param("tamanho", "1").param("cidade", "Campinas")
				.param("idCategoria", hidraulica.getId().toString()).header("Authorization", token))
				.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.totalItens").value(2)).andExpect(jsonPath("$.totalPaginas").value(2))
				.andExpect(jsonPath("$.itens.length()").value(1)).andExpect(jsonPath("$.itens[0].idPedido").value(segundo));
		mvc.perform(get(ROTA).param("pagina", "1").param("tamanho", "1").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].idPedido").value(primeiro));
	}

	@Test
	void comparaLocalidadesSemDiferencasDeCaixaAcentosEEspacos() throws Exception {
		int id = pedido("Localidade equivalente", eletrica, "  SAO  PAULO  ", "sp", "bÉla   vísta").getId();
		mvc.perform(get(ROTA).header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
				.andExpect(jsonPath("$.itens[0].idPedido").value(id));
		mvc.perform(get(ROTA + "/" + id).header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.idPedido").value(id));
	}

	@Test
	void prestadorSemBairrosAtendeTodaCidadeEMantemTodasSuasCategorias() throws Exception {
		jdbc.update("DELETE FROM prestador_bairro WHERE id_prestador = ?", prestador.getId());
		jdbc.update("INSERT INTO prestador_categoria VALUES (?, ?)", prestador.getId(), hidraulica.getId());
		entityManager.clear();
		pedido("Elétrica Centro", eletrica, "São Paulo", "SP", "Centro");
		pedido("Hidráulica Bela Vista", hidraulica, "São Paulo", "SP", "Bela Vista");
		pedido("Outra cidade", eletrica, "Santos", "SP", "Centro");
		mvc.perform(get(ROTA).header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(2));
	}

	@Test
	void detalheUsaMesmosFiltrosEDadosPublicaveisDaListagem() throws Exception {
		int id = pedido("Pedido visível", eletrica, "São Paulo", "SP", "Bela Vista").getId();
		String body = mvc.perform(get(ROTA + "/" + id).header("Authorization", token))
				.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.descricao").value("Descrição do serviço que pode ser consultada."))
				.andExpect(jsonPath("$.bairro").value("Bela Vista"))
				.andReturn().getResponse().getContentAsString();
		var data = json.readTree(body);
		assertThat(data.has("endereco")).isFalse();
		assertThat(data.has("cliente")).isFalse();
		assertThat(body).doesNotContain("Rua Privada", "01310100", "11999999999", "Cliente Privado", "numero", "complemento");
		for (var config : List.of(
				Map.of("cidade", "Campinas", "uf", "SP", "bairro", "Bela Vista"),
				Map.of("cidade", "São Paulo", "uf", "RJ", "bairro", "Bela Vista"),
				Map.of("cidade", "São Paulo", "uf", "SP", "bairro", "Centro"))) {
			int alheio = pedido("Incompatível", eletrica, config.get("cidade"), config.get("uf"), config.get("bairro")).getId();
			mvc.perform(get(ROTA + "/" + alheio).header("Authorization", token))
					.andExpect(status().isNotFound()).andExpect(jsonPath("$.mensagem").value("Oportunidade não encontrada"));
		}
		int categoriaAlheia = pedido("Categoria alheia", hidraulica, "São Paulo", "SP", "Bela Vista").getId();
		mvc.perform(get(ROTA + "/" + categoriaAlheia).header("Authorization", token)).andExpect(status().isNotFound());
		mvc.perform(get(ROTA + "/" + Integer.MAX_VALUE).header("Authorization", token)).andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@ValueSource(strings = { "aguardando_confirmacao", "agendado", "concluido", "cancelado" })
	void pedidoFechadoNaoApareceNemPodeSerAberto(String situacao) throws Exception {
		int id = pedido("Pedido indisponível", eletrica, "São Paulo", "SP", "Bela Vista").getId();
		jdbc.update("UPDATE pedido_servico SET status = ? WHERE id_pedido = ?", situacao, id);
		entityManager.clear();
		mvc.perform(get(ROTA).header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));
		mvc.perform(get(ROTA + "/" + id).header("Authorization", token)).andExpect(status().isNotFound());
	}

	@Test
	void pedidosRecebendoOrcamentosContinuamDisponiveisEDataDesejadaNaoExpiraPedido() throws Exception {
		int id = pedido("Data preferida passada", eletrica, "São Paulo", "SP", "Bela Vista").getId();
		jdbc.update("UPDATE pedido_servico SET status = 'em_orcamento', data_desejada = DATE '2020-01-01' WHERE id_pedido = ?", id);
		entityManager.clear();
		mvc.perform(get(ROTA).header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].idPedido").value(id));
	}

	@Test
	void pedidoAntigoSemRegiaoNaoPodeSerAtribuidoAoPrestador() throws Exception {
		jdbc.update("INSERT INTO pedido_servico (id_cliente, id_categoria, titulo, descricao) VALUES (?, ?, 'Pedido antigo', 'Descrição antiga')",
				cliente.getId(), eletrica.getId());
		mvc.perform(get(ROTA).header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));
	}

	@Test
	void regiaoDoPedidoPreservaHistoricoQuandoEnderecoOriginalMuda() throws Exception {
		int id = pedido("Endereço histórico", eletrica, "São Paulo", "SP", "Bela Vista").getId();
		jdbc.update("UPDATE endereco_cliente SET cidade = 'Campinas', bairro = 'Centro'");
		entityManager.clear();
		mvc.perform(get(ROTA + "/" + id).header("Authorization", token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.cidade").value("São Paulo"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "pendente", "reprovado" })
	void exigeAprovacaoConsultadaNoBancoEmCadaRequisicao(String situacao) throws Exception {
		int id = pedido("Protegido", eletrica, "São Paulo", "SP", "Bela Vista").getId();
		jdbc.update("UPDATE prestador SET status_aprovacao = ? WHERE id_prestador = ?", situacao, prestador.getId());
		entityManager.clear();
		mvc.perform(get(ROTA).header("Authorization", token)).andExpect(status().isForbidden());
		mvc.perform(get(ROTA + "/" + id).header("Authorization", token)).andExpect(status().isForbidden());
	}

	@Test
	void bloqueiaVisitanteClientePrestadorInativoEContaComTipoAlterado() throws Exception {
		mvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
		mvc.perform(get(ROTA + "/1")).andExpect(status().isUnauthorized());
		String tokenCliente = "Bearer " + tokens.emitir(cliente.getUsuario()).accessToken();
		mvc.perform(get(ROTA).header("Authorization", tokenCliente)).andExpect(status().isForbidden());
		int idUsuario = prestador.getUsuario().getId();
		jdbc.update("UPDATE usuario SET ativo = FALSE WHERE id_usuario = ?", idUsuario);
		entityManager.clear();
		mvc.perform(get(ROTA).header("Authorization", token)).andExpect(status().isForbidden());
		jdbc.update("UPDATE usuario SET ativo = TRUE, tipo_usuario = 'cliente' WHERE id_usuario = ?", idUsuario);
		entityManager.clear();
		mvc.perform(get(ROTA).header("Authorization", token)).andExpect(status().isForbidden());
	}

	@Test
	void perfilAntigoSemAreaOuCategoriasRecebeErroDeConfiguracao() throws Exception {
		jdbc.update("UPDATE prestador SET cidade = NULL, uf = NULL WHERE id_prestador = ?", prestador.getId());
		entityManager.clear();
		mvc.perform(get(ROTA).header("Authorization", token))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.cidade").exists());
		jdbc.update("UPDATE prestador SET cidade = 'São Paulo', uf = 'SP' WHERE id_prestador = ?", prestador.getId());
		jdbc.update("DELETE FROM prestador_categoria WHERE id_prestador = ?", prestador.getId());
		entityManager.clear();
		mvc.perform(get(ROTA).header("Authorization", token))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.idCategorias").exists());
	}

	@Test
	void validaLimitesDaPaginacaoEIdentificador() throws Exception {
		for (var parametro : List.of(
				Map.of("pagina", "-1"), Map.of("tamanho", "0"), Map.of("tamanho", "51"),
				Map.of("pagina", "abc"), Map.of("tamanho", "abc"))) {
			var entry = parametro.entrySet().iterator().next();
			mvc.perform(get(ROTA).param(entry.getKey(), entry.getValue()).header("Authorization", token))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos." + entry.getKey()).exists());
		}
		mvc.perform(get(ROTA + "/abc").header("Authorization", token)).andExpect(status().isBadRequest());
		mvc.perform(get(ROTA + "/-1").header("Authorization", token)).andExpect(status().isNotFound());
	}

	private PedidoServico pedido(String titulo, CategoriaServico categoria, String cidade, String uf, String bairro) {
		var endereco = enderecos.saveAndFlush(new EnderecoCliente(cliente, "01310100", "Rua Privada", "1000",
				"Apto 10", bairro, cidade, uf.toUpperCase(java.util.Locale.ROOT)));
		var pedido = pedidos.saveAndFlush(new PedidoServico(cliente, categoria, endereco, titulo,
				"Descrição do serviço que pode ser consultada.", LocalDate.now().plusDays(7), Instant.now()));
		return pedido;
	}
}
