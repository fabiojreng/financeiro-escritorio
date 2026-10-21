package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.dto.FiltroPagamentos;
import br.com.escritorio.financeiro.dto.PeriodoPagamentos;

import java.time.LocalDate;
import java.time.YearMonth;

final class FiltrosDaAgenda {

    private FiltrosDaAgenda() {
    }

    static void completar(FiltroPagamentos filtro, LocalDate hoje) {
        completar(filtro, hoje, PeriodoPagamentos.SEMANA);
    }

    static void completar(FiltroPagamentos filtro, LocalDate hoje, PeriodoPagamentos padrao) {
        if (filtro.getPeriodo() == null) {
            filtro.setPeriodo(padrao);
        }
        if (filtro.getPeriodo() == PeriodoPagamentos.MES && filtro.getMes() != null) {
            filtro.setData(filtro.getMes().atDay(1));
        }
        if (filtro.getData() == null) {
            filtro.setData(hoje);
        }
        filtro.setMes(YearMonth.from(filtro.getData()));
    }

    static String tituloPeriodo(FiltroPagamentos filtro) {
        return switch (filtro.getPeriodo()) {
            case DIA -> TituloPeriodo.dia(filtro.getData());
            case SEMANA -> TituloPeriodo.semana(filtro.getInicio(), filtro.getFim());
            case MES -> TituloPeriodo.mes(YearMonth.from(filtro.getData()));
            case PERSONALIZADO -> filtro.isTodasAsDatas()
                    ? "Todas as datas"
                    : TituloPeriodo.periodo(filtro.getDeOrdenado(), filtro.getAteOrdenado());
            case TODOS -> "Todas as datas";
        };
    }
}
