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

    public SituacaoRepassePagamento getSituacaoRepasse() {
        return situacaoRepasse;
    }

    public void setSituacaoRepasse(SituacaoRepassePagamento situacaoRepasse) {
        this.situacaoRepasse = situacaoRepasse;
    }
}
