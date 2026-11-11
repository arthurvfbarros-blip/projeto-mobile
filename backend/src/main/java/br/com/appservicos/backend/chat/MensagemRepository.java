package br.com.appservicos.backend.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MensagemRepository extends JpaRepository<Mensagem, Integer> {
    Optional<Mensagem> findByAgendamentoIdAndRemetenteIdAndIdEnvio(
            Integer agendamento, Integer remetente, UUID idEnvio);

    @Query(
            "select m from Mensagem m where m.agendamento.id=:agendamento and (:cursor is null or"
                + " m.id<:cursor) order by m.id desc")
    List<Mensagem> anteriores(
            @Param("agendamento") Integer agendamento,
            @Param("cursor") Integer cursor,
            Pageable page);

    @Query(
            "select m from Mensagem m where m.agendamento.id=:agendamento and m.id>:cursor order by"
                + " m.id asc")
    List<Mensagem> posteriores(
            @Param("agendamento") Integer agendamento,
            @Param("cursor") Integer cursor,
            Pageable page);
}
