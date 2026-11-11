package br.com.appservicos.backend.chat;

import br.com.appservicos.backend.agendamento.Agendamento;
import br.com.appservicos.backend.agendamento.AgendamentoRepository;
import br.com.appservicos.backend.api.RecursoNaoEncontradoException;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatAcessoService {
    private final UsuarioRepository usuarios;
    private final AgendamentoRepository agendamentos;

    public ChatAcessoService(UsuarioRepository usuarios, AgendamentoRepository agendamentos) {
        this.usuarios = usuarios;
        this.agendamentos = agendamentos;
    }

    @Transactional(readOnly = true)
    public Usuario usuario(String identificador) {
        Integer id;
        try {
            id = Integer.valueOf(identificador);
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("Conta não autorizada");
        }
        var u =
                usuarios.findById(id)
                        .orElseThrow(() -> new AccessDeniedException("Conta não autorizada"));
        if (!u.isAtivo()
                || !("cliente".equals(u.getTipoUsuario())
                        || "prestador".equals(u.getTipoUsuario())))
            throw new AccessDeniedException("Conta não autorizada");
        return u;
    }

    @Transactional(readOnly = true)
    public Agendamento buscar(String identificador, Integer id) {
        var u = usuario(identificador);
        var a =
                agendamentos
                        .buscarChatDoParticipante(id, u.getId(), u.getTipoUsuario())
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Conversa não encontrada"));
        exigirConfirmado(a);
        return a;
    }

    public void exigirConfirmado(Agendamento a) {
        var e = a.getEndereco();
        if (!"confirmado".equals(a.getStatus())
                || e == null
                || java.util.stream.Stream.of(
                                e.getCep(),
                                e.getLogradouro(),
                                e.getNumero(),
                                e.getBairro(),
                                e.getCidade(),
                                e.getUf())
                        .anyMatch(valor -> valor == null || valor.isBlank()))
            throw new ChatIndisponivelException();
    }
}
