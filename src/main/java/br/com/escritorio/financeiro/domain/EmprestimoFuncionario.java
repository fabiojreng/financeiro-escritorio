package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Empréstimo do escritório ao funcionário. As parcelas são lançamentos do tipo
 * {@link TipoLancamentoFuncionario#PARCELA_EMPRESTIMO}, descontados no acerto de cada competência.
 */
@Entity
@Table(name = "emprestimo_funcionario")
public class EmprestimoFuncionario extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "data_concessao", nullable = false)
    private LocalDate dataConcessao;

    @Column(name = "valor_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "numero_parcelas", nullable = false)
    private Integer numeroParcelas;

    /** Primeiro dia do mês da primeira parcela. */
    @Column(name = "primeira_competencia", nullable = false)
    private LocalDate primeiraCompetencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Column(length = 2000)
    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoEmprestimo situacao = SituacaoEmprestimo.EM_ABERTO;

    @Column(name = "quitado_em")
    private LocalDateTime quitadoEm;

    @Column(name = "quitado_por", length = 100)
    private String quitadoPor;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "cancelado_por", length = 100)
    private String canceladoPor;

    protected EmprestimoFuncionario() {
    }

    public EmprestimoFuncionario(Funcionario funcionario, LocalDate dataConcessao, BigDecimal valorTotal,
                                 int numeroParcelas, YearMonth primeiraCompetencia, FormaPagamento formaPagamento) {
        this.funcionario = funcionario;
        this.dataConcessao = dataConcessao;
        this.valorTotal = valorTotal;
        this.numeroParcelas = numeroParcelas;
        this.primeiraCompetencia = primeiraCompetencia.atDay(1);
        this.formaPagamento = formaPagamento;
    }

    public void quitar(String usuario, LocalDateTime quando) {
        exigirEmAberto();
        this.situacao = SituacaoEmprestimo.QUITADO;
        this.quitadoPor = usuario;
        this.quitadoEm = quando;
    }

    public void cancelar(String motivo, String usuario, LocalDateTime quando) {
        exigirEmAberto();
        this.situacao = SituacaoEmprestimo.CANCELADO;
        this.motivoCancelamento = motivo;
        this.canceladoPor = usuario;
        this.canceladoEm = quando;
    }

    private void exigirEmAberto() {
        if (situacao != SituacaoEmprestimo.EM_ABERTO) {
            throw new IllegalStateException("O empréstimo não está em aberto.");
        }
    }

    public boolean isEmAberto() {
        return situacao == SituacaoEmprestimo.EM_ABERTO;
    }

    public YearMonth getPrimeiraCompetencia() {
        return YearMonth.from(primeiraCompetencia);
    }

    public YearMonth getUltimaCompetencia() {
        return getPrimeiraCompetencia().plusMonths(numeroParcelas - 1L);
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public LocalDate getDataConcessao() {
        return dataConcessao;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public Integer getNumeroParcelas() {
        return numeroParcelas;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public SituacaoEmprestimo getSituacao() {
        return situacao;
    }

    public LocalDateTime getQuitadoEm() {
        return quitadoEm;
    }

    public String getQuitadoPor() {
        return quitadoPor;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public LocalDateTime getCanceladoEm() {
        return canceladoEm;
    }

    public String getCanceladoPor() {
        return canceladoPor;
    }
}
