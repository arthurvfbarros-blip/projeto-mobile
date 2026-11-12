package br.com.appservicos.backend.notificacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Integer> {
    Page<Notificacao> findByUsuarioIdAndLidaEmIsNull(Integer usuario, Pageable page);

    Optional<Notificacao> findByIdAndUsuarioId(Integer id, Integer usuario);

    @Modifying
    @Query(
            "update Notificacao n set n.lidaEm=:agora where n.id=:id and n.usuario.id=:usuario and"
                + " n.lidaEm is null")
    int marcarLida(
            @Param("id") Integer id,
            @Param("usuario") Integer usuario,
            @Param("agora") Instant agora);
}
