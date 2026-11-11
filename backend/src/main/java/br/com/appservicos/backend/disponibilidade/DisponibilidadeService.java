package br.com.appservicos.backend.disponibilidade;

import br.com.appservicos.backend.agendamento.AgendamentoElegibilidadeService;
import br.com.appservicos.backend.api.CampoInvalidoException;
import br.com.appservicos.backend.api.ConflitoException;
import br.com.appservicos.backend.cliente.ClienteAutenticadoService;
import br.com.appservicos.backend.disponibilidade.dto.CadastroDisponibilidadeRequest;
import br.com.appservicos.backend.disponibilidade.dto.DisponibilidadeResponse;
import br.com.appservicos.backend.disponibilidade.dto.PaginaDisponibilidadesResponse;
import br.com.appservicos.backend.prestador.PrestadorAutenticadoService;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class DisponibilidadeService {
    private final DisponibilidadeRepository disponibilidades;
    private final PrestadorAutenticadoService prestadores;
    private final ClienteAutenticadoService clientes;
    private final AgendamentoElegibilidadeService elegibilidade;
    private final Clock clock;

    public DisponibilidadeService(
            DisponibilidadeRepository disponibilidades,
            PrestadorAutenticadoService prestadores,
            ClienteAutenticadoService clientes,
            AgendamentoElegibilidadeService elegibilidade,
            Clock clock) {
        this.disponibilidades = disponibilidades;
        this.prestadores = prestadores;
        this.clientes = clientes;
        this.elegibilidade = elegibilidade;
        this.clock = clock;
    }

    @Transactional
    public DisponibilidadeResponse criar(String usuario, CadastroDisponibilidadeRequest request) {
        var p = prestadores.buscar(usuario);
        elegibilidade.exigirPrestadorApto(p);
        if (request.data().getYear() > 9999
                || !LocalDateTime.of(request.data(), request.hora())
                        .isAfter(LocalDateTime.now(clock))) {
            throw new CampoInvalidoException("data", "Informe uma data e um horário futuros");
        }
        if (request.hora().getSecond() != 0 || request.hora().getNano() != 0)
            throw new CampoInvalidoException("hora", "Informe o horário em horas e minutos");
        try {
            return DisponibilidadeResponse.from(
                    disponibilidades.saveAndFlush(
                            new Disponibilidade(p, request.data(), request.hora())));
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException constraint
                        && constraint.getConstraintName() != null
                        && constraint
                                .getConstraintName()
                                .toLowerCase(Locale.ROOT)
                                .contains("uq_disponibilidade_prestador_data_hora")) {
                    throw new ConflitoException(
                            "Este horário já está cadastrado na sua disponibilidade");
                }
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public PaginaDisponibilidadesResponse listarDoPrestador(
            String usuario, int pagina, int tamanho) {
        return listar(prestadores.buscar(usuario).getId(), false, pagina, tamanho);
    }

    @Transactional(readOnly = true)
    public PaginaDisponibilidadesResponse listarParaCliente(
            String usuario, Integer idOrcamento, int pagina, int tamanho) {
        var cliente = clientes.buscar(usuario);
        var o = elegibilidade.buscar(idOrcamento, cliente.getId());
        return listar(o.getPrestador().getId(), true, pagina, tamanho);
    }

    private PaginaDisponibilidadesResponse listar(
            Integer prestador, boolean livres, int pagina, int tamanho) {
        if (pagina < 0)
            throw new CampoInvalidoException("pagina", "A página deve ser maior ou igual a zero");
        if (tamanho < 1 || tamanho > 50)
            throw new CampoInvalidoException("tamanho", "O tamanho deve estar entre 1 e 50");
        var agora = LocalDateTime.now(clock);
        return PaginaDisponibilidadesResponse.from(
                disponibilidades.listarFuturas(
                        prestador,
                        agora.toLocalDate(),
                        agora.toLocalTime(),
                        livres,
                        PageRequest.of(pagina, tamanho, Sort.by("data", "hora", "id"))));
    }
}
