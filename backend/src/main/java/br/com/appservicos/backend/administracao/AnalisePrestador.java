package br.com.appservicos.backend.administracao;

import br.com.appservicos.backend.prestador.Prestador;
import br.com.appservicos.backend.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "analise_prestador")
public class AnalisePrestador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_analise")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_prestador", nullable = false, unique = true)
    private Prestador prestador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_administrador", nullable = false)
    private Usuario administrador;

    @Column(nullable = false, length = 20)
    private String decisao;

    @Column(length = 500)
    private String motivo;

    @Column(name = "analisada_em", nullable = false)
    private Instant analisadaEm;

    protected AnalisePrestador() {}

    public AnalisePrestador(Prestador prestador, Usuario administrador, String decisao,
            String motivo, Instant analisadaEm) {
        this.prestador = prestador;
        this.administrador = administrador;
        this.decisao = decisao;
        this.motivo = motivo;
        this.analisadaEm = analisadaEm;
    }

    public Usuario getAdministrador() { return administrador; }
    public String getDecisao() { return decisao; }
    public String getMotivo() { return motivo; }
    public Instant getAnalisadaEm() { return analisadaEm; }
}
