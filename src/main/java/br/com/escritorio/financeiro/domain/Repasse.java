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
 * Comissão ou repasse interno lançado à mão. Os valores são os informados pelo usuário: não há
 * percentual padrão nem cálculo sobre o benefício do INSS ou sobre os honorários. Uma regra
 * automática futura deve entrar como configuração explícita (regra, base e percentual ou valor).
 */
@Entity
@Table(name = "repasse")
public class Repasse extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimento_id", nullable = false, updatable = false)
    private Atendimento atendimento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Responsavel destinatario;

    @Column(nullable = false, length = 500)
    private String descricao;

    @Column(name = "valor_previsto", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPrevisto;

    @Column(name = "data_prevista", nullable = false)
    private LocalDate dataPrevista;

    @Column(name = "valor_pago", precision = 15, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoRepasse situacao = SituacaoRepasse.PENDENTE;

    @Column(length = 2000)
    private String observacoes;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "cancelado_por", length = 100)
    private String canceladoPor;

    protected Repasse() {
    }

    public Repasse(Atendimento atendimento, Responsavel destinatario, String descricao, BigDecimal valorPrevisto,
                   LocalDate dataPrevista) {
        this.atendimento = atendimento;
        this.destinatario = destinatario;
        this.descricao = descricao;
        this.valorPrevisto = valorPrevisto;
        this.dataPrevista = dataPrevista;
    }

    public void registrarPagamento(LocalDate dataPagamento, BigDecimal valorPago) {
        if (situacao != SituacaoRepasse.PENDENTE) {
            throw new IllegalStateException("Só repasse pendente pode receber pagamento.");
        }
        this.dataPagamento = dataPagamento;
        this.valorPago = valorPago;
        this.situacao = SituacaoRepasse.PAGO;
    }

    /** Cancela mantendo o registro; um repasse pago continua mostrando o que foi pago. */
    public void cancelar(String motivo, String usuario, LocalDateTime quando) {
        if (situacao == SituacaoRepasse.CANCELADO) {
            throw new IllegalStateException("O repasse já está cancelado.");
        }
        this.situacao = SituacaoRepasse.CANCELADO;
        this.motivoCancelamento = motivo;
        this.canceladoPor = usuario;
        this.canceladoEm = quando;
    }

    public boolean isAtrasado(LocalDate hoje) {
        return isPendente() && dataPrevista.isBefore(hoje);
    }

    public boolean isPendente() {
        return situacao == SituacaoRepasse.PENDENTE;
    }

    public boolean isPago() {
        return situacao == SituacaoRepasse.PAGO;
    }

    public boolean isCancelado() {
        return situacao == SituacaoRepasse.CANCELADO;
    }

    public Atendimento getAtendimento() {
        return atendimento;
    }

    public Responsavel getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(Responsavel destinatario) {
        this.destinatario = destinatario;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValorPrevisto() {
        return valorPrevisto;
    }

    public void setValorPrevisto(BigDecimal valorPrevisto) {
        this.valorPrevisto = valorPrevisto;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public void setDataPrevista(LocalDate dataPrevista) {
        this.dataPrevista = dataPrevista;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public SituacaoRepasse getSituacao() {
        return situacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
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
