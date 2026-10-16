package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Conta ou meio usado para pagar uma despesa. O nome é a sigla que vai para a coluna FORMA PAGTO
 * da planilha (ex.: "BB", "CX ECO"); a descrição explica a sigla para quem lança.
 */
@Entity
@Table(name = "forma_pagamento_despesa")
public class FormaPagamentoDespesa extends EntidadeBase {

    @Column(nullable = false, length = 30)
    private String nome;

    @Column(length = 200)
    private String descricao;

    @Column(nullable = false)
    private boolean ativo = true;

    protected FormaPagamentoDespesa() {
    }

    public FormaPagamentoDespesa(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
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

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
