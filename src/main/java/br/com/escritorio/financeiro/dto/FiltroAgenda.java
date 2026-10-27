package br.com.escritorio.financeiro.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/**
 * Período das agendas (pagamentos e perícias). Dia, semana (segunda a domingo) e mês valem o que contém a data
 * escolhida. No período mês, a tela escolhe o mês direto ({@link #mes}) e a data passa a ser o dia 1 dele.
 * No período personalizado, um lado em branco fica aberto e, com os dois em branco, aparecem todas as datas,
 * como em "Todo o período".
 */
public abstract class FiltroAgenda {

    private PeriodoPagamentos periodo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @DateTimeFormat(pattern = "yyyy-MM")
    private YearMonth mes;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate de;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ate;

    public LocalDate getInicio() {
        return switch (periodo) {
            case DIA -> data;
            case SEMANA -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> data.withDayOfMonth(1);
            case PERSONALIZADO -> Objects.requireNonNullElse(getDeOrdenado(), FiltroPeriodo.DATA_MINIMA);
            case TODOS -> FiltroPeriodo.DATA_MINIMA;
        };
    }

    public LocalDate getFim() {
        return switch (periodo) {
            case DIA -> data;
            case SEMANA -> getInicio().plusDays(6);
            case MES -> data.with(TemporalAdjusters.lastDayOfMonth());
            case PERSONALIZADO -> Objects.requireNonNullElse(getAteOrdenado(), FiltroPeriodo.DATA_MAXIMA);
            case TODOS -> FiltroPeriodo.DATA_MAXIMA;
        };
    }

    /** Data de referência do período que fica {@code quantidade} períodos à frente (ou atrás, se negativa). */
    public LocalDate deslocar(int quantidade) {
        return switch (periodo) {
            case DIA -> data.plusDays(quantidade);
            case SEMANA -> getInicio().plusWeeks(quantidade);
            case MES -> getInicio().plusMonths(quantidade);
            case PERSONALIZADO, TODOS ->
                    throw new IllegalStateException("O período " + periodo.getRotulo() + " não tem anterior nem seguinte.");
        };
    }

    public boolean isTodasAsDatas() {
        return periodo == PeriodoPagamentos.TODOS
                || periodo == PeriodoPagamentos.PERSONALIZADO && de == null && ate == null;
    }

    /** "De" informado (com as datas em ordem), ou {@code null} quando o intervalo está aberto. */
    public LocalDate getDeOrdenado() {
        return de != null && ate != null && de.isAfter(ate) ? ate : de;
    }

    /** "Até" informado (com as datas em ordem), ou {@code null} quando o intervalo está aberto. */
    public LocalDate getAteOrdenado() {
        return de != null && ate != null && de.isAfter(ate) ? de : ate;
    }

    /** Parte do nome do arquivo exportado, como "2026-10-12", "2026-10" ou "2026-10-01_a_2026-10-31". */
    public String getSufixoArquivo() {
        return switch (periodo) {
            case DIA, SEMANA -> getInicio().toString();
            case MES -> YearMonth.from(data).toString();
            case PERSONALIZADO -> texto(getDeOrdenado(), "inicio") + "_a_" + texto(getAteOrdenado(), "fim");
            case TODOS -> "todas-as-datas";
        };
    }

    private static String texto(LocalDate data, String quandoAberto) {
        return data == null ? quandoAberto : data.toString();
    }

    public PeriodoPagamentos getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoPagamentos periodo) {
        this.periodo = periodo;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public YearMonth getMes() {
        return mes;
    }

    public void setMes(YearMonth mes) {
        this.mes = mes;
    }

    public LocalDate getDe() {
        return de;
    }

    public void setDe(LocalDate de) {
        this.de = de;
    }

    public LocalDate getAte() {
        return ate;
    }

    public void setAte(LocalDate ate) {
        this.ate = ate;
    }
}
