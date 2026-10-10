package br.com.escritorio.financeiro.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Período do painel: o mês inteiro ou um intervalo personalizado. Um lado em branco do intervalo
 * fica aberto ("até" ou "a partir de").
 */
public class FiltroPeriodo {

    public static final LocalDate DATA_MINIMA = LocalDate.of(1900, 1, 1);
    public static final LocalDate DATA_MAXIMA = LocalDate.of(9999, 12, 31);

    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth mes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate inicio;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fim;

    public boolean isPeriodoPersonalizado() {
        return inicio != null || fim != null;
    }

    /** Início informado (com as datas em ordem), ou {@code null} quando o intervalo está aberto. */
    public LocalDate getInicioOrdenado() {
        return inicio != null && fim != null && inicio.isAfter(fim) ? fim : inicio;
    }

    /** Fim informado (com as datas em ordem), ou {@code null} quando o intervalo está aberto. */
    public LocalDate getFimOrdenado() {
        return inicio != null && fim != null && inicio.isAfter(fim) ? inicio : fim;
    }

    public LocalDate getInicioEfetivo() {
        if (!isPeriodoPersonalizado()) {
            return mes.atDay(1);
        }
        LocalDate ordenado = getInicioOrdenado();
        return ordenado == null ? DATA_MINIMA : ordenado;
    }

    public LocalDate getFimEfetivo() {
        if (!isPeriodoPersonalizado()) {
            return mes.atEndOfMonth();
        }
        LocalDate ordenado = getFimOrdenado();
        return ordenado == null ? DATA_MAXIMA : ordenado;
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
}
