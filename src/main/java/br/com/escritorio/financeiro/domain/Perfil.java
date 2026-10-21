package br.com.escritorio.financeiro.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/** Conjunto de permissões atribuído aos usuários. Cadastrado e editado pela tela de perfis. */
@Entity
@Table(name = "perfil")
public class Perfil extends EntidadeBase {

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(length = 255)
    private String descricao;

    /** Perfil do sistema (Administrador): não pode ser inativado e sempre tem todas as permissões. */
    @Column(nullable = false, updatable = false)
    private boolean sistema;

    @Column(nullable = false)
    private boolean ativo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_permissao", joinColumns = @JoinColumn(name = "perfil_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permissao", nullable = false, length = 50)
    private Set<Permissao> permissoes = new HashSet<>();

    protected Perfil() {
    }

    public Perfil(String nome, String descricao, Collection<Permissao> permissoes) {
        this.nome = nome;
        this.descricao = descricao;
        this.permissoes.addAll(permissoes);
    }

    /** Permissões que o perfil realmente concede; o perfil do sistema recebe todas, inclusive as criadas depois. */
    public Set<Permissao> permissoesEfetivas() {
        if (sistema) {
            return EnumSet.allOf(Permissao.class);
        }
        return permissoes.isEmpty() ? EnumSet.noneOf(Permissao.class) : EnumSet.copyOf(permissoes);
    }

    public boolean concede(Permissao permissao) {
        return sistema || permissoes.contains(permissao);
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

    public boolean isSistema() {
        return sistema;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Set<Permissao> getPermissoes() {
        return permissoes;
    }

    public void setPermissoes(Collection<Permissao> permissoes) {
        this.permissoes.clear();
        this.permissoes.addAll(permissoes);
    }
}
