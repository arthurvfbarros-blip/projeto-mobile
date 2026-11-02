package br.com.appservicos.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(@NotBlank String secret, @Positive long expirationSeconds, @NotBlank String issuer) {

	@Override
	public String toString() {
		return "JwtProperties[secret omitido]";
	}
}
