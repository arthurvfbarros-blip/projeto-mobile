package br.com.appservicos.backend.cliente;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

	boolean existsByCpf(String cpf);
}
