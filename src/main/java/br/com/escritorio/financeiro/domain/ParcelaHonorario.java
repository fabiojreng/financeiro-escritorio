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
 * Parcela do acordo de honorários. Fica paga quando os recebimentos ligados a ela cobrem o valor;
 * cancelada exige motivo e continua registrada.
 */
@Entity
@Table(name = "parcela_honorario")
public class ParcelaHonorario extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acordo_id", nullable = false, updatable = false)
    private AcordoHonorarios acordo;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoParcela situacao = SituacaoParcela.ABERTA;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "cancelada_em")
    private LocalDateTime canceladaEm;

    @Column(name = "cancelada_por", length = 100)
    private String canceladaPor;

    protected ParcelaHonorario() {
    }

    public ParcelaHonorario(AcordoHonorarios acordo, int numero, LocalDate vencimento, BigDecimal valor) {
        this.acordo = acordo;
        this.numero = numero;
        this.vencimento = vencimento;
        this.valor = valor;
    }

    /** Atualiza a situação a partir do total recebido nesta parcela; parcela cancelada não muda. */
    public void atualizarPagamento(BigDecimal totalRecebido) {
        if (situacao == SituacaoParcela.CANCELADA) {
            return;
        }
        situacao = totalRecebido.compareTo(valor) >= 0 ? SituacaoParcela.PAGA : SituacaoParcela.ABERTA;
    }

    public void cancelar(String motivo, String usuario, LocalDateTime quando) {
        if (situacao != SituacaoParcela.ABERTA) {
            throw new IllegalStateException("Só parcela aberta pode ser cancelada.");
        }
        this.situacao = SituacaoParcela.CANCELADA;
        this.motivoCancelamento = motivo;
        this.canceladaPor = usuario;
        this.canceladaEm = quando;
    }

    public boolean isVencida(LocalDate hoje) {
        return situacao == SituacaoParcela.ABERTA && vencimento.isBefore(hoje);
    }

    public boolean isAtiva() {
        return situacao != SituacaoParcela.CANCELADA;
    }

    public AcordoHonorarios getAcordo() {
        return acordo;
    }

    public int getNumero() {
        return numero;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public void setVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public SituacaoParcela getSituacao() {
        return situacao;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public LocalDateTime getCanceladaEm() {
        return canceladaEm;
    }

    public String getCanceladaPor() {
        return canceladaPor;
    }
}
