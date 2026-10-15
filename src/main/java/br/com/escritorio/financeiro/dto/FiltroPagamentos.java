package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.StatusPagamento;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Filtros da agenda de pagamentos. A semana vai de segunda a domingo e vale a que contém o dia escolhido. */
public class FiltroPagamentos {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate semana;

    private String busca;
    private Long responsavelId;
    private Long tipoServicoId;
    private StatusPagamento status;

    public LocalDate getInicio() {
        return semana.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public LocalDate getFim() {
        return getInicio().plusDays(6);
    }

    public LocalDate getSemana() {
        return semana;
    }

    public void setSemana(LocalDate semana) {
        this.semana = semana;
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

    public StatusPagamento getStatus() {
        return status;
    }

    public void setStatus(StatusPagamento status) {
        this.status = status;
    }
}
