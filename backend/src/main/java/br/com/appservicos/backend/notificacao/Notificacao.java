package br.com.appservicos.backend.notificacao;

import br.com.appservicos.backend.agendamento.Agendamento;
import br.com.appservicos.backend.usuario.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notificacao")
public class Notificacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacao")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_agendamento", nullable = false)
    private Agendamento agendamento;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 250)
    private String mensagem;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "lida_em")
    private Instant lidaEm;

    protected Notificacao() {}

    public Notificacao(Agendamento a, Instant agora) {
        this.agendamento = a;
        this.usuario = a.getOrcamento().getPrestador().getUsuario();
        this.tipo = "contratacao_confirmada";
        this.mensagem = "Contratação confirmada: " + a.getOrcamento().getPedido().getTitulo();
        this.criadaEm = agora;
    }

    public Integer getId() {
        return id;
    }

    public Agendamento getAgendamento() {
        return agendamento;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getTipo() {
        return tipo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getLidaEm() {
        return lidaEm;
    }

    public void marcarLida(Instant agora) {
        if (lidaEm == null) lidaEm = agora;
    }
}
