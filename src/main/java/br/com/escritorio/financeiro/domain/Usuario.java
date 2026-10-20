package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario extends EntidadeBase {

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "perfil_id", nullable = false)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Usuario() {
    }

    public Usuario(String username, String nome, String senhaHash, Perfil perfil) {
        this.username = username;
        this.nome = nome;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    public boolean possui(Permissao permissao) {
        return perfil.concede(permissao);
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

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
