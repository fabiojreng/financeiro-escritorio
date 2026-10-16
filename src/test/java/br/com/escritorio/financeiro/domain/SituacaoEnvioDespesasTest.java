package br.com.escritorio.financeiro.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class SituacaoEnvioDespesasTest {

    private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

    @Test
    void prazoEDia20DoMesSeguinte() {
        assertThat(SituacaoEnvioDespesas.prazo(SETEMBRO)).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(SituacaoEnvioDespesas.prazo(YearMonth.of(2026, 12))).isEqualTo(LocalDate.of(2027, 1, 20));
    }

    @Test
    void mesAtualOuFuturoAindaEstaEmAndamento() {
        assertThat(SituacaoEnvioDespesas.de(SETEMBRO, false, LocalDate.of(2026, 9, 30)))
                .isEqualTo(SituacaoEnvioDespesas.EM_ANDAMENTO);
        assertThat(SituacaoEnvioDespesas.de(YearMonth.of(2026, 11), false, LocalDate.of(2026, 9, 30)))
                .isEqualTo(SituacaoEnvioDespesas.EM_ANDAMENTO);
    }

    @Test
    void antesDoDia15AguardaEnvioSemAvisar() {
        SituacaoEnvioDespesas situacao = SituacaoEnvioDespesas.de(SETEMBRO, false, LocalDate.of(2026, 10, 14));

        assertThat(situacao).isEqualTo(SituacaoEnvioDespesas.AGUARDANDO_ENVIO);
        assertThat(situacao.isAvisar()).isFalse();
    }

    @Test
    void doDia15AoDia20OPrazoEstaProximo() {
        assertThat(SituacaoEnvioDespesas.de(SETEMBRO, false, LocalDate.of(2026, 10, 15)))
                .isEqualTo(SituacaoEnvioDespesas.PRAZO_PROXIMO);
        assertThat(SituacaoEnvioDespesas.de(SETEMBRO, false, LocalDate.of(2026, 10, 20)))
                .isEqualTo(SituacaoEnvioDespesas.PRAZO_PROXIMO);
        assertThat(SituacaoEnvioDespesas.PRAZO_PROXIMO.isAvisar()).isTrue();
    }

    @Test
    void depoisDoDia20EstaAtrasado() {
        assertThat(SituacaoEnvioDespesas.de(SETEMBRO, false, LocalDate.of(2026, 10, 21)))
                .isEqualTo(SituacaoEnvioDespesas.ATRASADO);
        assertThat(SituacaoEnvioDespesas.de(SETEMBRO, false, LocalDate.of(2027, 2, 1)))
                .isEqualTo(SituacaoEnvioDespesas.ATRASADO);
        assertThat(SituacaoEnvioDespesas.ATRASADO.isAvisar()).isTrue();
    }

    @Test
    void mesEnviadoNaoAvisaMesmoDepoisDoPrazo() {
        SituacaoEnvioDespesas situacao = SituacaoEnvioDespesas.de(SETEMBRO, true, LocalDate.of(2026, 11, 5));

        assertThat(situacao).isEqualTo(SituacaoEnvioDespesas.ENVIADO);
        assertThat(situacao.isAvisar()).isFalse();
    }
}
