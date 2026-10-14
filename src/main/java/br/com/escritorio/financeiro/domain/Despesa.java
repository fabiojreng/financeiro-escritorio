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
 * Gasto avulso já pago pelo escritório (conta paga). Cada despesa ativa vira uma linha da planilha
 * "Contas Pagas" do mês da data de pagamento. Não se mistura com honorários, repasses nem com o
 * benefício do INSS.
 */
@Entity
@Table(name = "despesa")
public class Despesa extends EntidadeBase {

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaDespesa categoria;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forma_pagamento_id", nullable = false)
    private FormaPagamentoDespesa formaPagamento;

    @Column(nullable = false, length = 200)
    private String historico;

    @Column(name = "numero_documento", length = 100)
    private String numeroDocumento;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 200)
    private String fornecedor;

    @Column(length = 2000)
    private String observacoes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoDespesa situacao = SituacaoDespesa.ATIVA;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "cancelado_por", length = 100)
    private String canceladoPor;

    protected Despesa() {
    }

    public Despesa(LocalDate dataPagamento, CategoriaDespesa categoria, FormaPagamentoDespesa formaPagamento,
                   String historico, BigDecimal valor, String fornecedor) {
        this.dataPagamento = dataPagamento;
        this.categoria = categoria;
        this.formaPagamento = formaPagamento;
        this.historico = historico;
        this.valor = valor;
        this.fornecedor = fornecedor;
    }

    /** Cancela mantendo o registro: a despesa sai da planilha, mas continua no histórico. */
    public void cancelar(String motivo, String usuario, LocalDateTime quando) {
        if (situacao == SituacaoDespesa.CANCELADA) {
            throw new IllegalStateException("A despesa já está cancelada.");
        }
        this.situacao = SituacaoDespesa.CANCELADA;
        this.motivoCancelamento = motivo;
        this.canceladoPor = usuario;
        this.canceladoEm = quando;
    }

    /** Mês da planilha em que a despesa entra. */
    public YearMonth getCompetencia() {
        return YearMonth.from(dataPagamento);
    }

    public boolean isAtiva() {
        return situacao == SituacaoDespesa.ATIVA;
    }

    public boolean isCancelada() {
        return situacao == SituacaoDespesa.CANCELADA;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public CategoriaDespesa getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaDespesa categoria) {
        this.categoria = categoria;
    }

    public FormaPagamentoDespesa getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamentoDespesa formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getHistorico() {
        return historico;
    }

    public void setHistorico(String historico) {
        this.historico = historico;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(String fornecedor) {
        this.fornecedor = fornecedor;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public SituacaoDespesa getSituacao() {
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
