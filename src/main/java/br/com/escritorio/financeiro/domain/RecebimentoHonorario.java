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

/**
 * Valor de honorário que entrou no caixa do escritório. Nunca é apagado: um lançamento errado é
 * cancelado com motivo e deixa de contar no saldo.
 */
@Entity
@Table(name = "recebimento_honorario")
public class RecebimentoHonorario extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parcela_id", nullable = false, updatable = false)
    private ParcelaHonorario parcela;

    @Column(name = "data_recebimento", nullable = false, updatable = false)
    private LocalDate dataRecebimento;

    @Column(nullable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, updatable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Column(length = 2000)
    private String observacoes;

    @Column(nullable = false)
    private boolean cancelado;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "cancelado_por", length = 100)
    private String canceladoPor;

    protected RecebimentoHonorario() {
    }

    public RecebimentoHonorario(ParcelaHonorario parcela, LocalDate dataRecebimento, BigDecimal valor,
                                FormaPagamento formaPagamento) {
        this.parcela = parcela;
        this.dataRecebimento = dataRecebimento;
        this.valor = valor;
        this.formaPagamento = formaPagamento;
    }

    public void cancelar(String motivo, String usuario, LocalDateTime quando) {
        if (cancelado) {
            throw new IllegalStateException("Recebimento já cancelado.");
        }
        this.cancelado = true;
        this.motivoCancelamento = motivo;
        this.canceladoPor = usuario;
        this.canceladoEm = quando;
    }

    public ParcelaHonorario getParcela() {
        return parcela;
    }

    public LocalDate getDataRecebimento() {
        return dataRecebimento;
    }

    public BigDecimal getValor() {
        return valor;
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

    public boolean isCancelado() {
        return cancelado;
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
