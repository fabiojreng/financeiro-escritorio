package br.com.escritorio.financeiro.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroPeriodoTest {

    @Test
    void semPeriodoPersonalizadoValeOMesInteiro() {
        FiltroPeriodo filtro = new FiltroPeriodo();
        filtro.setMes(YearMonth.of(2026, 2));

        assertThat(filtro.getInicioEfetivo()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(filtro.getFimEfetivo()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void datasInvertidasSaoColocadasEmOrdem() {
        FiltroPeriodo filtro = new FiltroPeriodo();
        filtro.setMes(YearMonth.of(2026, 10));
        filtro.setInicio(LocalDate.of(2026, 10, 31));
        filtro.setFim(LocalDate.of(2026, 10, 1));

        assertThat(filtro.getInicioEfetivo()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(filtro.getFimEfetivo()).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    void ladoEmBrancoDeixaOPeriodoAberto() {
        FiltroPeriodo filtro = new FiltroPeriodo();
        filtro.setMes(YearMonth.of(2026, 10));
        filtro.setInicio(LocalDate.of(2026, 10, 5));

        assertThat(filtro.getInicioEfetivo()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(filtro.getFimEfetivo()).isEqualTo(FiltroPeriodo.DATA_MAXIMA);
    }
}
