package br.com.appservicos.backend.disponibilidade;

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

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "disponibilidade")
public class Disponibilidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disponibilidade")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_prestador", nullable = false)
    private Prestador prestador;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false, length = 10)
    private String status;

    protected Disponibilidade() {}

    public Disponibilidade(Prestador prestador, LocalDate data, LocalTime hora) {
        this.prestador = prestador;
        this.data = data;
        this.hora = hora;
        this.status = "livre";
    }

    public Integer getId() {
        return id;
    }

    public Prestador getPrestador() {
        return prestador;
    }

    public LocalDate getData() {
        return data;
    }

    public LocalTime getHora() {
        return hora;
    }

    public String getStatus() {
        return status;
    }

    public void reservar() {
        status = "ocupado";
    }
}
