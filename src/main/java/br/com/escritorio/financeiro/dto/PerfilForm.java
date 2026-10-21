package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Permissao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.EnumSet;
import java.util.Set;

public class PerfilForm {

    static final int TAMANHO_MAXIMO_NOME = 60;
    private static final String PREFIXO_COPIA = "Cópia de ";

    private Long id;

    @NotBlank(message = "Informe o nome do perfil.")
    @Size(max = TAMANHO_MAXIMO_NOME, message = "O nome pode ter no máximo 60 caracteres.")
    private String nome;

    @Size(max = 255, message = "A descrição pode ter no máximo 255 caracteres.")
    private String descricao;

    private Set<Permissao> permissoes = EnumSet.noneOf(Permissao.class);

    /** Perfil do sistema: só a descrição pode ser alterada. */
    private boolean sistema;

    /** Perfil usado como base na duplicação; fica registrado na auditoria. */
    private Long origemId;

    public static PerfilForm de(Perfil perfil) {
        PerfilForm form = new PerfilForm();
        form.id = perfil.getId();
        form.nome = perfil.getNome();
        form.descricao = perfil.getDescricao();
        form.permissoes = perfil.permissoesEfetivas();
        form.sistema = perfil.isSistema();
        return form;
    }

    /** Formulário de um perfil novo, com as mesmas permissões do perfil de origem. */
    public static PerfilForm copiaDe(Perfil origem) {
        PerfilForm form = new PerfilForm();
        String nome = PREFIXO_COPIA + origem.getNome();
        form.nome = nome.length() > TAMANHO_MAXIMO_NOME ? nome.substring(0, TAMANHO_MAXIMO_NOME) : nome;
        form.descricao = origem.getDescricao();
        form.permissoes = origem.permissoesEfetivas();
        form.origemId = origem.getId();
        return form;
    }

    public boolean isNovo() {
        return id == null;
    }

    public boolean isDuplicacao() {
        return isNovo() && origemId != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Set<Permissao> getPermissoes() {
        return permissoes;
    }

    public void setPermissoes(Set<Permissao> permissoes) {
        this.permissoes = permissoes == null || permissoes.isEmpty()
                ? EnumSet.noneOf(Permissao.class)
                : EnumSet.copyOf(permissoes);
    }

    public boolean isSistema() {
        return sistema;
    }

    public void setSistema(boolean sistema) {
        this.sistema = sistema;
    }

    public Long getOrigemId() {
        return origemId;
    }

    public void setOrigemId(Long origemId) {
        this.origemId = origemId;
    }
}
