package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Acordo de honorários do atendimento: o que o cliente paga ao escritório. É independente do
 * benefício do INSS; mesmo na cobrança percentual, o valor contratado é informado pelo usuário.
 */
@Entity
@Table(name = "acordo_honorarios")
public class AcordoHonorarios extends EntidadeBase {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atendimento_id", nullable = false, updatable = false, unique = true)
    private Atendimento atendimento;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_cobranca", nullable = false, length = 20)
    private FormaCobranca formaCobranca;

    @Column(name = "valor_contratado", precision = 15, scale = 2)
    private BigDecimal valorContratado;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentual;

    @Column(name = "base_calculo", length = 500)
    private String baseCalculo;

    @Column(name = "quantidade_parcelas")
    private Integer quantidadeParcelas;

    @Column(length = 2000)
    private String condicoes;

    @Column(name = "data_acordo", nullable = false)
    private LocalDate dataAcordo;

    protected AcordoHonorarios() {
    }

    public AcordoHonorarios(Atendimento atendimento, FormaCobranca formaCobranca, LocalDate dataAcordo) {
        this.atendimento = atendimento;
        this.formaCobranca = formaCobranca;
        this.dataAcordo = dataAcordo;
    }

    /** Percentual e base de cálculo só existem na cobrança percentual. */
    public void definirCobranca(FormaCobranca forma, BigDecimal percentual, String baseCalculo) {
        this.formaCobranca = forma;
        boolean percentualAplicavel = forma == FormaCobranca.PERCENTUAL;
        this.percentual = percentualAplicavel ? percentual : null;
        this.baseCalculo = percentualAplicavel ? baseCalculo : null;
    }

    public Atendimento getAtendimento() {
        return atendimento;
    }

    public FormaCobranca getFormaCobranca() {
        return formaCobranca;
    }

    public BigDecimal getValorContratado() {
        return valorContratado;
    }

    public void setValorContratado(BigDecimal valorContratado) {
        this.valorContratado = valorContratado;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public String getBaseCalculo() {
        return baseCalculo;
    }

    public Integer getQuantidadeParcelas() {
        return quantidadeParcelas;
    }

    public void setQuantidadeParcelas(Integer quantidadeParcelas) {
        this.quantidadeParcelas = quantidadeParcelas;
    }

    public String getCondicoes() {
        return condicoes;
    }

    public void setCondicoes(String condicoes) {
        this.condicoes = condicoes;
    }

    public LocalDate getDataAcordo() {
        return dataAcordo;
    }

    public void setDataAcordo(LocalDate dataAcordo) {
        this.dataAcordo = dataAcordo;
    }
}
