package br.com.appservicos.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthIntegrationTests.RotaProtegidaDeTeste.class)
class AuthIntegrationTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private UsuarioRepository usuarios;

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JwtProperties jwtProperties;

	@BeforeEach
	void limparUsuariosDeTeste() {
		usuarios.deleteAll();
	}

	@ParameterizedTest
	@ValueSource(strings = { "cliente", "prestador" })
	void autenticaUsuarioAtivoEEmiteTokenUtilizavel(String tipo) throws Exception {
		Usuario usuario = cadastrarUsuario(tipo, true);
		String response = mvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("  teste@example.com  ", "SenhaTeste123!")))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andExpect(jsonPath("$.usuario.id").value(usuario.getId()))
				.andExpect(jsonPath("$.usuario.email").value("teste@example.com"))
				.andExpect(jsonPath("$.usuario.tipoUsuario").value(tipo))
				.andExpect(jsonPath("$.usuario.senhaHash").doesNotExist())
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		JsonNode body = json.readTree(response);
		String token = body.get("accessToken").asText();
		Jwt jwt = jwtDecoder.decode(token);
		assertThat(jwt.getSubject()).isEqualTo(usuario.getId().toString());
		assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plusSeconds(3600));
		assertThat(jwt.getClaimAsString("tipoUsuario")).isEqualTo(tipo);
		assertThat(response).doesNotContain("SenhaTeste123!", usuario.getSenhaHash());

		mvc.perform(get("/teste/protegido").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(usuario.getId().toString()))
				.andExpect(jsonPath("$.permissoes").value(hasItem("ROLE_" + tipo)));
	}

	@Test
	void rejeitaSenhaIncorreta() throws Exception {
		cadastrarUsuario("cliente", true);
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("teste@example.com", "SenhaErrada123!")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"))
				.andExpect(jsonPath("$.accessToken").doesNotExist());
	}

	@Test
	void rejeitaUsuarioInexistenteSemRevelarSeEmailExiste() throws Exception {
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("inexistente@example.com", "SenhaTeste123!")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
	}

	@Test
	void rejeitaUsuarioInativoMesmoComSenhaCorreta() throws Exception {
		cadastrarUsuario("cliente", false);
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("teste@example.com", "SenhaTeste123!")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos"));
	}

	@Test
	void naoAceitaSenhaArmazenadaComoTextoPuro() throws Exception {
		usuarios.save(new Usuario("teste@example.com", "SenhaTeste123!", "cliente", true));
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("teste@example.com", "SenhaTeste123!")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejeitaCamposVazios() throws Exception {
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.email").exists())
				.andExpect(jsonPath("$.campos.senha").exists());
	}

	@Test
	void rejeitaCamposAusentes() throws Exception {
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.email").exists())
				.andExpect(jsonPath("$.campos.senha").exists());
	}

	@Test
	void rejeitaEmailMalformado() throws Exception {
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("email-invalido", "SenhaTeste123!")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.email").exists());
	}

	@Test
	void rejeitaSenhaAcimaDoLimite() throws Exception {
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("teste@example.com", "a".repeat(73))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.senha").exists());
	}

	@Test
	void rejeitaSenhaUnicodeAcimaDe72BytesSemErroInterno() throws Exception {
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(credenciais("teste@example.com", "á".repeat(40))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejeitaJsonMalformado() throws Exception {
		mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value("Corpo da requisição inválido"));
	}

	@Test
	void exigeTokenNaRotaProtegida() throws Exception {
		mvc.perform(get("/teste/protegido")).andExpect(status().isUnauthorized());
	}

	@Test
	void rejeitaTokenMalformado() throws Exception {
		mvc.perform(get("/teste/protegido").header("Authorization", "Bearer invalido"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejeitaTokenExpirado() throws Exception {
		String token = emitirToken(Instant.now().minusSeconds(120), jwtProperties.issuer());
		mvc.perform(get("/teste/protegido").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejeitaTokenDeOutroEmissor() throws Exception {
		String token = emitirToken(Instant.now().plusSeconds(3600), "outro-emissor");
		mvc.perform(get("/teste/protegido").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejeitaTokenComAssinaturaAdulterada() throws Exception {
		String token = emitirToken(Instant.now().plusSeconds(3600), jwtProperties.issuer());
		int signatureStart = token.lastIndexOf('.') + 1;
		String adulterado = token.substring(0, signatureStart)
				+ (token.charAt(signatureStart) == 'A' ? 'B' : 'A') + token.substring(signatureStart + 1);
		mvc.perform(get("/teste/protegido").header("Authorization", "Bearer " + adulterado))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void permitePreflightDaOrigemConfigurada() throws Exception {
		mvc.perform(options("/auth/login")
				.header("Origin", "http://localhost:8081")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
	}

	@Test
	void rejeitaPreflightDeOrigemNaoConfigurada() throws Exception {
		mvc.perform(options("/auth/login")
				.header("Origin", "https://outra-origem.example")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden());
	}

	@Test
	void mantemHealthPublico() throws Exception {
		mvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	private Usuario cadastrarUsuario(String tipo, boolean ativo) {
		return usuarios.save(new Usuario("teste@example.com", encoder.encode("SenhaTeste123!"), tipo, ativo));
	}

	private String credenciais(String email, String senha) throws Exception {
		return json.writeValueAsString(Map.of("email", email, "senha", senha));
	}

	private String emitirToken(Instant expiresAt, String issuer) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer).subject("1").issuedAt(expiresAt.minusSeconds(3600)).expiresAt(expiresAt)
				.claim("tipoUsuario", "cliente").build();
		return jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}

	@RestController
	static class RotaProtegidaDeTeste {
		@GetMapping("/teste/protegido")
		Map<String, Object> protegido(Authentication authentication) {
			return Map.of("id", authentication.getName(), "permissoes",
					authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
		}
	}
}
