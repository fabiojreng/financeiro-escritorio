package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Lugar onde as perícias acontecem, por exemplo a agência do INSS do Grajaú. */
@Entity
@Table(name = "local_pericia")
public class LocalPericia extends EntidadeBase {

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 300)
    private String endereco;

    @Column(nullable = false)
    private boolean ativo = true;

    protected LocalPericia() {
    }

    public LocalPericia(String nome, String endereco) {
        this.nome = nome;
        this.endereco = endereco;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
