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

/**
 * Responsável que cuida (ou captou) o cliente de um pagamento agendado, com o repasse que cabe a ele.
 * Um pagamento pode ter vários responsáveis, cada um com o próprio repasse.
 */
@Entity
@Table(name = "pagamento_responsavel")
public class ResponsavelPagamento extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagamento_agendado_id", nullable = false)
    private PagamentoAgendado pagamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Responsavel responsavel;

    @Column(name = "valor_repasse", precision = 15, scale = 2)
    private BigDecimal valorRepasse;

    /** Quanto o responsável já recebeu; só é guardado na situação "Parcial". */
    @Column(name = "valor_repasse_pago", precision = 15, scale = 2)
    private BigDecimal valorRepassePago;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao_repasse", nullable = false, length = 20)
    private SituacaoRepassePagamento situacaoRepasse = SituacaoRepassePagamento.PENDENTE;

    protected ResponsavelPagamento() {
    }

    ResponsavelPagamento(PagamentoAgendado pagamento, Responsavel responsavel) {
        this.pagamento = pagamento;
        this.responsavel = responsavel;
    }

    public PagamentoAgendado getPagamento() {
        return pagamento;
    }

    public Responsavel getResponsavel() {
        return responsavel;
    }

    public BigDecimal getValorRepasse() {
        return valorRepasse;
    }

    public void setValorRepasse(BigDecimal valorRepasse) {
        this.valorRepasse = valorRepasse;
    }

    public BigDecimal getValorRepassePago() {
        return valorRepassePago;
    }

    public void setValorRepassePago(BigDecimal valorRepassePago) {
        this.valorRepassePago = valorRepassePago;
    }

    /** Quanto do repasse já chegou ao responsável: tudo no "Pago", nada no "Pendente". */
    public BigDecimal getValorPago() {
        if (valorRepasse == null) {
            return BigDecimal.ZERO;
        }
        return switch (situacaoRepasse) {
            case PAGO -> valorRepasse;
            case PARCIAL -> valorRepassePago == null ? BigDecimal.ZERO : valorRepassePago;
            case PENDENTE -> BigDecimal.ZERO;
        };
    }

    /** Quanto do repasse ainda falta pagar ao responsável. */
    public BigDecimal getSaldo() {
        return valorRepasse == null ? BigDecimal.ZERO : valorRepasse.subtract(getValorPago());
    }

    /** Se o responsável já recebeu alguma parte do repasse. */
    public boolean isComPagamento() {
        return situacaoRepasse != SituacaoRepassePagamento.PENDENTE;
    }

    public SituacaoRepassePagamento getSituacaoRepasse() {
        return situacaoRepasse;
    }

    public void setSituacaoRepasse(SituacaoRepassePagamento situacaoRepasse) {
        this.situacaoRepasse = situacaoRepasse;
    }
}
