package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.time.YearMonth;

/** Benefício já concedido a um cliente, lançado no mês da concessão. */
@Entity
@Table(name = "beneficio_concedido")
public class BeneficioConcedido extends EntidadeBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_servico_id", nullable = false)
    private TipoServico tipoServico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Responsavel responsavel;

    /** Primeiro dia do mês em que o benefício foi concedido. */
    @Column(nullable = false)
    private LocalDate competencia;

    /** Preenchido na listagem: já existe pagamento deste cliente para este mesmo benefício. */
    @Transient
    private boolean comPagamento;

    protected BeneficioConcedido() {
    }

    public BeneficioConcedido(Cliente cliente, TipoServico tipoServico, Responsavel responsavel, YearMonth competencia) {
        this.cliente = cliente;
        this.tipoServico = tipoServico;
        this.responsavel = responsavel;
        this.competencia = competencia.atDay(1);
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public TipoServico getTipoServico() {
        return tipoServico;
    }

    public void setTipoServico(TipoServico tipoServico) {
        this.tipoServico = tipoServico;
    }

    public Responsavel getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(Responsavel responsavel) {
        this.responsavel = responsavel;
    }

    public LocalDate getCompetencia() {
        return competencia;
    }

    public YearMonth getMes() {
        return YearMonth.from(competencia);
    }

    public void setMes(YearMonth mes) {
        this.competencia = mes.atDay(1);
    }

    public boolean isComPagamento() {
        return comPagamento;
    }

    public void setComPagamento(boolean comPagamento) {
        this.comPagamento = comPagamento;
    }
}
