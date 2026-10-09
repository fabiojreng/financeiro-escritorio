package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UsuarioForm {

    private Long id;

    @NotBlank(message = "Informe o login.")
    @Size(max = 50, message = "O login pode ter no máximo 50 caracteres.")
    @Pattern(regexp = "[A-Za-z0-9._-]*",
            message = "Use apenas letras sem acento, números, ponto, hífen ou sublinhado.")
    private String username;

    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "O nome pode ter no máximo 120 caracteres.")
    private String nome;

    @NotNull(message = "Selecione o perfil.")
    private Perfil perfil = Perfil.OPERADOR;

    private String senha;

    private String confirmacaoSenha;

    public static UsuarioForm de(Usuario usuario) {
        UsuarioForm form = new UsuarioForm();
        form.id = usuario.getId();
        form.username = usuario.getUsername();
        form.nome = usuario.getNome();
        form.perfil = usuario.getPerfil();
        return form;
    }

    public boolean isNovo() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}
