package br.com.appservicos.backend.prestador;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Integer> {

	boolean existsByCnpj(String cnpj);
}
