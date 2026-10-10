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

/**
 * Previsão de liberação de benefício do INSS. Cliente, responsável e tipo de serviço são lidos
 * do atendimento. A situação nunca muda sozinha com a passagem da data: a liberação só acontece
 * por {@link #confirmarLiberacao}.
 */
@Entity
@Table(name = "previsao_beneficio")
public class PrevisaoBeneficio extends EntidadeBase {

    public static final int DIAS_PROXIMIDADE = 7;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimento_id", nullable = false, updatable = false)
    private Atendimento atendimento;

    @Column(name = "data_prevista", nullable = false)
    private LocalDate dataPrevista;

    @Column(name = "valor_estimado", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorEstimado;

    @Column(name = "valor_confirmado", precision = 15, scale = 2)
    private BigDecimal valorConfirmado;

    @Column(name = "data_efetiva")
    private LocalDate dataEfetiva;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagamento", nullable = false, length = 30)
    private TipoPagamento tipoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SituacaoPrevisao situacao = SituacaoPrevisao.PREVISTO;

    @Column(length = 2000)
    private String observacoes;

    protected PrevisaoBeneficio() {
    }

    public PrevisaoBeneficio(Atendimento atendimento, LocalDate dataPrevista, BigDecimal valorEstimado,
                             TipoPagamento tipoPagamento) {
        this.atendimento = atendimento;
        this.dataPrevista = dataPrevista;
        this.valorEstimado = valorEstimado;
        this.tipoPagamento = tipoPagamento;
    }

    public void alterarSituacao(SituacaoPrevisao nova) {
        if (nova == SituacaoPrevisao.LIBERADO || situacao == SituacaoPrevisao.LIBERADO) {
            throw new IllegalStateException("A liberação só muda por confirmação ou desfazimento explícito.");
        }
        this.situacao = nova;
    }

    public void confirmarLiberacao(LocalDate dataEfetiva, BigDecimal valorConfirmado) {
        if (situacao == SituacaoPrevisao.CANCELADO) {
            throw new IllegalStateException("Previsão cancelada não pode ser liberada.");
        }
        this.dataEfetiva = dataEfetiva;
        this.valorConfirmado = valorConfirmado;
        this.situacao = SituacaoPrevisao.LIBERADO;
    }

    public void desfazerLiberacao() {
        if (situacao != SituacaoPrevisao.LIBERADO) {
            throw new IllegalStateException("A previsão não está liberada.");
        }
        this.dataEfetiva = null;
        this.valorConfirmado = null;
        this.situacao = SituacaoPrevisao.AGUARDANDO_CONFIRMACAO;
    }

    public boolean isLiberada() {
        return situacao == SituacaoPrevisao.LIBERADO;
    }

    /** Atrasada, prevista para hoje ou para os próximos dias; {@code null} quando não merece destaque. */
    public DestaquePrevisao destaque(LocalDate hoje) {
        if (!situacao.isPendente()) {
            return null;
        }
        if (dataPrevista.isBefore(hoje)) {
            return DestaquePrevisao.ATRASADA;
        }
        if (dataPrevista.isEqual(hoje)) {
            return DestaquePrevisao.HOJE;
        }
        if (!dataPrevista.isAfter(hoje.plusDays(DIAS_PROXIMIDADE))) {
            return DestaquePrevisao.PROXIMA;
        }
        return null;
    }

    public Atendimento getAtendimento() {
        return atendimento;
    }

    public LocalDate getDataPrevista() {
        return dataPrevista;
    }

    public void setDataPrevista(LocalDate dataPrevista) {
        this.dataPrevista = dataPrevista;
    }

    public BigDecimal getValorEstimado() {
        return valorEstimado;
    }

    public void setValorEstimado(BigDecimal valorEstimado) {
        this.valorEstimado = valorEstimado;
    }

    public BigDecimal getValorConfirmado() {
        return valorConfirmado;
    }

    public LocalDate getDataEfetiva() {
        return dataEfetiva;
    }

    public TipoPagamento getTipoPagamento() {
        return tipoPagamento;
    }

    public void setTipoPagamento(TipoPagamento tipoPagamento) {
        this.tipoPagamento = tipoPagamento;
    }

    public SituacaoPrevisao getSituacao() {
        return situacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
