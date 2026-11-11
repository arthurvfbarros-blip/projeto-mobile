package br.com.appservicos.backend.prestador.dto;

import java.util.List;
import java.util.Locale;

import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

import br.com.appservicos.backend.validacao.Utf8Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CadastroPrestadorRequest(
		@NotBlank(message = "Informe seu nome")
		@Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
		String nome,
		@NotBlank(message = "Informe o e-mail")
		@Email(message = "Informe um e-mail válido")
		@Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
		String email,
		@NotBlank(message = "Informe o CPF")
		@Pattern(regexp = "[0-9]{11}", message = "Informe um CPF válido")
		@CPF(message = "Informe um CPF válido")
		String cpf,
		@NotBlank(message = "Informe o telefone")
		@Pattern(regexp = "[0-9]{10,11}", message = "Informe o telefone com DDD")
		String telefone,
		@NotBlank(message = "Informe a senha")
		@Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres")
		@Utf8Size(max = 72, message = "A senha deve ter no máximo 72 bytes UTF-8")
		String senha,
		@NotEmpty(message = "Selecione ao menos uma categoria")
		@Size(max = 50, message = "Selecione até 50 categorias")
		List<@NotNull(message = "Selecione uma categoria válida")
				@Positive(message = "Selecione uma categoria válida") Integer> idCategorias,
		@NotBlank(message = "Informe a cidade")
		@Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
		String cidade,
		@NotBlank(message = "Informe a UF")
		@Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MG|MS|MT|PA|PB|PE|PI|PR|RJ|RN|RO|RR|RS|SC|SE|SP|TO",
				message = "Informe uma UF válida")
		String uf,
		@Size(max = 30, message = "Informe até 30 bairros")
		List<@NotBlank(message = "Informe um bairro válido")
				@Size(max = 100, message = "Cada bairro deve ter no máximo 100 caracteres") String> bairros,
		@Valid EmpresaRequest empresa) {

	public CadastroPrestadorRequest {
		nome = limpar(nome);
		email = limpar(email);
		if (cpf != null) cpf = cpf.strip().replace(".", "").replace("-", "");
		if (telefone != null) telefone = telefone.strip().replaceAll("[()\\s-]", "");
		cidade = normalizarLocal(cidade);
		if (uf != null) uf = uf.strip().toUpperCase(Locale.ROOT);
		bairros = bairros == null ? List.of() : bairros.stream().map(CadastroPrestadorRequest::normalizarLocal).toList();
	}

	public record EmpresaRequest(
			@NotBlank(message = "Informe a razão social")
			@Size(max = 200, message = "A razão social deve ter no máximo 200 caracteres")
			String razaoSocial,
			@Size(max = 200, message = "O nome fantasia deve ter no máximo 200 caracteres")
			String nomeFantasia,
			@NotBlank(message = "Informe o CNPJ")
			@Pattern(regexp = "[0-9A-Z]{12}[0-9]{2}", message = "Informe um CNPJ válido")
			@CNPJ(format = CNPJ.Format.ALPHANUMERIC, message = "Informe um CNPJ válido")
			String cnpj) {

		public EmpresaRequest {
			razaoSocial = limpar(razaoSocial);
			nomeFantasia = limpar(nomeFantasia);
			if (nomeFantasia != null && nomeFantasia.isEmpty()) nomeFantasia = null;
			if (cnpj != null) cnpj = cnpj.strip().toUpperCase(Locale.ROOT).replaceAll("[./-]", "");
		}

		@Override
		public String toString() {
			return "EmpresaRequest";
		}
	}

	private static String limpar(String valor) {
		return valor == null ? null : valor.strip();
	}

	private static String normalizarLocal(String valor) {
		return valor == null ? null : valor.strip().replaceAll("\\s+", " ");
	}

	@Override
	public String toString() {
		return "CadastroPrestadorRequest";
	}
}
