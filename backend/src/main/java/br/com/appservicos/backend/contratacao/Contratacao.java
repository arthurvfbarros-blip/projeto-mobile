package br.com.appservicos.backend.contratacao;

import br.com.appservicos.backend.agendamento.Agendamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "contratacao")
public class Contratacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contratacao")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_agendamento", nullable = false, unique = true)
    private Agendamento agendamento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "confirmada_em", nullable = false)
    private Instant confirmadaEm;

    protected Contratacao() {}

    public Contratacao(Agendamento agendamento, Instant confirmadaEm) {
        this.agendamento = agendamento;
        this.valor = agendamento.getOrcamento().getValor();
        this.confirmadaEm = confirmadaEm;
    }

    public Integer getId() {
        return id;
    }

    public Agendamento getAgendamento() {
        return agendamento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Instant getConfirmadaEm() {
        return confirmadaEm;
    }
}
