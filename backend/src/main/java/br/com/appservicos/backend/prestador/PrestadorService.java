package br.com.appservicos.backend.prestador;

import java.util.HashSet;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.appservicos.backend.api.CadastroDuplicadoException;
import br.com.appservicos.backend.api.CampoInvalidoException;
import br.com.appservicos.backend.categoria.CategoriaServicoRepository;
import br.com.appservicos.backend.prestador.dto.CadastroPrestadorRequest;
import br.com.appservicos.backend.prestador.dto.CadastroPrestadorResponse;
import br.com.appservicos.backend.prestador.dto.PrestadorResponse;
import br.com.appservicos.backend.usuario.Usuario;
import br.com.appservicos.backend.usuario.UsuarioRepository;

@Service
public class PrestadorService {

	private final UsuarioRepository usuarios;
	private final PrestadorRepository prestadores;
	private final EmpresaRepository empresas;
	private final CategoriaServicoRepository categorias;
	private final PasswordEncoder passwordEncoder;
	private final PrestadorAutenticadoService autenticados;

	public PrestadorService(UsuarioRepository usuarios, PrestadorRepository prestadores, EmpresaRepository empresas,
			CategoriaServicoRepository categorias, PasswordEncoder passwordEncoder, PrestadorAutenticadoService autenticados) {
		this.usuarios = usuarios;
		this.prestadores = prestadores;
		this.empresas = empresas;
		this.categorias = categorias;
		this.passwordEncoder = passwordEncoder;
		this.autenticados = autenticados;
	}

	@Transactional
	public CadastroPrestadorResponse cadastrar(CadastroPrestadorRequest request) {
		if (usuarios.existsByEmail(request.email())) {
			throw new CadastroDuplicadoException("email", "E-mail já cadastrado");
		}
		if (prestadores.existsByCpf(request.cpf())) {
			throw new CadastroDuplicadoException("cpf", "CPF já cadastrado");
		}

		var empresa = request.empresa();
		if (empresa != null && empresas.existsByCnpj(empresa.cnpj())) {
			throw new CadastroDuplicadoException("empresa.cnpj", "CNPJ já cadastrado");
		}
		var ids = new HashSet<>(request.idCategorias());
		var selecionadas = categorias.findAllById(ids);
		if (selecionadas.size() != ids.size()) {
			throw new CampoInvalidoException("idCategorias", "Selecione categorias disponíveis");
		}

		var usuario = usuarios.save(new Usuario(request.email(), passwordEncoder.encode(request.senha()), "prestador", true));
		var prestador = prestadores.save(new Prestador(usuario, request.nome(), request.cpf(), request.telefone(),
				request.cidade(), request.uf(), selecionadas, request.bairros()));
		if (empresa != null) {
			empresas.save(new Empresa(prestador, empresa.razaoSocial(), empresa.nomeFantasia(), empresa.cnpj()));
		}
		prestadores.flush();
		return new CadastroPrestadorResponse(prestador.getId(), prestador.getStatusAprovacao());
	}

	@Transactional(readOnly = true)
	public PrestadorResponse consultarPerfil(String idUsuario) {
		return PrestadorResponse.from(autenticados.buscar(idUsuario));
	}
}
