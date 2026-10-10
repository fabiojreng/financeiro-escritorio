package br.com.escritorio.financeiro.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotaisRepassesTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 10);

    @Test
    void totaisSeparamPrevistoPagoEPendenteSemOsCancelados() {
        Repasse pendente = repasse("300.00");
        Repasse pago = repasse("500.00");
        pago.registrarPagamento(HOJE, new BigDecimal("450.00"));
        Repasse cancelado = repasse("1000.00");
        cancelado.cancelar("Lançado por engano", "admin", LocalDateTime.of(2026, 10, 10, 9, 0));

        TotaisRepasses totais = TotaisRepasses.de(List.of(pendente, pago, cancelado));

        assertThat(totais.previsto()).isEqualByComparingTo("800.00");
        assertThat(totais.pago()).isEqualByComparingTo("450.00");
        assertThat(totais.pendente()).isEqualByComparingTo("300.00");
    }

    @Test
    void listaVaziaDaZero() {
        TotaisRepasses totais = TotaisRepasses.de(List.of());

        assertThat(totais.previsto()).isZero();
        assertThat(totais.pago()).isZero();
        assertThat(totais.pendente()).isZero();
    }

    @Test
    void cancelarRepassePagoMantemOValorPago() {
        Repasse pago = repasse("500.00");
        pago.registrarPagamento(HOJE, new BigDecimal("500.00"));

        pago.cancelar("Pago para a pessoa errada", "admin", LocalDateTime.of(2026, 10, 10, 9, 0));

        assertThat(pago.getSituacao()).isEqualTo(SituacaoRepasse.CANCELADO);
        assertThat(pago.getValorPago()).isEqualByComparingTo("500.00");
        assertThat(pago.getDataPagamento()).isEqualTo(HOJE);
    }

    @Test
    void repassePagoNaoRecebeOutroPagamento() {
        Repasse pago = repasse("500.00");
        pago.registrarPagamento(HOJE, new BigDecimal("500.00"));

        assertThatThrownBy(() -> pago.registrarPagamento(HOJE, new BigDecimal("10.00")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void atrasadoSoQuandoPendenteComDataPassada() {
        Repasse vencido = new Repasse(null, null, "Comissão", new BigDecimal("100.00"), HOJE.minusDays(1));
        Repasse hoje = new Repasse(null, null, "Comissão", new BigDecimal("100.00"), HOJE);
        Repasse pagoVencido = new Repasse(null, null, "Comissão", new BigDecimal("100.00"), HOJE.minusDays(1));
        pagoVencido.registrarPagamento(HOJE, new BigDecimal("100.00"));

        assertThat(vencido.isAtrasado(HOJE)).isTrue();
        assertThat(hoje.isAtrasado(HOJE)).isFalse();
        assertThat(pagoVencido.isAtrasado(HOJE)).isFalse();
    }

    private static Repasse repasse(String valorPrevisto) {
        return new Repasse(null, null, "Comissão", new BigDecimal(valorPrevisto), HOJE);
    }
}
