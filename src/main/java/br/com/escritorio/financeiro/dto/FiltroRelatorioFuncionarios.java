package br.com.escritorio.financeiro.dto;

import java.time.LocalDate;
import java.time.YearMonth;

/** Período (mês ou intervalo) e funcionário dos relatórios de funcionários. */
public class FiltroRelatorioFuncionarios extends FiltroPeriodo {

    private Long funcionarioId;

    /** Primeiro mês do período; com o início em aberto, conta {@code mesesQuandoAberto} meses antes do fim. */
    public YearMonth getMesInicial(YearMonth hoje, int mesesQuandoAberto) {
        if (!isPeriodoPersonalizado()) {
            return getMes();
        }
        LocalDate inicio = getInicioOrdenado();
        return inicio == null ? getMesFinal(hoje).minusMonths(mesesQuandoAberto - 1L) : YearMonth.from(inicio);
    }

    /** Último mês do período; com o fim em aberto, vai até o mês atual. */
    public YearMonth getMesFinal(YearMonth hoje) {
        if (!isPeriodoPersonalizado()) {
            return getMes();
        }
        LocalDate fim = getFimOrdenado();
        return fim == null ? hoje : YearMonth.from(fim);
    }

    /** Parte do nome do arquivo exportado, como "2026-10" ou "2026-01_a_2026-10". */
    public String sufixoArquivo(YearMonth inicio, YearMonth fim) {
        return inicio.equals(fim) ? inicio.toString() : inicio + "_a_" + fim;
    }

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(Long funcionarioId) {
        this.funcionarioId = funcionarioId;
    }
}
