package br.com.appservicos.backend.administracao;

import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministradorAutenticadoService {
    private final UsuarioRepository usuarios;

    public AdministradorAutenticadoService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public Usuario buscar(String identificador) {
        Integer id;
        try {
            id = Integer.valueOf(identificador);
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("Administrador não autorizado");
        }
        return usuarios.findById(id)
                .filter(u -> u.isAtivo() && "administrador".equals(u.getTipoUsuario()))
                .orElseThrow(() -> new AccessDeniedException("Administrador não autorizado"));
    }
}
