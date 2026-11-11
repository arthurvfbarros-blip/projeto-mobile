package br.com.appservicos.backend.chat;

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
import java.util.UUID;

@Entity
@Table(name = "mensagem")
public class Mensagem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensagem")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_agendamento", nullable = false)
    private Agendamento agendamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_remetente", nullable = false)
    private Usuario remetente;

    @Column(name = "id_envio", nullable = false)
    private UUID idEnvio;

    @Column(nullable = false, columnDefinition = "text")
    private String conteudo;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Mensagem() {}

    public Mensagem(
            Agendamento agendamento,
            Usuario remetente,
            UUID idEnvio,
            String conteudo,
            Instant criadoEm) {
        this.agendamento = agendamento;
        this.remetente = remetente;
        this.idEnvio = idEnvio;
        this.conteudo = conteudo;
        this.criadoEm = criadoEm;
    }

    public Integer getId() {
        return id;
    }

    public Agendamento getAgendamento() {
        return agendamento;
    }

    public Usuario getRemetente() {
        return remetente;
    }

    public UUID getIdEnvio() {
        return idEnvio;
    }

    public String getConteudo() {
        return conteudo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
