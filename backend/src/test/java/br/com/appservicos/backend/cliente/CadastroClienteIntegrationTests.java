package br.com.appservicos.backend.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CadastroClienteIntegrationTests {

	private static final String CPF = "52998224725";
	private static final String SENHA = "SenhaCadastro123!";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private JdbcTemplate jdbc;

	@MockitoSpyBean
	private UsuarioRepository usuarios;

	@MockitoSpyBean
	private ClienteRepository clientes;

	@BeforeEach
	@AfterEach
	void limparBancoTemporario() {
		clientes.deleteAll();
		usuarios.deleteAll();
	}

	@Test
	void cadastraClienteCompletoEPermiteLogin() throws Exception {
		Map<String, String> request = dadosValidos();
		request.put("nome", "  Cliente de Teste  ");
		request.put("email", "  cadastro@example.com  ");
		request.put("cpf", "529.982.247-25");
		request.put("celular", "(11) 99999-9999");

		String response = mvc.perform(post("/auth/cadastro/cliente")
				.header("Origin", "http://localhost:8081")
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"))
				.andExpect(jsonPath("$.idCliente").isNumber())
				.andExpect(jsonPath("$.idUsuario").isNumber())
				.andExpect(jsonPath("$.nome").value("Cliente de Teste"))
				.andExpect(jsonPath("$.email").value("cadastro@example.com"))
				.andExpect(jsonPath("$.tipoUsuario").value("cliente"))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist())
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andReturn().getResponse().getContentAsString();

		JsonNode body = json.readTree(response);
		Usuario usuario = usuarios.findByEmail("cadastro@example.com").orElseThrow();
		Cliente cliente = clientes.findById(body.get("idCliente").asInt()).orElseThrow();
		assertThat(usuarios.count()).isEqualTo(1);
		assertThat(clientes.count()).isEqualTo(1);
		assertThat(usuario.isAtivo()).isTrue();
		assertThat(usuario.getId()).isEqualTo(body.get("idUsuario").asInt());
		assertThat(usuario.getSenhaHash()).startsWith("$2").isNotEqualTo(SENHA);
		assertThat(encoder.matches(SENHA, usuario.getSenhaHash())).isTrue();
		assertThat(cliente.getNome()).isEqualTo("Cliente de Teste");
		assertThat(cliente.getCpf()).isEqualTo(CPF);
		assertThat(cliente.getTelefone()).isEqualTo("11999999999");
		assertThat(jdbc.queryForObject("SELECT id_usuario FROM cliente WHERE id_cliente = ?", Integer.class, cliente.getId()))
				.isEqualTo(usuario.getId());
		assertThat(response).doesNotContain(SENHA, usuario.getSenhaHash(), CPF);

		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("email", "cadastro@example.com", "senha", SENHA))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.usuario.id").value(usuario.getId()))
				.andExpect(jsonPath("$.usuario.tipoUsuario").value("cliente"));
	}

	@Test
	void naoPermiteEscolherTipoOuEstadoDoUsuario() throws Exception {
		Map<String, Object> request = new LinkedHashMap<>(dadosValidos());
		request.put("tipoUsuario", "prestador");
		request.put("ativo", false);
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoUsuario").value("cliente"));
		Usuario usuario = usuarios.findByEmail("cadastro@example.com").orElseThrow();
		assertThat(usuario.getTipoUsuario()).isEqualTo("cliente");
		assertThat(usuario.isAtivo()).isTrue();
	}

	@Test
	void emailJaExistenteEmQualquerTipoRetornaConflito() throws Exception {
		usuarios.save(new Usuario("cadastro@example.com", encoder.encode(SENHA), "prestador", true));
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(dadosValidos())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"))
				.andExpect(jsonPath("$.campos.email").exists());
		assertThat(usuarios.count()).isEqualTo(1);
		assertThat(clientes.count()).isZero();
	}

	@Test
	void cpfJaExistenteRetornaConflitoSemCriarUsuario() throws Exception {
		cadastrarClienteExistente();
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(dadosValidos())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensagem").value("CPF já cadastrado"))
				.andExpect(jsonPath("$.campos.cpf").exists());
		assertThat(usuarios.count()).isEqualTo(1);
		assertThat(clientes.count()).isEqualTo(1);
		assertThat(usuarios.findByEmail("cadastro@example.com")).isEmpty();
	}

	@Test
	void colisaoDeCpfNoBancoDesfazTambemCriacaoDoUsuario() throws Exception {
		cadastrarClienteExistente();
		// Simula a checagem anterior à gravação ficando desatualizada.
		doReturn(false).when(clientes).existsByCpf(CPF);
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(dadosValidos())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.campos.cpf").exists());
		assertThat(usuarios.count()).isEqualTo(1);
		assertThat(clientes.count()).isEqualTo(1);
		assertThat(usuarios.findByEmail("cadastro@example.com")).isEmpty();
	}

	@Test
	void colisaoDeEmailNoBancoRetornaConflitoSemDuplicarDados() throws Exception {
		usuarios.save(new Usuario("cadastro@example.com", encoder.encode(SENHA), "cliente", true));
		doReturn(false).when(usuarios).existsByEmail("cadastro@example.com");
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(dadosValidos())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.campos.email").exists());
		assertThat(usuarios.count()).isEqualTo(1);
		assertThat(clientes.count()).isZero();
	}

	@ParameterizedTest
	@MethodSource("camposInvalidos")
	void rejeitaCamposInvalidosSemGravarDados(String campo, String valor) throws Exception {
		Map<String, String> request = dadosValidos();
		request.put(campo, valor);
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos." + campo).exists());
		assertThat(usuarios.count()).isZero();
		assertThat(clientes.count()).isZero();
	}

	static Stream<Arguments> camposInvalidos() {
		return Stream.of(
				Arguments.of("nome", "   "),
				Arguments.of("nome", "a".repeat(151)),
				Arguments.of("email", "email-invalido"),
				Arguments.of("email", "a".repeat(139) + "@example.com"),
				Arguments.of("email", ""),
				Arguments.of("cpf", "52998224724"),
				Arguments.of("cpf", "11111111111"),
				Arguments.of("cpf", "5299822472"),
				Arguments.of("cpf", "cpf-invalido"),
				Arguments.of("celular", "99999"),
				Arguments.of("celular", "1199999999a"),
				Arguments.of("senha", "curta"),
				Arguments.of("senha", "        "),
				Arguments.of("senha", "a".repeat(73)),
				Arguments.of("senha", "á".repeat(40)));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Abcd123!", " SenhaComEspacos123! " })
	void respeitaSenhaMinimaEPreservaEspacosNaSenha(String senha) throws Exception {
		Map<String, String> request = dadosValidos();
		request.put("senha", senha);
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(request))).andExpect(status().isCreated());
		Usuario usuario = usuarios.findByEmail("cadastro@example.com").orElseThrow();
		assertThat(encoder.matches(senha, usuario.getSenhaHash())).isTrue();
	}

	@Test
	void aceitaSenhaUnicodeNoLimiteDe72Bytes() throws Exception {
		String senha = "á".repeat(36);
		Map<String, String> request = dadosValidos();
		request.put("senha", senha);
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(request))).andExpect(status().isCreated());
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("email", "cadastro@example.com", "senha", senha))))
				.andExpect(status().isOk());
	}

	@Test
	void mesmoCpfComOuSemMascaraNaoPodeGerarDoisClientes() throws Exception {
		cadastrarClienteExistente();
		Map<String, String> request = dadosValidos();
		request.put("cpf", "529.982.247-25");
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.campos.cpf").exists());
		assertThat(clientes.count()).isEqualTo(1);
		assertThat(usuarios.count()).isEqualTo(1);
	}

	@Test
	void rejeitaCamposAusentes() throws Exception {
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.nome").exists())
				.andExpect(jsonPath("$.campos.email").exists())
				.andExpect(jsonPath("$.campos.cpf").exists())
				.andExpect(jsonPath("$.campos.celular").exists())
				.andExpect(jsonPath("$.campos.senha").exists());
		assertThat(usuarios.count()).isZero();
		assertThat(clientes.count()).isZero();
	}

	@Test
	void rejeitaJsonMalformado() throws Exception {
		mvc.perform(post("/auth/cadastro/cliente").contentType(MediaType.APPLICATION_JSON).content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido"));
		assertThat(usuarios.count()).isZero();
	}

	@Test
	void naoAbreOutrasRotasAoLiberarCadastro() throws Exception {
		mvc.perform(get("/auth/cadastro/cliente")).andExpect(status().isUnauthorized());
		mvc.perform(post("/auth/cadastro/prestador").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	private void cadastrarClienteExistente() {
		Usuario usuario = usuarios.save(new Usuario("existente@example.com", encoder.encode(SENHA), "cliente", true));
		clientes.saveAndFlush(new Cliente(usuario, "Cliente existente", CPF, "11999999999"));
	}

	private Map<String, String> dadosValidos() {
		return new LinkedHashMap<>(Map.of("nome", "Cliente de Teste", "email", "cadastro@example.com",
				"celular", "11999999999", "cpf", CPF, "senha", SENHA));
	}
}
