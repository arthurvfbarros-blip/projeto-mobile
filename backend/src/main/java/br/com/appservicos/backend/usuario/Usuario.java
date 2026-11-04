package br.com.appservicos.backend.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Integer id;

	@Column(nullable = false, unique = true, length = 150)
	private String email;

	@Column(name = "senha_hash", nullable = false, length = 255)
	private String senhaHash;

	@Column(name = "tipo_usuario", nullable = false, length = 20)
	private String tipoUsuario;

	@Column(nullable = false)
	private boolean ativo;

	protected Usuario() {
	}

	public Usuario(String email, String senhaHash, String tipoUsuario, boolean ativo) {
		this.email = email;
		this.senhaHash = senhaHash;
		this.tipoUsuario = tipoUsuario;
		this.ativo = ativo;
	}

	public Integer getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public String getTipoUsuario() {
		return tipoUsuario;
	}

	public boolean isAtivo() {
		return ativo;
	}
}
