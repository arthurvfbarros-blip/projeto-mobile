package br.com.appservicos.backend.administracao;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalisePrestadorRepository extends JpaRepository<AnalisePrestador, Integer> {
    Optional<AnalisePrestador> findByPrestadorId(Integer idPrestador);
}
