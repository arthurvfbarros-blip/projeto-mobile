package br.com.appservicos.backend.pedido;

import java.time.Instant;
import java.time.LocalDate;

import br.com.appservicos.backend.categoria.CategoriaServico;
import br.com.appservicos.backend.cliente.Cliente;
import br.com.appservicos.backend.endereco.EnderecoCliente;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedido_servico")
public class PedidoServico {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_pedido")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cliente", nullable = false)
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_categoria", nullable = false)
	private CategoriaServico categoria;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_endereco")
	private EnderecoCliente endereco;

	@Embedded
	private EnderecoPedido enderecoPedido;

	@Column(nullable = false, length = 150)
	private String titulo;

	@Column(nullable = false, columnDefinition = "text")
	private String descricao;

	@Column(nullable = false, length = 25)
	private String status;

	@Column(name = "data_desejada")
	private LocalDate dataDesejada;

	@Column(name = "criado_em")
	private Instant criadoEm;

	protected PedidoServico() {
	}

	public PedidoServico(Cliente cliente, CategoriaServico categoria, EnderecoCliente endereco,
			String titulo, String descricao, LocalDate dataDesejada, Instant criadoEm) {
		this.cliente = cliente;
		this.categoria = categoria;
		this.endereco = endereco;
		this.enderecoPedido = new EnderecoPedido(endereco);
		this.titulo = titulo;
		this.descricao = descricao;
		this.status = "aberto";
		this.dataDesejada = dataDesejada;
		this.criadoEm = criadoEm;
	}

	public Cliente getCliente() { return cliente; }
	public void agendar() { status = "agendado"; }

	public void receberOrcamento() {
		if ("aberto".equals(status)) status = "em_orcamento";
	}

	public Integer getId() {
		return id;
	}

	public CategoriaServico getCategoria() {
		return categoria;
	}

	public EnderecoPedido getEnderecoPedido() {
		return enderecoPedido;
	}

	public String getTitulo() {
		return titulo;
	}

	public String getDescricao() {
		return descricao;
	}

	public String getStatus() {
		return status;
	}

	public LocalDate getDataDesejada() {
		return dataDesejada;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}
}
