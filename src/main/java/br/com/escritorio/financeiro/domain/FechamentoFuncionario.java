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
 * Acerto mensal pago a um funcionário. Enquanto está pago, a competência do funcionário fica fechada:
 * lançamentos não podem ser feitos, alterados nem cancelados. Para corrigir, quem tem a permissão de reabrir
 * reabre o acerto informando o motivo.
 */
@Entity
@Table(name = "fechamento_funcionario")
public class FechamentoFuncionario extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "funcionario_id", nullable = false, updatable = false)
    private Funcionario funcionario;

    /** Primeiro dia do mês. */
    @Column(nullable = false, updatable = false)
    private LocalDate competencia;

    @Column(nullable = false)
    private boolean pago;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 20)
    private FormaPagamento formaPagamento;

    @Column(name = "pago_em")
    private LocalDateTime pagoEm;

    @Column(name = "pago_por", length = 100)
    private String pagoPor;

    @Column(name = "salario_base", precision = 15, scale = 2)
    private BigDecimal salarioBase;

    @Column(name = "total_bonificacoes", precision = 15, scale = 2)
    private BigDecimal totalBonificacoes;

    @Column(name = "total_reembolsos", precision = 15, scale = 2)
    private BigDecimal totalReembolsos;

    @Column(name = "total_vales", precision = 15, scale = 2)
    private BigDecimal totalVales;

    @Column(name = "total_descontos", precision = 15, scale = 2)
    private BigDecimal totalDescontos;

    @Column(precision = 15, scale = 2)
    private BigDecimal liquido;

    @Column(name = "reaberto_em")
    private LocalDateTime reabertoEm;

    @Column(name = "reaberto_por", length = 100)
    private String reabertoPor;

    @Column(name = "motivo_reabertura", length = 500)
    private String motivoReabertura;

    protected FechamentoFuncionario() {
    }

    public FechamentoFuncionario(Funcionario funcionario, YearMonth competencia) {
        this.funcionario = funcionario;
        this.competencia = competencia.atDay(1);
    }

    /** Registra o pagamento guardando o acerto como estava naquele momento. */
    public void marcarPago(AcertoMensal acerto, LocalDate dataPagamento, FormaPagamento formaPagamento,
                           String usuario, LocalDateTime quando) {
        if (pago) {
            throw new IllegalStateException("O acerto já está pago.");
        }
        this.pago = true;
        this.dataPagamento = dataPagamento;
        this.formaPagamento = formaPagamento;
        this.pagoPor = usuario;
        this.pagoEm = quando;
        this.salarioBase = acerto.salarioBase();
        this.totalBonificacoes = acerto.bonificacoes();
        this.totalReembolsos = acerto.reembolsos();
        this.totalVales = acerto.vales();
        this.totalDescontos = acerto.descontos();
        this.liquido = acerto.liquido();
    }

    public void reabrir(String motivo, String usuario, LocalDateTime quando) {
        if (!pago) {
            throw new IllegalStateException("O acerto não está pago.");
        }
        this.pago = false;
        this.motivoReabertura = motivo;
        this.reabertoPor = usuario;
        this.reabertoEm = quando;
    }

    /** Retrato do acerto gravado no pagamento. */
    public AcertoMensal getAcerto() {
        return new AcertoMensal(salarioBase, totalBonificacoes, totalReembolsos, totalVales, totalDescontos,
                liquido);
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public YearMonth getCompetencia() {
        return YearMonth.from(competencia);
    }

    public boolean isPago() {
        return pago;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public LocalDateTime getPagoEm() {
        return pagoEm;
    }

    public String getPagoPor() {
        return pagoPor;
    }

    public BigDecimal getLiquido() {
        return liquido;
    }

    public LocalDateTime getReabertoEm() {
        return reabertoEm;
    }

    public String getReabertoPor() {
        return reabertoPor;
    }

    public String getMotivoReabertura() {
        return motivoReabertura;
    }
}
