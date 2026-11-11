package br.com.appservicos.backend.orcamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrcamentoIntegrationTests {
	@Autowired private MockMvc mvc;
	@Autowired private JdbcTemplate jdbc;
	@Autowired private EntityManager em;
	@Autowired private TokenService tokens;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private ClienteRepository clientes;
	@Autowired private PrestadorRepository prestadores;
	@Autowired private CategoriaServicoRepository categorias;
	@Autowired private EnderecoClienteRepository enderecos;
	@Autowired private PedidoServicoRepository pedidos;

	private Cliente cliente;
	private Prestador prestador;
	private PedidoServico pedido;
	private String tokenPrestador;
	private String tokenCliente;

	@BeforeEach
	void preparar() {
		var categoria = categorias.saveAndFlush(new CategoriaServico("Elétrica US15", null));
		var contaCliente = usuarios.saveAndFlush(new Usuario("us15-cliente@example.invalid", "hash", "cliente", true));
		cliente = clientes.saveAndFlush(new Cliente(contaCliente, "Cliente privado US15", "52998224725", "11987654321"));
		var endereco = enderecos.saveAndFlush(new EnderecoCliente(cliente, "01310100", "Rua Particular US15", "42", "Casa", "Bela Vista", "São Paulo", "SP"));
		pedido = pedidos.saveAndFlush(new PedidoServico(cliente, categoria, endereco, "Instalação US15", "Preciso instalar um chuveiro novo no banheiro.", LocalDate.now().plusDays(2), Instant.now()));
		var conta = usuarios.saveAndFlush(new Usuario("us15-prestador@example.invalid", "hash", "prestador", true));
		prestador = prestadores.saveAndFlush(new Prestador(conta, "Prestador US15", "11144477735", "11912345678", "Sao Paulo", "SP", List.of(categoria), List.of("Bela Vista")));
		jdbc.update("UPDATE prestador SET status_aprovacao='aprovado' WHERE id_prestador=?", prestador.getId());
		tokenPrestador = bearer(conta);
		tokenCliente = bearer(contaCliente);
		em.clear();
	}

	@Test
	void enviaPersisteAtualizaPedidoEExpõeSomenteDadosNecessarios() throws Exception {
		String body = mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON)
				.content("{\"valor\":180.50,\"prazoDias\":2,\"descricao\":\"  Inclui instalação.  \",\"status\":\"aceito\",\"idPrestador\":999}"))
				.andExpect(status().isCreated()).andExpect(header().string("Location", rota()))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.valor").value(180.5)).andExpect(jsonPath("$.prazoDias").value(2))
				.andExpect(jsonPath("$.descricao").value("Inclui instalação."))
				.andExpect(jsonPath("$.status").value("enviado"))
				.andExpect(jsonPath("$.prestador.idPrestador").value(prestador.getId()))
				.andReturn().getResponse().getContentAsString();
		assertThat(body).doesNotContain("telefone", "email", "cpf", "endereco", "Rua Particular", "senha", "Cliente privado");
		assertThat(jdbc.queryForObject("SELECT status FROM pedido_servico WHERE id_pedido=?", String.class, pedido.getId())).isEqualTo("em_orcamento");
		mvc.perform(get(rota()).header("Authorization", tokenPrestador)).andExpect(status().isOk()).andExpect(jsonPath("$.tituloPedido").value("Instalação US15"));
		mvc.perform(get(rotaCliente()).header("Authorization", tokenCliente))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
				.andExpect(jsonPath("$.itens[0].prestador.nome").value("Prestador US15"));
	}

	@Test
	void rejeitaDuplicadoSemAlterarValorOuCriarOutroRegistro() throws Exception {
		enviar();
		mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON).content("{\"valor\":999,\"prazoDias\":1}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.mensagem").value("Você já enviou um orçamento para este pedido"));
		assertThat(jdbc.queryForObject("SELECT count(*) FROM orcamento WHERE id_pedido=?", Integer.class, pedido.getId())).isEqualTo(1);
		mvc.perform(get(rota()).header("Authorization", tokenPrestador)).andExpect(jsonPath("$.valor").value(180.5));
	}

	@ParameterizedTest
	@ValueSource(strings={"{}", "{\"valor\":0,\"prazoDias\":1}", "{\"valor\":-1,\"prazoDias\":1}", "{\"valor\":1.001,\"prazoDias\":1}", "{\"valor\":100000000,\"prazoDias\":1}", "{\"valor\":180}", "{\"valor\":180,\"prazoDias\":0}", "{\"valor\":180,\"prazoDias\":-1}", "{\"valor\":180,\"prazoDias\":1.5}", "{\"valor\":180,\"prazoDias\":2147483648}"})
	void rejeitaValoresInvalidosSemAlterarPedido(String body) throws Exception {
		mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
		assertThat(jdbc.queryForObject("SELECT count(*) FROM orcamento WHERE id_pedido=?", Integer.class, pedido.getId())).isZero();
		assertThat(jdbc.queryForObject("SELECT status FROM pedido_servico WHERE id_pedido=?", String.class, pedido.getId())).isEqualTo("aberto");
	}

	@Test
	void validaDescricaoEPermiteObservacoesOpcionais() throws Exception {
		mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON)
				.content("{\"valor\":180,\"prazoDias\":2,\"descricao\":\""+"a".repeat(5001)+"\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.descricao").exists());
		mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON)
				.content("{\"valor\":0.01,\"prazoDias\":1,\"descricao\":\"   \"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.descricao").isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings={"pendente","reprovado"})
	void exigePrestadorAprovado(String status) throws Exception {
		jdbc.update("UPDATE prestador SET status_aprovacao=? WHERE id_prestador=?", status, prestador.getId());em.clear();
		mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isForbidden());
	}

	@Test
	void exigeContaAtivaEMesmoTipoMesmoComTokenEmitidoAntes() throws Exception {
		jdbc.update("UPDATE usuario SET ativo=FALSE WHERE id_usuario=?", prestador.getUsuario().getId());em.clear();
		mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isForbidden());
		jdbc.update("UPDATE usuario SET ativo=TRUE,tipo_usuario='cliente' WHERE id_usuario=?", prestador.getUsuario().getId());em.clear();
		mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isForbidden());
	}

	@Test
	void exigeCategoriaCidadeEBairroCompativeisENaoExpoePedidoAlheio() throws Exception {
		for(var coluna:List.of("endereco_cidade", "endereco_bairro", "endereco_uf")) {
			jdbc.update("UPDATE pedido_servico SET "+coluna+"=? WHERE id_pedido=?", coluna.equals("endereco_uf")?"RJ":"Outro local", pedido.getId());em.clear();
			mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isNotFound());
			jdbc.update("UPDATE pedido_servico SET endereco_cidade='São Paulo',endereco_bairro='Bela Vista',endereco_uf='SP' WHERE id_pedido=?",pedido.getId());em.clear();
		}
		var outra=categorias.saveAndFlush(new CategoriaServico("Outra US15",null));
		jdbc.update("UPDATE pedido_servico SET id_categoria=? WHERE id_pedido=?",outra.getId(),pedido.getId());em.clear();
		mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isNotFound());
		mvc.perform(post("/prestadores/me/oportunidades/2147483647/orcamento").header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isNotFound());
	}

	@ParameterizedTest
	@ValueSource(strings={"aguardando_confirmacao","agendado","concluido","cancelado"})
	void impedeEnvioAposFechamentoMasPreservaConsultaDoProprioHistorico(String status) throws Exception {
		enviar();
		jdbc.update("UPDATE pedido_servico SET status=? WHERE id_pedido=?", status,pedido.getId());em.clear();
		mvc.perform(post(rota()).header("Authorization", tokenPrestador).contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isNotFound());
		mvc.perform(get(rota()).header("Authorization", tokenPrestador)).andExpect(status().isOk());
		mvc.perform(get(rotaCliente()).header("Authorization", tokenCliente)).andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1));
	}

	@Test
	void isolaContasEPapeis() throws Exception {
		enviar();
		mvc.perform(post(rota()).contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isUnauthorized());
		mvc.perform(get(rota())).andExpect(status().isUnauthorized());
		mvc.perform(get(rotaCliente())).andExpect(status().isUnauthorized());
		mvc.perform(post(rota()).header("Authorization",tokenCliente).contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isForbidden());
		mvc.perform(get(rota()).header("Authorization",tokenCliente)).andExpect(status().isForbidden());
		mvc.perform(get(rotaCliente()).header("Authorization",tokenPrestador)).andExpect(status().isForbidden());
		var conta=usuarios.saveAndFlush(new Usuario("us15-outro-cliente@example.invalid","hash","cliente",true));
		clientes.saveAndFlush(new Cliente(conta,"Outro cliente","93541134780",null));
		mvc.perform(get(rotaCliente()).header("Authorization",bearer(conta))).andExpect(status().isNotFound());
		var outro=usuarios.saveAndFlush(new Usuario("us15-outro-prestador@example.invalid","hash","prestador",true));
		prestadores.saveAndFlush(new Prestador(outro,"Outro prestador","93541134780",null,"São Paulo","SP",List.of(pedido.getCategoria()),List.of()));
		mvc.perform(get(rota()).header("Authorization",bearer(outro))).andExpect(status().isNotFound());
	}

	@Test
	void paginaOrdenaETrataOrcamentoLegadoComPrazoNulo() throws Exception {
		enviar();
		var conta=usuarios.saveAndFlush(new Usuario("us15-pagina@example.invalid","hash","prestador",true));
		var outro=prestadores.saveAndFlush(new Prestador(conta,"Segundo prestador","93541134780",null,null,null,List.of(),List.of()));
		jdbc.update("INSERT INTO orcamento(id_pedido,id_prestador,valor) VALUES (?,?,220)",pedido.getId(),outro.getId());em.clear();
		mvc.perform(get(rotaCliente()).param("tamanho","1").header("Authorization",tokenCliente))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(2)).andExpect(jsonPath("$.totalPaginas").value(2))
				.andExpect(jsonPath("$.itens[0].prestador.nome").value("Segundo prestador")).andExpect(jsonPath("$.itens[0].prazoDias").isEmpty());
		mvc.perform(get(rotaCliente()).param("tamanho","1").param("pagina","1").header("Authorization",tokenCliente)).andExpect(jsonPath("$.itens[0].valor").value(180.5));
		for(var query:List.of("pagina=-1","tamanho=0","tamanho=51","pagina=abc")) {
			mvc.perform(get(rotaCliente()+"?"+query).header("Authorization",tokenCliente)).andExpect(status().isBadRequest());
		}
		mvc.perform(get(rotaCliente()).param("pagina","100").header("Authorization",tokenCliente)).andExpect(jsonPath("$.itens.length()").value(0));
	}

	private void enviar() throws Exception {
		mvc.perform(post(rota()).header("Authorization",tokenPrestador).contentType(MediaType.APPLICATION_JSON).content(valido())).andExpect(status().isCreated());
	}
	private String valido(){return "{\"valor\":180.50,\"prazoDias\":2}";}
	private String rota(){return "/prestadores/me/oportunidades/"+pedido.getId()+"/orcamento";}
	private String rotaCliente(){return "/clientes/me/pedidos/"+pedido.getId()+"/orcamentos";}
	private String bearer(Usuario conta){return "Bearer "+tokens.emitir(conta).accessToken();}
}
