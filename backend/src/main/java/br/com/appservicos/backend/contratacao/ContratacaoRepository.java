package br.com.appservicos.backend.contratacao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContratacaoRepository extends JpaRepository<Contratacao, Integer> {
    Optional<Contratacao> findByAgendamentoId(Integer id);
}
