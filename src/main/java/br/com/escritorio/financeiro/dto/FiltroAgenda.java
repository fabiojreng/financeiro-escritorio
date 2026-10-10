package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Filtros da agenda mensal. Sem período personalizado, vale o mês inteiro; com "somente atrasadas",
 * o período é ignorado para trazer também pendências de meses anteriores.
 */
public class FiltroAgenda {

    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth mes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate inicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fim;

    private String busca;
    private Long responsavelId;
    private Long tipoServicoId;
    private SituacaoPrevisao situacao;
    private boolean atrasadas;

    public boolean isPeriodoPersonalizado() {
        return inicio != null || fim != null;
    }

    public LocalDate getInicioEfetivo() {
        if (!isPeriodoPersonalizado()) {
            return mes.atDay(1);
        }
        return fim != null && inicio != null && inicio.isAfter(fim) ? fim : inicio;
    }

    public LocalDate getFimEfetivo() {
        if (!isPeriodoPersonalizado()) {
            return mes.atEndOfMonth();
        }
        return fim != null && inicio != null && inicio.isAfter(fim) ? inicio : fim;
    }

    public YearMonth getMes() {
        return mes;
    }

    public void setMes(YearMonth mes) {
        this.mes = mes;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate inicio) {
        this.inicio = inicio;
    }

    public LocalDate getFim() {
        return fim;
    }

    public void setFim(LocalDate fim) {
        this.fim = fim;
    }

    public String getBusca() {
        return busca;
    }

    public void setBusca(String busca) {
        this.busca = busca;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    public Long getTipoServicoId() {
        return tipoServicoId;
    }

    public void setTipoServicoId(Long tipoServicoId) {
        this.tipoServicoId = tipoServicoId;
    }

    public SituacaoPrevisao getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoPrevisao situacao) {
        this.situacao = situacao;
    }

    public boolean isAtrasadas() {
        return atrasadas;
    }

    public void setAtrasadas(boolean atrasadas) {
        this.atrasadas = atrasadas;
    }
}
