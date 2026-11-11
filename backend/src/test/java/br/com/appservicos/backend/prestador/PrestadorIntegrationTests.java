package br.com.appservicos.backend.prestador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.auth.TokenService;
import br.com.appservicos.backend.categoria.CategoriaServico;
import br.com.appservicos.backend.categoria.CategoriaServicoRepository;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrestadorIntegrationTests {

	private static final String CADASTRO = "/auth/cadastro/prestador";
	private static final String PERFIL = "/prestadores/me";
	private static final String EMAIL = "us12@example.invalid";
	private static final String SENHA = "SenhaTeste123!";

	@Autowired private MockMvc mvc;
	@Autowired private ObjectMapper json;
	@Autowired private JdbcTemplate jdbc;
	@Autowired private EntityManager entityManager;
	@Autowired private CategoriaServicoRepository categorias;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private PasswordEncoder encoder;
	@Autowired private TokenService tokens;

	private CategoriaServico categoria;

	@BeforeEach
	void cadastrarCategoria() {
		categoria = categorias.saveAndFlush(new CategoriaServico("Elétrica US12", null));
	}

	@Test
	void criaPerfilPendenteComCategoriasEBairrosEPermiteAcompanharPeloLogin() throws Exception {
		var dados = dadosValidos();
		dados.put("idCategorias", List.of(categoria.getId(), categoria.getId()));
		dados.put("bairros", List.of(" Bela  Vista ", "bela vista", "Centro"));
		dados.put("statusAprovacao", "aprovado");
		dados.put("tipoUsuario", "cliente");

		int id = cadastrar(dados);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM empresa WHERE id_prestador = ?", Integer.class, id))
				.isZero();
		var usuario = usuarios.findByEmail(EMAIL).orElseThrow();
		assertThat(usuario.getSenhaHash()).isNotEqualTo(SENHA);
		assertThat(encoder.matches(SENHA, usuario.getSenhaHash())).isTrue();

		String token = login();
		mvc.perform(get(PERFIL).param("idPrestador", "999999").header("Authorization", token))
				.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.idPrestador").value(id))
				.andExpect(jsonPath("$.nome").value("Prestador de Teste"))
				.andExpect(jsonPath("$.statusAprovacao").value("pendente"))
				.andExpect(jsonPath("$.cidade").value("São Paulo")).andExpect(jsonPath("$.uf").value("SP"))
				.andExpect(jsonPath("$.bairros.length()").value(2))
				.andExpect(jsonPath("$.bairros[0]").value("Bela Vista"))
				.andExpect(jsonPath("$.categorias.length()").value(1))
				.andExpect(jsonPath("$.categorias[0].idCategoria").value(categoria.getId()))
				.andExpect(jsonPath("$.cpf").doesNotExist()).andExpect(jsonPath("$.senhaHash").doesNotExist());
		mvc.perform(get("/clientes/me/pedidos").header("Authorization", token)).andExpect(status().isForbidden());

		for (String statusAprovacao : List.of("aprovado", "reprovado")) {
			jdbc.update("UPDATE prestador SET status_aprovacao = ? WHERE id_prestador = ?", statusAprovacao, id);
			entityManager.clear();
			mvc.perform(get(PERFIL).header("Authorization", token)).andExpect(status().isOk())
					.andExpect(jsonPath("$.statusAprovacao").value(statusAprovacao));
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "04.252.011/0001-10", "12.abc.345/01de-35" })
	void gravaEmpresaOpcionalComCnpjNumericoOuAlfanumerico(String cnpj) throws Exception {
		var dados = dadosValidos();
		dados.put("empresa", Map.of("razaoSocial", " Oficina de Teste ", "nomeFantasia", " ", "cnpj", cnpj));
		int id = cadastrar(dados);
		assertThat(jdbc.queryForObject("SELECT cnpj FROM empresa WHERE id_prestador = ?", String.class, id))
				.isEqualTo(cnpj.toUpperCase(java.util.Locale.ROOT).replaceAll("[./-]", ""));
		assertThat(jdbc.queryForObject("SELECT razao_social FROM empresa WHERE id_prestador = ?", String.class, id))
				.isEqualTo("Oficina de Teste");
	}

	@Test
	void rejeitaCategoriaInexistenteECnpjInvalidoAntesDeCriarConta() throws Exception {
		var dados = dadosValidos();
		dados.put("idCategorias", List.of(Integer.MAX_VALUE));
		enviar(dados).andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.idCategorias").exists());
		dados.put("idCategorias", List.of(categoria.getId()));
		dados.put("empresa", Map.of("razaoSocial", "Oficina de Teste", "cnpj", "12ABC34501DE36"));
		enviar(dados).andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos['empresa.cnpj']").exists());
		assertThat(usuarios.existsByEmail(EMAIL)).isFalse();
	}

	@Test
	void rejeitaCnpjDuplicadoSemCriarOutraConta() throws Exception {
		var dados = dadosValidos();
		dados.put("empresa", Map.of("razaoSocial", "Oficina de Teste", "cnpj", "12ABC34501DE35"));
		cadastrar(dados);
		dados.put("email", "us12-duplicado@example.invalid");
		dados.put("cpf", "11144477735");
		enviar(dados).andExpect(status().isConflict()).andExpect(jsonPath("$.campos['empresa.cnpj']").exists());
		assertThat(usuarios.existsByEmail("us12-duplicado@example.invalid")).isFalse();
	}

	@Test
	void exigePrestadorAutenticadoParaConsultarPerfil() throws Exception {
		mvc.perform(get(PERFIL)).andExpect(status().isUnauthorized());
		var cliente = usuarios.saveAndFlush(new Usuario("us12-cliente@example.invalid", encoder.encode(SENHA), "cliente", true));
		mvc.perform(get(PERFIL).header("Authorization", "Bearer " + tokens.emitir(cliente).accessToken()))
				.andExpect(status().isForbidden());
		enviar(Map.of()).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.idCategorias").exists())
				.andExpect(jsonPath("$.campos.cidade").exists()).andExpect(jsonPath("$.campos.uf").exists());
	}

	private Map<String, Object> dadosValidos() {
		return new HashMap<>(Map.of(
				"nome", " Prestador de Teste ", "email", EMAIL, "cpf", "529.982.247-25",
				"telefone", "(11) 99999-9999", "senha", SENHA, "idCategorias", List.of(categoria.getId()),
				"cidade", "São Paulo", "uf", "sp"));
	}

	private ResultActions enviar(Map<String, ?> dados) throws Exception {
		return mvc.perform(post(CADASTRO).contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(dados)));
	}

	private int cadastrar(Map<String, Object> dados) throws Exception {
		JsonNode body = json.readTree(enviar(dados).andExpect(status().isCreated())
				.andExpect(jsonPath("$.statusAprovacao").value("pendente"))
				.andReturn().getResponse().getContentAsString());
		return body.get("idPrestador").asInt();
	}

	private String login() throws Exception {
		var response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(Map.of("email", EMAIL, "senha", SENHA))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.usuario.tipoUsuario").value("prestador"))
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + json.readTree(response).get("accessToken").asText();
	}
}
