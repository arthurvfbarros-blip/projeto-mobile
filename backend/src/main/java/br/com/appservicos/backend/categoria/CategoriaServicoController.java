package br.com.appservicos.backend.categoria;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.appservicos.backend.categoria.dto.CategoriaResponse;

@RestController
@RequestMapping("/categorias")
public class CategoriaServicoController {

	private final CategoriaServicoService service;

	public CategoriaServicoController(CategoriaServicoService service) {
		this.service = service;
	}

	@GetMapping
	public ResponseEntity<List<CategoriaResponse>> listar() {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listar());
	}
}
