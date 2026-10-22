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
 * Movimentação financeira do funcionário (vale, desconto, extra, reembolso ou parcela de empréstimo).
 * Entra no acerto da competência informada, que por padrão é o mês da data.
 */
@Entity
@Table(name = "lancamento_funcionario")
public class LancamentoFuncionario extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(nullable = false)
    private LocalDate data;

    /** Primeiro dia do mês do acerto. */
    @Column(nullable = false)
    private LocalDate competencia;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoLancamentoFuncionario tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamento formaPagamento;

    @Column(length = 200)
    private String descricao;

    @Column(length = 2000)
    private String observacoes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emprestimo_id")
    private EmprestimoFuncionario emprestimo;

    @Column(name = "numero_parcela")
    private Integer numeroParcela;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoLancamentoFuncionario situacao = SituacaoLancamentoFuncionario.ATIVO;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "cancelado_por", length = 100)
    private String canceladoPor;

    protected LancamentoFuncionario() {
    }

    public LancamentoFuncionario(Funcionario funcionario, TipoLancamentoFuncionario tipo, LocalDate data,
                                 YearMonth competencia, BigDecimal valor) {
        this.funcionario = funcionario;
        this.tipo = tipo;
        this.data = data;
        this.competencia = competencia.atDay(1);
        this.valor = valor;
    }

    public static LancamentoFuncionario parcela(EmprestimoFuncionario emprestimo, Integer numero, YearMonth competencia,
                                                BigDecimal valor, String descricao) {
        LancamentoFuncionario parcela = new LancamentoFuncionario(emprestimo.getFuncionario(),
                TipoLancamentoFuncionario.PARCELA_EMPRESTIMO, competencia.atDay(1), competencia, valor);
        parcela.emprestimo = emprestimo;
        parcela.numeroParcela = numero;
        parcela.descricao = descricao;
        return parcela;
    }

    /** Cancela mantendo o registro: o lançamento sai do acerto, mas continua no histórico. */
    public void cancelar(String motivo, String usuario, LocalDateTime quando) {
        if (situacao == SituacaoLancamentoFuncionario.CANCELADO) {
            throw new IllegalStateException("O lançamento já está cancelado.");
        }
        this.situacao = SituacaoLancamentoFuncionario.CANCELADO;
        this.motivoCancelamento = motivo;
        this.canceladoPor = usuario;
        this.canceladoEm = quando;
    }

    public boolean isAtivo() {
        return situacao == SituacaoLancamentoFuncionario.ATIVO;
    }

    public boolean isCancelado() {
        return situacao == SituacaoLancamentoFuncionario.CANCELADO;
    }

    /** Valor com o sinal do efeito no líquido a pagar. */
    public BigDecimal getValorComSinal() {
        return tipo.isSoma() ? valor : valor.negate();
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public YearMonth getCompetencia() {
        return YearMonth.from(competencia);
    }

    public void setCompetencia(YearMonth competencia) {
        this.competencia = competencia.atDay(1);
    }

    public TipoLancamentoFuncionario getTipo() {
        return tipo;
    }

    public void setTipo(TipoLancamentoFuncionario tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public EmprestimoFuncionario getEmprestimo() {
        return emprestimo;
    }

    public Integer getNumeroParcela() {
        return numeroParcela;
    }

    public SituacaoLancamentoFuncionario getSituacao() {
        return situacao;
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
