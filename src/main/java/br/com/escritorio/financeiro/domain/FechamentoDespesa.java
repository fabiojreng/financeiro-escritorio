package br.com.escritorio.financeiro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Envio da planilha de despesas de um mês à contabilidade. Enquanto o mês está enviado, as
 * despesas dele não podem ser lançadas, alteradas nem canceladas; para corrigir, um administrador
 * reabre o mês informando o motivo.
 */
@Entity
@Table(name = "fechamento_despesa")
public class FechamentoDespesa extends EntidadeBase {

    /** Primeiro dia do mês. */
    @Column(nullable = false, updatable = false)
    private LocalDate competencia;

    @Column(nullable = false)
    private boolean enviado;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "enviado_por", length = 100)
    private String enviadoPor;

    private Integer quantidade;

    @Column(precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "reaberto_em")
    private LocalDateTime reabertoEm;

    @Column(name = "reaberto_por", length = 100)
    private String reabertoPor;

    @Column(name = "motivo_reabertura", length = 500)
    private String motivoReabertura;

    protected FechamentoDespesa() {
    }

    public FechamentoDespesa(YearMonth competencia) {
        this.competencia = competencia.atDay(1);
    }

    /** Registra o envio com a quantidade e o total que estavam na planilha naquele momento. */
    public void marcarEnviado(String usuario, LocalDateTime quando, int quantidade, BigDecimal total) {
        if (enviado) {
            throw new IllegalStateException("O mês já está marcado como enviado.");
        }
        this.enviado = true;
        this.enviadoPor = usuario;
        this.enviadoEm = quando;
        this.quantidade = quantidade;
        this.total = total;
    }

    public void reabrir(String motivo, String usuario, LocalDateTime quando) {
        if (!enviado) {
            throw new IllegalStateException("O mês não está marcado como enviado.");
        }
        this.enviado = false;
        this.motivoReabertura = motivo;
        this.reabertoPor = usuario;
        this.reabertoEm = quando;
    }

    public YearMonth getCompetencia() {
        return YearMonth.from(competencia);
    }

    public boolean isEnviado() {
        return enviado;
    }

    public LocalDateTime getEnviadoEm() {
        return enviadoEm;
    }

    public String getEnviadoPor() {
        return enviadoPor;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public BigDecimal getTotal() {
        return total;
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
