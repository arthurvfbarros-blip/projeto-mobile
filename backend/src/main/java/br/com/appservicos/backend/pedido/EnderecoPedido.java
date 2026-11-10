package br.com.appservicos.backend.pedido;

import br.com.appservicos.backend.endereco.EnderecoCliente;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EnderecoPedido {

	@Column(name = "endereco_cep", length = 8)
	private String cep;
	@Column(name = "endereco_logradouro", length = 150)
	private String logradouro;
	@Column(name = "endereco_numero", length = 20)
	private String numero;
	@Column(name = "endereco_complemento", length = 100)
	private String complemento;
	@Column(name = "endereco_bairro", length = 100)
	private String bairro;
	@Column(name = "endereco_cidade", length = 100)
	private String cidade;
	@Column(name = "endereco_uf", length = 2)
	private String uf;

	protected EnderecoPedido() {
	}

	public EnderecoPedido(EnderecoCliente endereco) {
		cep = endereco.getCep();
		logradouro = endereco.getLogradouro();
		numero = endereco.getNumero();
		complemento = endereco.getComplemento();
		bairro = endereco.getBairro();
		cidade = endereco.getCidade();
		uf = endereco.getUf();
	}

	public String getCep() {
		return cep;
	}

	public String getLogradouro() {
		return logradouro;
	}

	public String getNumero() {
		return numero;
	}

	public String getComplemento() {
		return complemento;
	}

	public String getBairro() {
		return bairro;
	}

	public String getCidade() {
		return cidade;
	}

	public String getUf() {
		return uf;
	}
}
