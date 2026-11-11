package br.com.appservicos.backend.orcamento;

import java.math.BigDecimal;

import br.com.appservicos.backend.pedido.PedidoServico;
import br.com.appservicos.backend.prestador.Prestador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "orcamento")
public class Orcamento {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_orcamento")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_pedido", nullable = false)
	private PedidoServico pedido;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_prestador", nullable = false)
	private Prestador prestador;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal valor;
	@Column(name = "prazo_dias")
	private Integer prazoDias;
	@Column(columnDefinition = "text")
	private String descricao;
	@Column(nullable = false, length = 15)
	private String status;

	protected Orcamento() { }

	public Orcamento(PedidoServico pedido, Prestador prestador, BigDecimal valor, Integer prazoDias, String descricao) {
		this.pedido = pedido;
		this.prestador = prestador;
		this.valor = valor;
		this.prazoDias = prazoDias;
		this.descricao = descricao;
		this.status = "enviado";
	}

	public void aceitar() { status = "aceito"; }
	public void recusar() { status = "recusado"; }

	public Integer getId() { return id; }
	public PedidoServico getPedido() { return pedido; }
	public Prestador getPrestador() { return prestador; }
	public BigDecimal getValor() { return valor; }
	public Integer getPrazoDias() { return prazoDias; }
	public String getDescricao() { return descricao; }
	public String getStatus() { return status; }
}
