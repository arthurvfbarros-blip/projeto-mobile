package br.com.appservicos.backend.categoria;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaServicoRepository extends JpaRepository<CategoriaServico, Integer> {

	List<CategoriaServico> findAllByOrderByNomeAsc();
}
