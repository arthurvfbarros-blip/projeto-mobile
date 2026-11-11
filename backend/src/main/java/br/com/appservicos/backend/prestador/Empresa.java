package br.com.appservicos.backend.prestador;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "empresa")
public class Empresa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_empresa")
	private Integer id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_prestador", nullable = false, unique = true)
	private Prestador prestador;

	@Column(name = "razao_social", nullable = false, length = 200)
	private String razaoSocial;

	@Column(name = "nome_fantasia", length = 200)
	private String nomeFantasia;

	@Column(nullable = false, unique = true, length = 14)
	private String cnpj;

	protected Empresa() {
	}

	public Empresa(Prestador prestador, String razaoSocial, String nomeFantasia, String cnpj) {
		this.prestador = prestador;
		this.razaoSocial = razaoSocial;
		this.nomeFantasia = nomeFantasia;
		this.cnpj = cnpj;
	}
}
