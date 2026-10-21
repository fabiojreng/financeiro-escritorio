package br.com.escritorio.financeiro.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FiltroPagamentosTest {

    private static final LocalDate QUINTA = LocalDate.of(2026, 10, 15);

    @Test
    void diaValeSoAData() {
        FiltroPagamentos filtro = filtro(PeriodoPagamentos.DIA);

        assertThat(filtro.getInicio()).isEqualTo(QUINTA);
        assertThat(filtro.getFim()).isEqualTo(QUINTA);
        assertThat(filtro.deslocar(-1)).isEqualTo(LocalDate.of(2026, 10, 14));
        assertThat(filtro.getSufixoArquivo()).isEqualTo("2026-10-15");
    }

    @Test
    void semanaVaiDeSegundaADomingo() {
        FiltroPagamentos filtro = filtro(PeriodoPagamentos.SEMANA);

        assertThat(filtro.getInicio()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(filtro.getFim()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(filtro.deslocar(1)).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(filtro.getSufixoArquivo()).isEqualTo("2026-10-12");
    }

    @Test
    void mesValeDoPrimeiroAoUltimoDia() {
        FiltroPagamentos filtro = filtro(PeriodoPagamentos.MES);

        assertThat(filtro.getInicio()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(filtro.getFim()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(filtro.deslocar(-1)).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(filtro.getSufixoArquivo()).isEqualTo("2026-10");
    }

    @Test
    void personalizadoColocaAsDatasEmOrdem() {
        FiltroPagamentos filtro = filtro(PeriodoPagamentos.PERSONALIZADO);
        filtro.setDe(LocalDate.of(2026, 11, 20));
        filtro.setAte(LocalDate.of(2026, 10, 5));

        assertThat(filtro.getInicio()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(filtro.getFim()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(filtro.getSufixoArquivo()).isEqualTo("2026-10-05_a_2026-11-20");
    }

    @Test
    void personalizadoComLadoEmBrancoFicaAberto() {
        FiltroPagamentos filtro = filtro(PeriodoPagamentos.PERSONALIZADO);
        filtro.setDe(LocalDate.of(2026, 10, 5));

        assertThat(filtro.getInicio()).isEqualTo(LocalDate.of(2026, 10, 5));
        assertThat(filtro.getFim()).isEqualTo(FiltroPeriodo.DATA_MAXIMA);
        assertThat(filtro.isTodasAsDatas()).isFalse();

        filtro.setDe(null);
        assertThat(filtro.getInicio()).isEqualTo(FiltroPeriodo.DATA_MINIMA);
        assertThat(filtro.isTodasAsDatas()).isTrue();
    }

    @Test
    void todoOPeriodoNaoLimitaAsDatasNemNavega() {
        FiltroPagamentos filtro = filtro(PeriodoPagamentos.TODOS);

        assertThat(filtro.getInicio()).isEqualTo(FiltroPeriodo.DATA_MINIMA);
        assertThat(filtro.getFim()).isEqualTo(FiltroPeriodo.DATA_MAXIMA);
        assertThat(filtro.isTodasAsDatas()).isTrue();
        assertThat(filtro.getSufixoArquivo()).isEqualTo("todas-as-datas");
        assertThat(PeriodoPagamentos.TODOS.isNavegavel()).isFalse();
        assertThatThrownBy(() -> filtro.deslocar(1)).isInstanceOf(IllegalStateException.class);
    }

    private static FiltroPagamentos filtro(PeriodoPagamentos periodo) {
        FiltroPagamentos filtro = new FiltroPagamentos();
        filtro.setPeriodo(periodo);
        filtro.setData(QUINTA);
        return filtro;
    }
}
