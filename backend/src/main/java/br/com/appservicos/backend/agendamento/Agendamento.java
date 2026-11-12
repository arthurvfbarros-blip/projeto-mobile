package br.com.appservicos.backend.agendamento;

import br.com.appservicos.backend.contratacao.Contratacao;
import br.com.appservicos.backend.disponibilidade.Disponibilidade;
import br.com.appservicos.backend.endereco.EnderecoCliente;
import br.com.appservicos.backend.orcamento.Orcamento;
import br.com.appservicos.backend.pedido.EnderecoPedido;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "agendamento")
public class Agendamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_agendamento")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orcamento", nullable = false, unique = true)
    private Orcamento orcamento;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_disponibilidade", nullable = false, unique = true)
    private Disponibilidade disponibilidade;

    @Column(nullable = false, length = 15)
    private String status;

    @Embedded private EnderecoPedido endereco;

    @OneToOne(mappedBy = "agendamento", fetch = FetchType.LAZY)
    private Contratacao contratacao;

    public Contratacao getContratacao() {
        return contratacao;
    }

    public void registrarContratacao(Contratacao contratacao) {
        this.contratacao = contratacao;
    }

    @Column(name = "conversa_iniciada_em")
    private Instant conversaIniciadaEm;

    public void iniciarConversa(Instant instante) {
        if (conversaIniciadaEm == null) conversaIniciadaEm = instante;
    }

    public String getSituacaoConversa() {
        return !"confirmado".equals(status)
                ? null
                : conversaIniciadaEm == null ? "aguardando_conversa" : "em_conversa";
    }

    protected Agendamento() {}

    public Agendamento(
            Orcamento orcamento, Disponibilidade disponibilidade, EnderecoCliente endereco) {
        this.orcamento = orcamento;
        this.disponibilidade = disponibilidade;
        this.endereco = new EnderecoPedido(endereco);
        this.status = "confirmado";
    }

    public Integer getId() {
        return id;
    }

    public Orcamento getOrcamento() {
        return orcamento;
    }

    public Disponibilidade getDisponibilidade() {
        return disponibilidade;
    }

    public String getStatus() {
        return status;
    }

    public EnderecoPedido getEndereco() {
        return endereco;
    }
}
