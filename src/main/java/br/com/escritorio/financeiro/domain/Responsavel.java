package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Pessoa responsável pelos pagamentos da agenda. Não precisa ter acesso ao sistema.
 */
@Entity
@Table(name = "responsavel")
public class Responsavel extends EntidadeBase {

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 1000)
    private String observacoes;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Responsavel() {
    }

    public Responsavel(String nome, String observacoes) {
        this.nome = nome;
        this.observacoes = observacoes;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
