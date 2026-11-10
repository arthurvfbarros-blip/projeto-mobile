package br.com.appservicos.backend.endereco.dto;

import java.util.Locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroEnderecoRequest(
		@NotBlank(message = "Informe o CEP")
		@Pattern(regexp = "[0-9]{8}", message = "Informe um CEP com 8 dígitos")
		String cep,
		@NotBlank(message = "Informe o logradouro")
		@Size(max = 150, message = "O logradouro deve ter no máximo 150 caracteres")
		String logradouro,
		@NotBlank(message = "Informe o número ou S/N")
		@Size(max = 20, message = "O número deve ter no máximo 20 caracteres")
		String numero,
		@Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres")
		String complemento,
		@NotBlank(message = "Informe o bairro")
		@Size(max = 100, message = "O bairro deve ter no máximo 100 caracteres")
		String bairro,
		@NotBlank(message = "Informe a cidade")
		@Size(max = 100, message = "A cidade deve ter no máximo 100 caracteres")
		String cidade,
		@NotBlank(message = "Informe a UF")
		@Pattern(regexp = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO",
				message = "Informe uma UF válida")
		String uf) {

	public CadastroEnderecoRequest {
		cep = limpar(cep);
		if (cep != null && cep.matches("[0-9]{5}-[0-9]{3}")) cep = cep.replace("-", "");
		logradouro = limpar(logradouro);
		numero = limpar(numero);
		complemento = limpar(complemento);
		if (complemento != null && complemento.isEmpty()) complemento = null;
		bairro = limpar(bairro);
		cidade = limpar(cidade);
		uf = limpar(uf);
		if (uf != null) uf = uf.toUpperCase(Locale.ROOT);
	}

	private static String limpar(String valor) {
		return valor == null ? null : valor.strip();
	}
}
