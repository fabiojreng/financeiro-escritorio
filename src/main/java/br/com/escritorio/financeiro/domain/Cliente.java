package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Locale;

@Entity
@Table(name = "cliente")
public class Cliente extends EntidadeBase {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 11)
    private String cpf;

    @Column(name = "nome_mae", length = 150)
    private String nomeMae;

    @Column(length = 30)
    private String telefone;

    @Column(length = 2000)
    private String observacoes;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDate dataCadastro;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Cliente() {
    }

    public Cliente(String nome, LocalDate dataCadastro) {
        this.nome = maiusculas(nome);
        this.dataCadastro = dataCadastro;
    }

    /** Nome no padrão do sistema: maiúsculas, sem espaços nas pontas. Acentos são mantidos. */
    public static String maiusculas(String nome) {
        return nome == null ? null : nome.trim().toUpperCase(Locale.of("pt", "BR"));
    }

    public String getCpfFormatado() {
        return Cpf.formatar(cpf);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = maiusculas(nome);
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getNomeMae() {
        return nomeMae;
    }

    public void setNomeMae(String nomeMae) {
        this.nomeMae = nomeMae;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDate getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDate dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
