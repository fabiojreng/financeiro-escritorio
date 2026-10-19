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
 * Acordo que reduz ou parcela o honorário de um pagamento agendado. O valor original do honorário
 * fica congelado aqui; o que o cliente vai pagar é o valor negociado.
 */
@Entity
@Table(name = "negociacao_honorario")
public class NegociacaoHonorario extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pagamento_agendado_id", nullable = false, updatable = false)
    private PagamentoAgendado pagamento;

    @Column(name = "valor_original", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorOriginal;

    @Column(name = "valor_negociado", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorNegociado;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal desconto;

    @Column(nullable = false, length = 2000)
    private String motivo;

    @Column(name = "data_negociacao", nullable = false)
    private LocalDate dataNegociacao;

    @Column(name = "registrado_por", nullable = false, length = 100)
    private String registradoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoNegociacao situacao = SituacaoNegociacao.ATIVA;

    protected NegociacaoHonorario() {
    }

    public NegociacaoHonorario(PagamentoAgendado pagamento, BigDecimal valorOriginal, BigDecimal valorNegociado,
                               String motivo, LocalDate dataNegociacao, String registradoPor) {
        this.pagamento = pagamento;
        this.valorOriginal = valorOriginal;
        this.valorNegociado = valorNegociado;
        this.desconto = valorOriginal.subtract(valorNegociado);
        this.motivo = motivo;
        this.dataNegociacao = dataNegociacao;
        this.registradoPor = registradoPor;
    }

    public void substituir() {
        this.situacao = SituacaoNegociacao.SUBSTITUIDA;
    }

    public boolean isAtiva() {
        return situacao == SituacaoNegociacao.ATIVA;
    }

    public PagamentoAgendado getPagamento() {
        return pagamento;
    }

    public BigDecimal getValorOriginal() {
        return valorOriginal;
    }

    public BigDecimal getValorNegociado() {
        return valorNegociado;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDate getDataNegociacao() {
        return dataNegociacao;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public SituacaoNegociacao getSituacao() {
        return situacao;
    }
}
