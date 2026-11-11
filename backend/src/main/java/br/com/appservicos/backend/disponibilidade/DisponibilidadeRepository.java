package br.com.appservicos.backend.disponibilidade;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Disponibilidade d where d.id = :id")
    Optional<Disponibilidade> buscarParaReserva(@Param("id") Integer id);

    @Query(
            "select d from Disponibilidade d where d.prestador.id = :prestador and (d.data > :data"
                + " or (d.data = :data and d.hora > :hora)) and (:somenteLivres = false or d.status"
                + " = 'livre')")
    Page<Disponibilidade> listarFuturas(
            @Param("prestador") Integer prestador,
            @Param("data") LocalDate data,
            @Param("hora") LocalTime hora,
            @Param("somenteLivres") boolean somenteLivres,
            Pageable page);
}
