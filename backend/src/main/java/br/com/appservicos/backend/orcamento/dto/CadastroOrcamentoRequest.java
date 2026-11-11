package br.com.appservicos.backend.orcamento.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CadastroOrcamentoRequest(
		@NotNull(message = "Informe o preço do serviço")
		@DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
		@Digits(integer = 8, fraction = 2, message = "Informe um valor de até R$ 99.999.999,99 com no máximo duas casas decimais")
		BigDecimal valor,
		@NotNull(message = "Informe o prazo de execução em dias")
		@Positive(message = "O prazo deve ser maior que zero")
		@DecimalMax(value = "2147483647", message = "Informe um prazo válido")
		@Digits(integer = 10, fraction = 0, message = "Informe o prazo em dias inteiros") BigDecimal prazoDias,
		@Size(max = 5000, message = "As observações devem ter no máximo 5000 caracteres") String descricao) {
	public CadastroOrcamentoRequest {
		if (descricao != null) descricao = descricao.strip();
		if (descricao != null && descricao.isEmpty()) descricao = null;
	}
}
