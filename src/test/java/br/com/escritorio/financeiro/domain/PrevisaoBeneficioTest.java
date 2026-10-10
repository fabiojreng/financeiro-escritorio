package br.com.escritorio.financeiro.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrevisaoBeneficioTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 10);

    @Test
    void dataVencidaSemConfirmacaoEAtrasadaMasContinuaPrevista() {
        PrevisaoBeneficio previsao = previsaoPara(HOJE.minusDays(1));

        assertThat(previsao.destaque(HOJE)).isEqualTo(DestaquePrevisao.ATRASADA);
        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.PREVISTO);
    }

    @Test
    void destacaHojeEProximosSeteDias() {
        assertThat(previsaoPara(HOJE).destaque(HOJE)).isEqualTo(DestaquePrevisao.HOJE);
        assertThat(previsaoPara(HOJE.plusDays(7)).destaque(HOJE)).isEqualTo(DestaquePrevisao.PROXIMA);
        assertThat(previsaoPara(HOJE.plusDays(8)).destaque(HOJE)).isNull();
    }

    @Test
    void liberadaAdiadaOuCanceladaNaoFicaAtrasada() {
        PrevisaoBeneficio liberada = previsaoPara(HOJE.minusDays(5));
        liberada.confirmarLiberacao(HOJE.minusDays(4), new BigDecimal("1000.00"));
        PrevisaoBeneficio adiada = previsaoPara(HOJE.minusDays(5));
        adiada.alterarSituacao(SituacaoPrevisao.ADIADO);

        assertThat(liberada.destaque(HOJE)).isNull();
        assertThat(adiada.destaque(HOJE)).isNull();
    }

    @Test
    void aguardandoConfirmacaoTambemFicaAtrasada() {
        PrevisaoBeneficio previsao = previsaoPara(HOJE.minusDays(2));
        previsao.alterarSituacao(SituacaoPrevisao.AGUARDANDO_CONFIRMACAO);

        assertThat(previsao.destaque(HOJE)).isEqualTo(DestaquePrevisao.ATRASADA);
    }

    @Test
    void liberacaoSoPorConfirmacao() {
        PrevisaoBeneficio previsao = previsaoPara(HOJE);

        assertThatThrownBy(() -> previsao.alterarSituacao(SituacaoPrevisao.LIBERADO))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void desfazerLiberacaoLimpaDadosEfetivos() {
        PrevisaoBeneficio previsao = previsaoPara(HOJE);
        previsao.confirmarLiberacao(HOJE, new BigDecimal("900.00"));

        previsao.desfazerLiberacao();

        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.AGUARDANDO_CONFIRMACAO);
        assertThat(previsao.getDataEfetiva()).isNull();
        assertThat(previsao.getValorConfirmado()).isNull();
        assertThat(previsao.getValorEstimado()).isEqualByComparingTo("1500.00");
    }

    private static PrevisaoBeneficio previsaoPara(LocalDate data) {
        return new PrevisaoBeneficio(null, data, new BigDecimal("1500.00"), TipoPagamento.PRIMEIRO_PAGAMENTO);
    }
}
