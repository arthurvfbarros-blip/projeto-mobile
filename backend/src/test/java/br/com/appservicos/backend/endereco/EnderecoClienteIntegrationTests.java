package br.com.appservicos.backend.endereco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

import br.com.appservicos.backend.auth.JwtProperties;
import br.com.appservicos.backend.auth.TokenService;
import br.com.appservicos.backend.cliente.Cliente;
import br.com.appservicos.backend.cliente.ClienteRepository;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class EnderecoClienteIntegrationTests {

	private static final String ROTA = "/clientes/me/enderecos";

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private ClienteRepository clientes;

	@Autowired
	private EnderecoClienteRepository enderecos;

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private TokenService tokens;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JwtProperties jwtProperties;

	@Autowired
	private JdbcTemplate jdbc;

	private Cliente cliente;
	private String token;

	@BeforeEach
	void prepararCliente() {
		limparBancoTemporario();
		Usuario usuario = usuarios.save(new Usuario("endereco@example.com", encoder.encode("SenhaTeste123!"), "cliente", true));
		cliente = clientes.saveAndFlush(new Cliente(usuario, "Cliente Endereço", "52998224725", "11999999999"));
		token = tokens.emitir(usuario).accessToken();
	}

	@AfterEach
	void limparBancoTemporario() {
		enderecos.deleteAll();
		clientes.deleteAll();
		usuarios.deleteAll();
	}

	@Test
	void cadastraNormalizaEListaEnderecoDoClienteLogado() throws Exception {
		Map<String, Object> request = dadosValidos();
		request.put("cep", " 01310-100 ");
		request.put("logradouro", " Avenida Paulista ");
		request.put("numero", " 1000 ");
		request.put("complemento", " Apto 12 ");
		request.put("bairro", " Bela Vista ");
		request.put("cidade", " São Paulo ");
		request.put("uf", " sp ");

		String response = mvc.perform(post(ROTA).header("Authorization", "Bearer " + token)
				.header("Origin", "http://localhost:8081")
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"))
				.andExpect(jsonPath("$.idEndereco").isNumber())
				.andExpect(jsonPath("$.cep").value("01310100"))
				.andExpect(jsonPath("$.logradouro").value("Avenida Paulista"))
				.andExpect(jsonPath("$.numero").value("1000"))
				.andExpect(jsonPath("$.complemento").value("Apto 12"))
				.andExpect(jsonPath("$.bairro").value("Bela Vista"))
				.andExpect(jsonPath("$.cidade").value("São Paulo"))
				.andExpect(jsonPath("$.uf").value("SP"))
				.andExpect(jsonPath("$.cliente").doesNotExist())
				.andExpect(jsonPath("$.usuario").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		JsonNode body = json.readTree(response);
		assertThat(jdbc.queryForObject("SELECT id_cliente FROM endereco_cliente WHERE id_endereco = ?",
				Integer.class, body.get("idEndereco").asInt())).isEqualTo(cliente.getId());
		mvc.perform(get(ROTA).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].idEndereco").value(body.get("idEndereco").asInt()));
	}

	@Test
	void listaVaziaQuandoClienteNaoTemEndereco() throws Exception {
		mvc.perform(get(ROTA).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void naoListaEnderecosDeOutroClienteMesmoComIdNaQuery() throws Exception {
		EnderecoCliente primeiro = salvarEndereco(cliente, "1000");
		Cliente outro = cadastrarOutroCliente();
		salvarEndereco(outro, "2000");
		EnderecoCliente segundo = salvarEndereco(cliente, "3000");
		mvc.perform(get(ROTA).param("idCliente", outro.getId().toString()).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].idEndereco").value(primeiro.getId()))
				.andExpect(jsonPath("$[1].idEndereco").value(segundo.getId()));
		mvc.perform(get(ROTA).header("Authorization", "Bearer " + tokens.emitir(outro.getUsuario()).accessToken()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].numero").value("2000"));
	}

	@Test
	void naoPermiteEscolherDonoOuIdDoEnderecoNoCorpo() throws Exception {
		Cliente outro = cadastrarOutroCliente();
		Map<String, Object> request = dadosValidos();
		request.put("idCliente", outro.getId());
		request.put("idUsuario", outro.getUsuario().getId());
		request.put("cliente", Map.of("id", outro.getId()));
		request.put("idEndereco", -100);
		String response = mvc.perform(post(ROTA).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		int id = json.readTree(response).get("idEndereco").asInt();
		assertThat(id).isPositive();
		assertThat(jdbc.queryForObject("SELECT id_cliente FROM endereco_cliente WHERE id_endereco = ?", Integer.class, id))
				.isEqualTo(cliente.getId());
		assertThat(enderecos.findByClienteIdOrderByIdAsc(outro.getId())).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   " })
	void aceitaEnderecoSemNumeroEComplementoEmBranco(String complemento) throws Exception {
		Map<String, Object> request = dadosValidos();
		request.put("numero", "S/N");
		request.put("complemento", complemento);
		mvc.perform(post(ROTA).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.numero").value("S/N"));
		assertThat(enderecos.findByClienteIdOrderByIdAsc(cliente.getId()).getFirst().getComplemento()).isNull();
	}

	@ParameterizedTest
	@MethodSource("camposInvalidos")
	void rejeitaCamposInvalidosSemGravarEndereco(String campo, String valor) throws Exception {
		Map<String, Object> request = dadosValidos();
		request.put(campo, valor);
		mvc.perform(post(ROTA).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(request)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").value("Dados inválidos"))
				.andExpect(jsonPath("$.campos." + campo).exists());
		assertThat(enderecos.count()).isZero();
	}

	static Stream<Arguments> camposInvalidos() {
		return Stream.of(
				Arguments.of("cep", ""), Arguments.of("cep", "0131010"), Arguments.of("cep", "013101000"),
				Arguments.of("cep", "01310A00"), Arguments.of("cep", "013 10100"),
				Arguments.of("cep", "0131-0100"), Arguments.of("cep", "01310--100"),
				Arguments.of("logradouro", "   "), Arguments.of("logradouro", "a".repeat(151)),
				Arguments.of("numero", ""), Arguments.of("numero", "1".repeat(21)),
				Arguments.of("complemento", "a".repeat(101)),
				Arguments.of("bairro", "   "), Arguments.of("bairro", "a".repeat(101)),
				Arguments.of("cidade", "   "), Arguments.of("cidade", "a".repeat(101)),
				Arguments.of("uf", ""), Arguments.of("uf", "XX"), Arguments.of("uf", "SPP"));
	}

	@Test
	void rejeitaCamposAusentes() throws Exception {
		mvc.perform(post(ROTA).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.cep").exists()).andExpect(jsonPath("$.campos.logradouro").exists())
				.andExpect(jsonPath("$.campos.numero").exists()).andExpect(jsonPath("$.campos.bairro").exists())
				.andExpect(jsonPath("$.campos.cidade").exists()).andExpect(jsonPath("$.campos.uf").exists());
		assertThat(enderecos.count()).isZero();
	}

	@Test
	void rejeitaJsonMalformado() throws Exception {
		mvc.perform(post(ROTA).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido"));
		assertThat(enderecos.count()).isZero();
	}

	@Test
	void exigeTokenParaCadastrarEListar() throws Exception {
		mvc.perform(get(ROTA)).andExpect(status().isUnauthorized());
		mvc.perform(post(ROTA).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dadosValidos())))
				.andExpect(status().isUnauthorized());
		mvc.perform(get(ROTA).header("Authorization", "Bearer invalido")).andExpect(status().isUnauthorized());
		assertThat(enderecos.count()).isZero();
	}

	@Test
	void prestadorNaoPodeCadastrarNemListarEnderecosDeCliente() throws Exception {
		Usuario prestador = usuarios.save(new Usuario("prestador@example.com", encoder.encode("SenhaTeste123!"), "prestador", true));
		esperaAcessoNegado(tokens.emitir(prestador).accessToken());
	}

	@Test
	void usuarioSemCadastroDeClienteNaoPodeAcessar() throws Exception {
		Usuario semCliente = usuarios.save(new Usuario("sem-cliente@example.com", encoder.encode("SenhaTeste123!"), "cliente", true));
		esperaAcessoNegado(tokens.emitir(semCliente).accessToken());
	}

	@Test
	void bloqueiaUsuarioDesativadoDepoisDeEmitirToken() throws Exception {
		jdbc.update("UPDATE usuario SET ativo = FALSE WHERE id_usuario = ?", cliente.getUsuario().getId());
		esperaAcessoNegado(token);
	}

	@Test
	void bloqueiaUsuarioQueDeixouDeSerClienteDepoisDeEmitirToken() throws Exception {
		jdbc.update("UPDATE usuario SET tipo_usuario = 'prestador' WHERE id_usuario = ?", cliente.getUsuario().getId());
		esperaAcessoNegado(token);
	}

	@ParameterizedTest
	@ValueSource(strings = { "invalido", "2147483648", "-1" })
	void identidadeInvalidaNoTokenNaoCausaErroInterno(String subject) throws Exception {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer(jwtProperties.issuer()).subject(subject)
				.issuedAt(now).expiresAt(now.plusSeconds(3600)).claim("tipoUsuario", "cliente").build();
		String tokenInvalido = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
		esperaAcessoNegado(tokenInvalido);
	}

	@Test
	void permitePreflightComAuthorization() throws Exception {
		mvc.perform(options(ROTA).header("Origin", "http://localhost:8081")
				.header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "content-type,authorization"))
				.andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
	}

	private void esperaAcessoNegado(String bearer) throws Exception {
		mvc.perform(get(ROTA).header("Authorization", "Bearer " + bearer)).andExpect(status().isForbidden());
		mvc.perform(post(ROTA).header("Authorization", "Bearer " + bearer)
				.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dadosValidos())))
				.andExpect(status().isForbidden());
		assertThat(enderecos.count()).isZero();
	}

	private Cliente cadastrarOutroCliente() {
		Usuario usuario = usuarios.save(new Usuario("outro@example.com", encoder.encode("SenhaTeste123!"), "cliente", true));
		return clientes.saveAndFlush(new Cliente(usuario, "Outro cliente", "11144477735", "11999999998"));
	}

	private EnderecoCliente salvarEndereco(Cliente dono, String numero) {
		return enderecos.saveAndFlush(new EnderecoCliente(dono, "01310100", "Avenida Paulista", numero,
				null, "Bela Vista", "São Paulo", "SP"));
	}

	private Map<String, Object> dadosValidos() {
		return new LinkedHashMap<>(Map.of("cep", "01310100", "logradouro", "Avenida Paulista", "numero", "1000",
				"bairro", "Bela Vista", "cidade", "São Paulo", "uf", "SP"));
	}
}
