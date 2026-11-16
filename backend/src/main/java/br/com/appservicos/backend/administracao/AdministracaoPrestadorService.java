package br.com.appservicos.backend.administracao;

import br.com.appservicos.backend.administracao.dto.AnalisePrestadorRequest;
import br.com.appservicos.backend.administracao.dto.PaginaPrestadoresResponse;
import br.com.appservicos.backend.administracao.dto.PrestadorAdministrativoResponse;
import br.com.appservicos.backend.api.CampoInvalidoException;
import br.com.appservicos.backend.api.ConflitoException;
import br.com.appservicos.backend.api.RecursoNaoEncontradoException;
import br.com.appservicos.backend.prestador.Prestador;
import br.com.appservicos.backend.prestador.PrestadorRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministracaoPrestadorService {
    private final AdministradorAutenticadoService administradores;
    private final PrestadorRepository prestadores;
    private final AnalisePrestadorRepository analises;
    private final Clock clock;

    public AdministracaoPrestadorService(AdministradorAutenticadoService administradores,
            PrestadorRepository prestadores, AnalisePrestadorRepository analises, Clock clock) {
        this.administradores = administradores;
        this.prestadores = prestadores;
        this.analises = analises;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PaginaPrestadoresResponse listar(String usuario, String status, int pagina, int tamanho) {
        administradores.buscar(usuario);
        if (!Set.of("pendente", "aprovado", "reprovado").contains(status))
            throw new CampoInvalidoException("status", "Selecione pendente, aprovado ou reprovado");
        if (pagina < 0) throw new CampoInvalidoException("pagina", "Informe uma página válida");
        if (tamanho < 1 || tamanho > 50)
            throw new CampoInvalidoException("tamanho", "O tamanho deve estar entre 1 e 50");
        return PaginaPrestadoresResponse.from(prestadores.findByStatusAprovacao(status,
                PageRequest.of(pagina, tamanho, Sort.by("id"))));
    }

    @Transactional(readOnly = true)
    public PrestadorAdministrativoResponse buscar(String usuario, Integer id) {
        administradores.buscar(usuario);
        return resposta(prestadores.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prestador não encontrado")));
    }

    @Transactional
    public PrestadorAdministrativoResponse analisar(String usuario, Integer id, AnalisePrestadorRequest request) {
        var administrador = administradores.buscar(usuario);
        var p = prestadores.buscarParaAnalise(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Prestador não encontrado"));
        var anterior = analises.findByPrestadorId(id);
        if (anterior.isPresent() && anterior.get().getDecisao().equals(request.decisao())
                && p.getStatusAprovacao().equals(request.decisao())) return resposta(p);
        if (!"pendente".equals(p.getStatusAprovacao()) || anterior.isPresent())
            throw new ConflitoException("Este cadastro já foi analisado. Atualize a tela para consultar a decisão");
        if (!p.getUsuario().isAtivo() || !"prestador".equals(p.getUsuario().getTipoUsuario()))
            throw new ConflitoException("O prestador precisa ter uma conta ativa para ser analisado");
        if ("reprovado".equals(request.decisao()) && request.motivo() == null)
            throw new CampoInvalidoException("motivo", "Informe o motivo da reprovação");
        if ("aprovado".equals(request.decisao()) && (p.getCidade() == null || p.getCidade().isBlank()
                || p.getUf() == null || p.getUf().isBlank() || p.getCategorias().isEmpty()))
            throw new ConflitoException("O prestador precisa informar categorias, cidade e UF antes da aprovação");
        p.analisar(request.decisao());
        analises.saveAndFlush(new AnalisePrestador(p, administrador, request.decisao(),
                "reprovado".equals(request.decisao()) ? request.motivo() : null, Instant.now(clock)));
        return resposta(p);
    }

    private PrestadorAdministrativoResponse resposta(Prestador p) {
        return PrestadorAdministrativoResponse.from(p, analises.findByPrestadorId(p.getId()).orElse(null));
    }
}
