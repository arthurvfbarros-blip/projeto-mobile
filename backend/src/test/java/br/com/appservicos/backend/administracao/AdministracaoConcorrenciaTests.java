package br.com.appservicos.backend.administracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.com.appservicos.backend.auth.TokenService;
import br.com.appservicos.backend.categoria.CategoriaServico;
import br.com.appservicos.backend.categoria.CategoriaServicoRepository;
import br.com.appservicos.backend.prestador.Prestador;
import br.com.appservicos.backend.prestador.PrestadorRepository;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:admin-concorrencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdministracaoConcorrenciaTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PrestadorRepository prestadores;
    @Autowired CategoriaServicoRepository categorias;
    @Autowired TokenService tokens;
    @Autowired PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc;
    private static final AtomicInteger sequencia = new AtomicInteger();
    private record Cadastro(Integer prestador, String token1, String token2) {}

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void decisoesSimultaneasPreservamUmaUnicaAnalise(boolean mesmaDecisao) throws Exception {
        var cadastro = new TransactionTemplate(transactions).execute(tx -> {
            var n = sequencia.incrementAndGet();
            var a1 = usuarios.saveAndFlush(new Usuario("admin-corrida1-" + n + "@example.invalid", "hash", "administrador", true));
            var a2 = usuarios.saveAndFlush(new Usuario("admin-corrida2-" + n + "@example.invalid", "hash", "administrador", true));
            var u = usuarios.saveAndFlush(new Usuario("prestador-corrida-" + n + "@example.invalid", "hash", "prestador", true));
            var c = categorias.saveAndFlush(new CategoriaServico("Categoria análise corrida " + n, null));
            var p = prestadores.saveAndFlush(new Prestador(u, "Prestador corrida", "123456789" + String.format("%02d", n), "81999999999", "Recife", "PE", List.of(c), List.of()));
            return new Cadastro(p.getId(), "Bearer " + tokens.emitir(a1).accessToken(), "Bearer " + tokens.emitir(a2).accessToken());
        });
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = List.of(0, 1).stream().map(i -> executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return mvc.perform(post("/admin/prestadores/" + cadastro.prestador() + "/analise")
                        .header("Authorization", i == 0 ? cadastro.token1() : cadastro.token2())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(i == 0 || mesmaDecisao ? "{\"decisao\":\"aprovado\"}" : "{\"decisao\":\"reprovado\",\"motivo\":\"Dados incompletos\"}"))
                        .andReturn().getResponse().getStatus();
            })).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var resultados = List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
            assertThat(resultados).containsExactlyInAnyOrder(200, mesmaDecisao ? 200 : 409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analise_prestador WHERE id_prestador = ?", Integer.class, cadastro.prestador())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT p.status_aprovacao = a.decisao FROM prestador p JOIN analise_prestador a ON a.id_prestador = p.id_prestador WHERE p.id_prestador = ?", Boolean.class, cadastro.prestador())).isTrue();
    }
}
