package br.com.appservicos.backend.auth;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import br.com.appservicos.backend.auth.dto.LoginResponse;
import br.com.appservicos.backend.auth.dto.LoginResponse.UsuarioResponse;
import br.com.appservicos.backend.usuario.Usuario;

@Service
public class TokenService {

	private final JwtEncoder encoder;
	private final JwtProperties properties;

	public TokenService(JwtEncoder encoder, JwtProperties properties) {
		this.encoder = encoder;
		this.properties = properties;
	}

	public LoginResponse emitir(Usuario usuario) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.subject(usuario.getId().toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(properties.expirationSeconds()))
				.claim("tipoUsuario", usuario.getTipoUsuario())
				.build();
		String token = encoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
		return new LoginResponse(token, "Bearer", properties.expirationSeconds(),
				new UsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getTipoUsuario()));
	}
}
