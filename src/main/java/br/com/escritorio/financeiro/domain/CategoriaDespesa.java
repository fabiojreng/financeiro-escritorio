package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Categoria da coluna CATEGORIA da planilha da contabilidade. O histórico padrão é o texto que a
 * contabilidade espera na coluna HISTORICO e é sugerido ao lançar uma despesa.
 */
@Entity
@Table(name = "categoria_despesa")
public class CategoriaDespesa extends EntidadeBase {

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "historico_padrao", length = 200)
    private String historicoPadrao;

    @Column(nullable = false)
    private boolean ativo = true;

    protected CategoriaDespesa() {
    }

    public CategoriaDespesa(String nome, String historicoPadrao) {
        this.nome = nome;
        this.historicoPadrao = historicoPadrao;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getHistoricoPadrao() {
        return historicoPadrao;
    }

    public void setHistoricoPadrao(String historicoPadrao) {
        this.historicoPadrao = historicoPadrao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
