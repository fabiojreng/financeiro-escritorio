package br.com.escritorio.financeiro.domain;

import br.com.escritorio.financeiro.dto.ResumoHonorarios;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoHonorariosTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 10);

    @Test
    void recebimentoParcialReduzOSaldo() {
        AcordoHonorarios acordo = acordo("3000.00");

        ResumoHonorarios resumo = ResumoHonorarios.de(acordo, List.of(), List.of(recebimento(acordo, null, "1000.00")));

        assertThat(resumo.totalRecebido()).isEqualByComparingTo("1000.00");
        assertThat(resumo.saldo()).isEqualByComparingTo("2000.00");
    }

    @Test
    void recebimentoCanceladoVoltaParaOSaldo() {
        AcordoHonorarios acordo = acordo("3000.00");
        RecebimentoHonorario valido = recebimento(acordo, null, "1000.00");
        RecebimentoHonorario cancelado = recebimento(acordo, null, "500.00");
        cancelado.cancelar("Lançado em duplicidade", "admin", LocalDateTime.now());

        ResumoHonorarios resumo = ResumoHonorarios.de(acordo, List.of(), List.of(valido, cancelado));

        assertThat(resumo.totalRecebido()).isEqualByComparingTo("1000.00");
        assertThat(resumo.saldo()).isEqualByComparingTo("2000.00");
    }

    @Test
    void semValorContratadoOSaldoFicaADefinir() {
        AcordoHonorarios acordo = new AcordoHonorarios(null, FormaCobranca.PERCENTUAL, HOJE);
        acordo.definirCobranca(FormaCobranca.PERCENTUAL, new BigDecimal("30"), "Retroativos");

        ResumoHonorarios resumo = ResumoHonorarios.de(acordo, List.of(), List.of(recebimento(acordo, null, "200.00")));

        assertThat(acordo.getValorContratado()).isNull();
        assertThat(resumo.saldo()).isNull();
        assertThat(resumo.totalRecebido()).isEqualByComparingTo("200.00");
    }

    @Test
    void recebidoPorParcelaConsideraSoOsRecebimentosDela() {
        AcordoHonorarios acordo = acordo("2000.00");
        ParcelaHonorario primeira = comId(new ParcelaHonorario(acordo, 1, HOJE, new BigDecimal("1000.00")), 1L);
        ParcelaHonorario segunda = comId(new ParcelaHonorario(acordo, 2, HOJE.plusMonths(1), new BigDecimal("1000.00")), 2L);

        ResumoHonorarios resumo = ResumoHonorarios.de(acordo, List.of(primeira, segunda),
                List.of(recebimento(acordo, primeira, "400.00"), recebimento(acordo, null, "100.00")));

        assertThat(resumo.parcelas().get(0).recebido()).isEqualByComparingTo("400.00");
        assertThat(resumo.parcelas().get(0).getPendente()).isEqualByComparingTo("600.00");
        assertThat(resumo.parcelas().get(1).recebido()).isEqualByComparingTo("0");
        assertThat(resumo.saldo()).isEqualByComparingTo("1500.00");
        assertThat(resumo.isParcelasDivergentes()).isFalse();
    }

    @Test
    void divideComDiferencaDeCentavosNaUltimaParcela() {
        assertThat(CalculoHonorarios.dividir(new BigDecimal("1000.00"), 3))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("333.33"), new BigDecimal("333.33"), new BigDecimal("333.34"));
        assertThat(CalculoHonorarios.dividir(new BigDecimal("150.00"), 1))
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("150.00"));
    }

    @Test
    void parcelaFicaPagaQuandoORecebidoCobreOValorEReabreSeDiminuir() {
        ParcelaHonorario parcela = new ParcelaHonorario(acordo("1000.00"), 1, HOJE, new BigDecimal("500.00"));

        parcela.atualizarPagamento(new BigDecimal("500.00"));
        assertThat(parcela.getSituacao()).isEqualTo(SituacaoParcela.PAGA);

        parcela.atualizarPagamento(new BigDecimal("200.00"));
        assertThat(parcela.getSituacao()).isEqualTo(SituacaoParcela.ABERTA);
    }

    @Test
    void parcelaAbertaComVencimentoPassadoEstaVencida() {
        ParcelaHonorario parcela = new ParcelaHonorario(acordo("1000.00"), 1, HOJE.minusDays(1), new BigDecimal("500.00"));

        assertThat(parcela.isVencida(HOJE)).isTrue();
        parcela.atualizarPagamento(new BigDecimal("500.00"));
        assertThat(parcela.isVencida(HOJE)).isFalse();
    }

    private static AcordoHonorarios acordo(String valor) {
        AcordoHonorarios acordo = new AcordoHonorarios(null, FormaCobranca.VALOR_FIXO, HOJE);
        acordo.setValorContratado(new BigDecimal(valor));
        return acordo;
    }

    private static RecebimentoHonorario recebimento(AcordoHonorarios acordo, ParcelaHonorario parcela, String valor) {
        return new RecebimentoHonorario(acordo, parcela, HOJE, new BigDecimal(valor), FormaPagamento.PIX);
    }

    private static <T extends EntidadeBase> T comId(T entidade, Long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
