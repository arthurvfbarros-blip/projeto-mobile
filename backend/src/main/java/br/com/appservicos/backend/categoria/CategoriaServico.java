package br.com.appservicos.backend.categoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categoria_servico")
public class CategoriaServico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_categoria")
	private Integer id;

	@Column(nullable = false, unique = true, length = 100)
	private String nome;

	@Column(columnDefinition = "text")
	private String descricao;

	protected CategoriaServico() {
	}

	public CategoriaServico(String nome, String descricao) {
		this.nome = nome;
		this.descricao = descricao;
	}

	public Integer getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getDescricao() {
		return descricao;
	}
}
