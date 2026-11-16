package br.com.appservicos.backend.administracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.appservicos.backend.auth.TokenService;
import br.com.appservicos.backend.categoria.CategoriaServico;
import br.com.appservicos.backend.categoria.CategoriaServicoRepository;
import br.com.appservicos.backend.prestador.Prestador;
import br.com.appservicos.backend.prestador.PrestadorRepository;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdministracaoIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PrestadorRepository prestadores;
    @Autowired CategoriaServicoRepository categorias;
    @Autowired AnalisePrestadorRepository analises;
    @Autowired TokenService tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired Validator validator;
    Usuario admin;
    Prestador prestador;
    String bearer;

    @BeforeEach
    void preparar() {
        admin = usuarios.saveAndFlush(new Usuario("admin-analise@example.invalid", encoder.encode("AdminTeste123!"), "administrador", true));
        bearer = "Bearer " + tokens.emitir(admin).accessToken();
        var u = usuarios.saveAndFlush(new Usuario("prestador-analise@example.invalid", encoder.encode("SenhaTeste123!"), "prestador", true));
        var c = categorias.saveAndFlush(new CategoriaServico("Categoria análise", null));
        prestador = prestadores.saveAndFlush(new Prestador(u, "Prestador análise", "52998224725", "81999999999", "Recife", "PE", List.of(c), List.of("Centro")));
    }

    @Test
    void loginReconheceAdministradorEListaApenasSituacaoSolicitadaSemDadosSensiveis() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", admin.getEmail(), "senha", "AdminTeste123!"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.tipoUsuario").value("administrador"));
        mvc.perform(get("/admin/prestadores").header("Authorization", bearer).param("tamanho", "1"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.totalItens").value(1)).andExpect(jsonPath("$.itens[0].perfil.idPrestador").value(prestador.getId()))
                .andExpect(jsonPath("$.itens[0].perfil.cpf").doesNotExist())
                .andExpect(jsonPath("$.itens[0].senhaHash").doesNotExist());
        mvc.perform(get("/admin/prestadores").header("Authorization", bearer).param("status", "aprovado"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cliente", "prestador"})
    void somenteAdministradorPodeListarConsultarOuAnalisar(String tipo) throws Exception {
        var u = usuarios.saveAndFlush(new Usuario("intruso-" + tipo + "@example.invalid", "hash", tipo, true));
        var token = "Bearer " + tokens.emitir(u).accessToken();
        mvc.perform(get("/admin/prestadores").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get(rota()).header("Authorization", token)).andExpect(status().isForbidden());
        decidir(token, "aprovado", "").andExpect(status().isForbidden());
        assertThat(analises.count()).isZero();
        assertThat(prestador.getStatusAprovacao()).isEqualTo("pendente");
    }

    @Test
    void visitanteNaoPodeAcessarPainelNemDecidir() throws Exception {
        mvc.perform(get("/admin/prestadores")).andExpect(status().isUnauthorized());
        mvc.perform(post(rota() + "/analise").contentType(MediaType.APPLICATION_JSON)
                .content("{\"decisao\":\"aprovado\"}")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"inativo", "tipo_alterado", "excluido"})
    void tokenAntigoNaoPreservaPermissaoAdministrativa(String mudanca) throws Exception {
        if (mudanca.equals("inativo")) jdbc.update("UPDATE usuario SET ativo = false WHERE id_usuario = ?", admin.getId());
        else if (mudanca.equals("tipo_alterado")) jdbc.update("UPDATE usuario SET tipo_usuario = 'cliente' WHERE id_usuario = ?", admin.getId());
        else jdbc.update("DELETE FROM usuario WHERE id_usuario = ?", admin.getId());
        em.clear();
        mvc.perform(get("/admin/prestadores").header("Authorization", bearer)).andExpect(status().isForbidden());
        mvc.perform(get(rota()).header("Authorization", bearer)).andExpect(status().isForbidden());
        decidir(bearer, "aprovado", "").andExpect(status().isForbidden());
    }

    @Test
    void aprovaRegistraResponsavelEPermiteOportunidadesSemNovoLogin() throws Exception {
        var tokenPrestador = "Bearer " + tokens.emitir(prestador.getUsuario()).accessToken();
        mvc.perform(get("/prestadores/me/oportunidades").header("Authorization", tokenPrestador)).andExpect(status().isForbidden());
        decidir(bearer, "aprovado", "").andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil.statusAprovacao").value("aprovado"))
                .andExpect(jsonPath("$.analise.responsavel").value(admin.getEmail()))
                .andExpect(jsonPath("$.analise.analisadaEm").exists())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
        mvc.perform(get("/prestadores/me/oportunidades").header("Authorization", tokenPrestador)).andExpect(status().isOk());
        assertThat(analises.count()).isEqualTo(1);
    }

    @Test
    void reprovaComMotivoVisivelSomenteAoProprioPrestadorEAdministracao() throws Exception {
        decidir(bearer, "reprovado", "   Cadastre os dados profissionais corretamente.  ")
                .andExpect(status().isOk()).andExpect(jsonPath("$.analise.motivo").value("Cadastre os dados profissionais corretamente."));
        mvc.perform(get("/prestadores/me").header("Authorization", "Bearer " + tokens.emitir(prestador.getUsuario()).accessToken()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.motivoReprovacao").value("Cadastre os dados profissionais corretamente."))
                .andExpect(jsonPath("$.analise.responsavel").doesNotExist());
        mvc.perform(get(rota()).header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.analise.decisao").value("reprovado"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void naoReprovaSemMotivo(String motivo) throws Exception {
        decidir(bearer, "reprovado", motivo).andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.motivo").exists());
        assertThat(analises.count()).isZero();
        assertThat(prestador.getStatusAprovacao()).isEqualTo("pendente");
    }

    @Test
    void repeticaoNaoDuplicaAuditoriaENovaDecisaoConflitanteNaoSobrescreve() throws Exception {
        decidir(bearer, "aprovado", "").andExpect(status().isOk());
        var registro = analises.findByPrestadorId(prestador.getId()).orElseThrow();
        decidir(bearer, "aprovado", "").andExpect(status().isOk());
        decidir(bearer, "reprovado", "Outra decisão").andExpect(status().isConflict());
        assertThat(analises.count()).isEqualTo(1);
        assertThat(analises.findByPrestadorId(prestador.getId()).orElseThrow().getAnalisadaEm()).isEqualTo(registro.getAnalisadaEm());
    }

    @ParameterizedTest
    @ValueSource(strings = {"inativo", "sem_regiao", "sem_categoria", "ja_aprovado"})
    void impedeAprovacaoDeCadastroInaptoOuJaAnalisado(String situacao) throws Exception {
        switch (situacao) {
            case "inativo" -> jdbc.update("UPDATE usuario SET ativo = false WHERE id_usuario = ?", prestador.getUsuario().getId());
            case "sem_regiao" -> jdbc.update("UPDATE prestador SET cidade = NULL, uf = NULL WHERE id_prestador = ?", prestador.getId());
            case "sem_categoria" -> jdbc.update("DELETE FROM prestador_categoria WHERE id_prestador = ?", prestador.getId());
            default -> jdbc.update("UPDATE prestador SET status_aprovacao = 'aprovado' WHERE id_prestador = ?", prestador.getId());
        }
        em.clear();
        decidir(bearer, "aprovado", "").andExpect(status().isConflict());
        assertThat(analises.count()).isZero();
    }

    @Test
    void validaParametrosIdentificadoresEDecisao() throws Exception {
        for (var params : List.of(Map.of("status", "todos"), Map.of("pagina", "-1"), Map.of("tamanho", "51"), Map.of("tamanho", "0"))) {
            var request = get("/admin/prestadores").header("Authorization", bearer);
            params.forEach(request::param);
            mvc.perform(request).andExpect(status().isBadRequest());
        }
        decidir(bearer, "pendente", "").andExpect(status().isBadRequest());
        decidir(bearer, "reprovado", "x".repeat(501)).andExpect(status().isBadRequest());
        mvc.perform(get("/admin/prestadores/2147483647").header("Authorization", bearer)).andExpect(status().isNotFound());
    }

    @Test
    void cadastroPublicoNuncaCriaAdministradorNemAprovaPrestador() throws Exception {
        mvc.perform(post("/auth/cadastro/prestador").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nome", "Cadastro público", "email", "publico@example.invalid",
                        "cpf", "11144477735", "telefone", "81999999999", "senha", "SenhaTeste123!",
                        "idCategorias", prestador.getCategorias().stream().map(c -> c.getId()).toList(),
                        "cidade", "Recife", "uf", "PE", "tipoUsuario", "administrador", "statusAprovacao", "aprovado"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.statusAprovacao").value("pendente"));
        assertThat(usuarios.findByEmail("publico@example.invalid").orElseThrow().getTipoUsuario()).isEqualTo("prestador");
    }

    @Test
    void bootstrapNuncaPromoveContaExistenteNemCriaSegundoAdministrador() {
        var args = new DefaultApplicationArguments();
        var bootstrap = new AdministradorBootstrap(usuarios, encoder, validator, prestador.getUsuario().getEmail(), "SenhaSegura123!");
        assertThatThrownBy(() -> bootstrap.run(args)).isInstanceOf(IllegalStateException.class);
        assertThat(prestador.getUsuario().getTipoUsuario()).isEqualTo("prestador");
        assertThatThrownBy(() -> new AdministradorBootstrap(usuarios, encoder, validator, "segundo@example.invalid", "SenhaSegura123!").run(args))
                .isInstanceOf(IllegalStateException.class);
        assertThat(usuarios.existsByEmail("segundo@example.invalid")).isFalse();
    }

    @Test
    void bootstrapCriaPrimeiroAdministradorComHashSemRedefinirSenhaAoReiniciar() throws Exception {
        usuarios.delete(admin);
        usuarios.flush();
        var args = new DefaultApplicationArguments();
        var bootstrap = new AdministradorBootstrap(usuarios, encoder, validator, "novo-admin@example.invalid", "SenhaSegura123!");
        bootstrap.run(args);
        var criado = usuarios.findByEmail("novo-admin@example.invalid").orElseThrow();
        assertThat(criado.getTipoUsuario()).isEqualTo("administrador");
        assertThat(encoder.matches("SenhaSegura123!", criado.getSenhaHash())).isTrue();
        new AdministradorBootstrap(usuarios, encoder, validator, "novo-admin@example.invalid", "OutraSenha123!").run(args);
        assertThat(usuarios.findByEmail(criado.getEmail()).orElseThrow().getSenhaHash()).isEqualTo(criado.getSenhaHash());
    }

    @ParameterizedTest
    @ValueSource(strings = {"curta", "ááááááááááááááááááááááááááááááááááááá"})
    void bootstrapRejeitaSenhaInvalidaSemCriarConta(String senha) {
        assertThatThrownBy(() -> new AdministradorBootstrap(usuarios, encoder, validator, "novo-admin@example.invalid", senha)
                .run(new DefaultApplicationArguments())).isInstanceOf(IllegalStateException.class);
        assertThat(usuarios.existsByEmail("novo-admin@example.invalid")).isFalse();
    }

    private String rota() { return "/admin/prestadores/" + prestador.getId(); }

    private ResultActions decidir(String token, String decisao, String motivo) throws Exception {
        return mvc.perform(post(rota() + "/analise").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("decisao", decisao, "motivo", motivo))));
    }
}
