package br.com.appservicos.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.auth.dto.LoginRequest;
import br.com.appservicos.backend.auth.dto.LoginResponse;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;

@Service
public class AuthService {

	private final UsuarioRepository usuarios;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokens;
	private final String hashInexistente;

	public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokens) {
		this.usuarios = usuarios;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
		this.hashInexistente = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		if (request.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new BadCredentialsException("E-mail ou senha inválidos");
		}
		var usuario = usuarios.findByEmail(request.email());
		String hash = usuario.map(Usuario::getSenhaHash).orElse(hashInexistente);
		boolean senhaValida = passwordEncoder.matches(request.senha(), hash);
		if (!senhaValida || usuario.isEmpty() || !usuario.get().isAtivo()) {
			throw new BadCredentialsException("E-mail ou senha inválidos");
		}
		return tokens.emitir(usuario.get());
	}
}
