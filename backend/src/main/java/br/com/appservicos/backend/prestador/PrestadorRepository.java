package br.com.appservicos.backend.prestador;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PrestadorRepository extends JpaRepository<Prestador, Integer> {

	boolean existsByCpf(String cpf);

	@EntityGraph(attributePaths = "usuario")
	Optional<Prestador> findByUsuarioId(Integer idUsuario);

	@EntityGraph(attributePaths = "usuario")
	Page<Prestador> findByStatusAprovacao(String statusAprovacao, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Prestador p where p.id = :id")
	Optional<Prestador> buscarParaAnalise(Integer id);
}
