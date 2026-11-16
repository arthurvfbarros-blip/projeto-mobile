package br.com.appservicos.backend.administracao;

import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;
import br.com.appservicos.backend.validacao.Utf8Size;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.admin.bootstrap.enabled", havingValue = "true")
public class AdministradorBootstrap implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final Validator validator;
    private final String email;
    private final String senha;

    public AdministradorBootstrap(UsuarioRepository usuarios, PasswordEncoder encoder, Validator validator,
            @Value("${app.admin.bootstrap.email:}") String email,
            @Value("${app.admin.bootstrap.password:}") String senha) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.validator = validator;
        this.email = email.strip();
        this.senha = senha;
    }

    private record Credenciais(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 12) @Utf8Size(max = 72) String senha) {
        @Override public String toString() { return "Credenciais[omitidas]"; }
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!validator.validate(new Credenciais(email, senha)).isEmpty())
            throw new IllegalStateException("Configure um e-mail válido e uma senha de administrador com pelo menos 12 caracteres e até 72 bytes UTF-8");
        var existente = usuarios.findByEmail(email);
        if (existente.isPresent()) {
            if ("administrador".equals(existente.get().getTipoUsuario()) && existente.get().isAtivo()) return;
            throw new IllegalStateException("O e-mail configurado já pertence a outra conta; nenhuma conta foi alterada");
        }
        if (usuarios.existsByTipoUsuario("administrador"))
            throw new IllegalStateException("Já existe administrador; desative a configuração de acesso inicial");
        usuarios.saveAndFlush(new Usuario(email, encoder.encode(senha), "administrador", true));
    }
}
