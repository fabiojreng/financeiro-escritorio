package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Funcionário do escritório. O salário base é só a referência do acerto mensal; a folha (INSS, FGTS,
 * férias, 13º) continua com a contabilidade.
 */
@Entity
@Table(name = "funcionario")
public class Funcionario extends EntidadeBase {

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 11)
    private String cpf;

    @Column(length = 100)
    private String cargo;

    @Column(name = "data_admissao", nullable = false)
    private LocalDate dataAdmissao;

    @Column(name = "data_desligamento")
    private LocalDate dataDesligamento;

    @Column(name = "salario_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal salarioBase;

    @Column(name = "dia_pagamento", nullable = false)
    private Integer diaPagamento;

    @Column(length = 100)
    private String banco;

    @Column(length = 20)
    private String agencia;

    @Column(length = 30)
    private String conta;

    @Column(name = "chave_pix", length = 150)
    private String chavePix;

    @Column(length = 2000)
    private String observacoes;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Funcionario() {
    }

    public Funcionario(String nome, LocalDate dataAdmissao, BigDecimal salarioBase, Integer diaPagamento) {
        this.nome = nome;
        this.dataAdmissao = dataAdmissao;
        this.salarioBase = salarioBase;
        this.diaPagamento = diaPagamento;
    }

    /** O funcionário entra no acerto do mês se estava admitido e ainda não desligado em algum dia do mês. */
    public boolean trabalhouEm(YearMonth competencia) {
        return !dataAdmissao.isAfter(competencia.atEndOfMonth())
                && (dataDesligamento == null || !dataDesligamento.isBefore(competencia.atDay(1)));
    }

    public String getCpfFormatado() {
        return Cpf.formatar(cpf);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public LocalDate getDataAdmissao() {
        return dataAdmissao;
    }

    public void setDataAdmissao(LocalDate dataAdmissao) {
        this.dataAdmissao = dataAdmissao;
    }

    public LocalDate getDataDesligamento() {
        return dataDesligamento;
    }

    public void setDataDesligamento(LocalDate dataDesligamento) {
        this.dataDesligamento = dataDesligamento;
    }

    public BigDecimal getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(BigDecimal salarioBase) {
        this.salarioBase = salarioBase;
    }

    public Integer getDiaPagamento() {
        return diaPagamento;
    }

    public void setDiaPagamento(Integer diaPagamento) {
        this.diaPagamento = diaPagamento;
    }

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getAgencia() {
        return agencia;
    }

    public void setAgencia(String agencia) {
        this.agencia = agencia;
    }

    public String getConta() {
        return conta;
    }

    public void setConta(String conta) {
        this.conta = conta;
    }

    public String getChavePix() {
        return chavePix;
    }

    public void setChavePix(String chavePix) {
        this.chavePix = chavePix;
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
