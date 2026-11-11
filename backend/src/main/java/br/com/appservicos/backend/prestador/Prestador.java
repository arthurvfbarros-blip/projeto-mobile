package br.com.appservicos.backend.prestador;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import br.com.appservicos.backend.categoria.CategoriaServico;
import br.com.appservicos.backend.usuario.Usuario;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "prestador")
public class Prestador {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_prestador")
	private Integer id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_usuario", nullable = false, unique = true)
	private Usuario usuario;

	@Column(nullable = false, length = 150)
	private String nome;

	@Column(nullable = false, unique = true, length = 11)
	private String cpf;

	@Column(length = 20)
	private String telefone;

	@Column(name = "status_aprovacao", nullable = false, length = 20)
	private String statusAprovacao;

	@Column(length = 100)
	private String cidade;

	@Column(length = 2)
	private String uf;

	@ManyToMany
	@JoinTable(name = "prestador_categoria",
			joinColumns = @JoinColumn(name = "id_prestador"),
			inverseJoinColumns = @JoinColumn(name = "id_categoria"))
	private Set<CategoriaServico> categorias = new HashSet<>();

	@ElementCollection
	@CollectionTable(name = "prestador_bairro", joinColumns = @JoinColumn(name = "id_prestador"))
	@Column(name = "bairro", nullable = false, length = 100)
	private Set<String> bairros = new HashSet<>();

	protected Prestador() {
	}

	public Prestador(Usuario usuario, String nome, String cpf, String telefone, String cidade, String uf,
			Collection<CategoriaServico> categorias, Collection<String> bairros) {
		this.usuario = usuario;
		this.nome = nome;
		this.cpf = cpf;
		this.telefone = telefone;
		this.cidade = cidade;
		this.uf = uf;
		this.statusAprovacao = "pendente";
		this.categorias.addAll(categorias);
		Set<String> bairrosUnicos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		bairrosUnicos.addAll(bairros);
		this.bairros.addAll(bairrosUnicos);
	}

	public Integer getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public String getNome() {
		return nome;
	}

	public String getStatusAprovacao() {
		return statusAprovacao;
	}

	public String getCidade() {
		return cidade;
	}

	public String getUf() {
		return uf;
	}

	public Set<CategoriaServico> getCategorias() {
		return categorias;
	}

	public Set<String> getBairros() {
		return bairros;
	}
}
