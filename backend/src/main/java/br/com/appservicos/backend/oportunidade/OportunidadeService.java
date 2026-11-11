package br.com.appservicos.backend.oportunidade;

import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.api.CampoInvalidoException;
import br.com.appservicos.backend.api.RecursoNaoEncontradoException;
import br.com.appservicos.backend.categoria.CategoriaServico;
import br.com.appservicos.backend.oportunidade.dto.OportunidadeResponse;
import br.com.appservicos.backend.pedido.PedidoServico;
import br.com.appservicos.backend.pedido.PedidoServicoRepository;
import br.com.appservicos.backend.pedido.dto.PaginaPedidosResponse;
import br.com.appservicos.backend.prestador.AreaAtendimento;
import br.com.appservicos.backend.prestador.Prestador;
import br.com.appservicos.backend.prestador.PrestadorAutenticadoService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;

@Service
public class OportunidadeService {

	private final PrestadorAutenticadoService prestadores;
	private final PedidoServicoRepository pedidos;

	public OportunidadeService(PrestadorAutenticadoService prestadores, PedidoServicoRepository pedidos) {
		this.prestadores = prestadores;
		this.pedidos = pedidos;
	}

	@Transactional(readOnly = true)
	public PaginaPedidosResponse listar(String idUsuario, int pagina, int tamanho) {
		var prestador = buscarApto(idUsuario);
		if (pagina < 0) {
			throw new CampoInvalidoException("pagina", "A página deve ser maior ou igual a zero");
		}
		if (tamanho < 1 || tamanho > 50) {
			throw new CampoInvalidoException("tamanho", "O tamanho deve estar entre 1 e 50");
		}
		return PaginaPedidosResponse.from(pedidos.findAll(compativeis(prestador),
				PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id"))));
	}

	@Transactional(readOnly = true)
	public OportunidadeResponse buscar(String idUsuario, Integer idPedido) {
		var prestador = buscarApto(idUsuario);
		Specification<PedidoServico> identificador = (root, query, cb) -> cb.equal(root.get("id"), idPedido);
		return pedidos.findOne(compativeis(prestador).and(identificador))
				.map(OportunidadeResponse::from)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Oportunidade não encontrada"));
	}

	private Prestador buscarApto(String idUsuario) {
		var prestador = prestadores.buscar(idUsuario);
		if (!"aprovado".equals(prestador.getStatusAprovacao())) {
			throw new AccessDeniedException("Cadastro de prestador não aprovado");
		}
		if (prestador.getCidade() == null || prestador.getCidade().isBlank()
				|| prestador.getUf() == null || prestador.getUf().isBlank()) {
			throw new CampoInvalidoException("cidade", "Informe a cidade e a UF de atendimento no cadastro profissional");
		}
		if (prestador.getCategorias().isEmpty()) {
			throw new CampoInvalidoException("idCategorias", "Informe os serviços oferecidos no cadastro profissional");
		}
		return prestador;
	}

	private Specification<PedidoServico> compativeis(Prestador prestador) {
		var idsCategorias = prestador.getCategorias().stream().map(CategoriaServico::getId).toList();
		var bairros = prestador.getBairros().stream().map(OportunidadeService::normalizarLocal).toList();
		var cidade = normalizarLocal(prestador.getCidade());
		var uf = prestador.getUf().strip().toUpperCase(Locale.ROOT);

		return (root, query, cb) -> {
			var regiao = root.get("enderecoPedido");
			var compativel = cb.and(
					root.get("status").in("aberto", "em_orcamento"),
					root.get("categoria").get("id").in(idsCategorias),
					cb.equal(normalizarLocal(cb, regiao.get("cidade")), cidade),
					cb.equal(cb.upper(cb.trim(regiao.get("uf"))), uf));
			if (!bairros.isEmpty()) {
				compativel = cb.and(compativel, normalizarLocal(cb, regiao.get("bairro")).in(bairros));
			}
			return compativel;
		};
	}

	private static String normalizarLocal(String valor) {
		return AreaAtendimento.normalizar(valor);
	}

	private static Expression<String> normalizarLocal(CriteriaBuilder cb, Expression<String> valor) {
		var semAcentos = cb.function("translate", String.class, cb.lower(valor),
				cb.literal("áàâãäéèêëíìîïóòôõöúùûüçñýÿ"), cb.literal("aaaaaeeeeiiiiooooouuuucnyy"));
		return cb.trim(cb.function("regexp_replace", String.class, semAcentos,
				cb.literal("\\s+"), cb.literal(" "), cb.literal("g")));
	}
}
