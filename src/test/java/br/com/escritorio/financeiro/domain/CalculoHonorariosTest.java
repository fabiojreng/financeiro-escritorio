package br.com.escritorio.financeiro.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoHonorariosTest {

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
    void saldoNaoFicaNegativo() {
        assertThat(CalculoHonorarios.saldo(new BigDecimal("100.00"), new BigDecimal("40.00")))
                .isEqualByComparingTo("60.00");
        assertThat(CalculoHonorarios.saldo(new BigDecimal("100.00"), new BigDecimal("150.00")))
                .isEqualByComparingTo("0");
    }

    @Test
    void situacaoSemNegociacaoPreservaLancamentoSimples() {
        assertThat(CalculoHonorarios.situacao(new BigDecimal("500.00"), SituacaoHonorarioPagamento.RECEBIDO,
                false, null, BigDecimal.ZERO, false)).isEqualTo(SituacaoHonorarioPagamento.RECEBIDO);
        assertThat(CalculoHonorarios.situacao(new BigDecimal("500.00"), SituacaoHonorarioPagamento.A_RECEBER,
                false, null, BigDecimal.ZERO, false)).isEqualTo(SituacaoHonorarioPagamento.A_RECEBER);
        assertThat(CalculoHonorarios.situacao(null, SituacaoHonorarioPagamento.A_RECEBER, false, null,
                BigDecimal.ZERO, false)).isEqualTo(SituacaoHonorarioPagamento.A_RECEBER);
    }

    @Test
    void situacaoComNegociacaoUsaRecebimentosEAtraso() {
        BigDecimal negociado = new BigDecimal("400.00");
        assertThat(CalculoHonorarios.situacao(new BigDecimal("500.00"), SituacaoHonorarioPagamento.A_RECEBER,
                true, negociado, BigDecimal.ZERO, false)).isEqualTo(SituacaoHonorarioPagamento.NEGOCIADO);
        assertThat(CalculoHonorarios.situacao(new BigDecimal("500.00"), SituacaoHonorarioPagamento.NEGOCIADO,
                true, negociado, new BigDecimal("100.00"), false)).isEqualTo(SituacaoHonorarioPagamento.PARCIAL);
        assertThat(CalculoHonorarios.situacao(new BigDecimal("500.00"), SituacaoHonorarioPagamento.PARCIAL,
                true, negociado, new BigDecimal("100.00"), true)).isEqualTo(SituacaoHonorarioPagamento.EM_ATRASO);
        assertThat(CalculoHonorarios.situacao(new BigDecimal("500.00"), SituacaoHonorarioPagamento.PARCIAL,
                true, negociado, new BigDecimal("400.00"), true)).isEqualTo(SituacaoHonorarioPagamento.RECEBIDO);
    }

    @Test
    void parcelaFicaPagaQuandoORecebidoCobreOValorEReabreSeDiminuir() {
        NegociacaoHonorario negociacao = negociacao();
        ParcelaHonorario parcela = new ParcelaHonorario(negociacao, 1, java.time.LocalDate.of(2026, 10, 10),
                new BigDecimal("500.00"));

        parcela.atualizarPagamento(new BigDecimal("500.00"));
        assertThat(parcela.getSituacao()).isEqualTo(SituacaoParcela.PAGA);

        parcela.atualizarPagamento(new BigDecimal("200.00"));
        assertThat(parcela.getSituacao()).isEqualTo(SituacaoParcela.ABERTA);
    }

    @Test
    void parcelaAbertaComVencimentoPassadoEstaVencida() {
        java.time.LocalDate hoje = java.time.LocalDate.of(2026, 10, 19);
        ParcelaHonorario parcela = new ParcelaHonorario(negociacao(), 1, hoje.minusDays(1), new BigDecimal("500.00"));

        assertThat(parcela.isVencida(hoje)).isTrue();
        parcela.atualizarPagamento(new BigDecimal("500.00"));
        assertThat(parcela.isVencida(hoje)).isFalse();
    }

    private static NegociacaoHonorario negociacao() {
        PagamentoAgendado pagamento = new PagamentoAgendado(new Cliente("Maria", java.time.LocalDate.of(2026, 1, 1)),
                java.util.List.of(new Responsavel("Ana", null)), new TipoServico("Benefício", null),
                java.time.LocalDate.of(2026, 10, 10), new BigDecimal("1500.00"), "Banco");
        pagamento.setValorHonorario(new BigDecimal("500.00"));
        return new NegociacaoHonorario(pagamento, new BigDecimal("500.00"), new BigDecimal("400.00"),
                "Desconto combinado", java.time.LocalDate.of(2026, 10, 10), "ana");
    }
}
