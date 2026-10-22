package br.com.escritorio.financeiro.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoAcertoTest {

    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);
    private static final Funcionario MARIA = new Funcionario("Maria", LocalDate.of(2025, 3, 1),
            new BigDecimal("2500.00"), 5);

    @Test
    void exemploDaDemanda() {
        AcertoMensal acerto = CalculoAcerto.calcular(new BigDecimal("2500.00"), List.of(
                lancamento(TipoLancamentoFuncionario.VALE, "300.00"),
                lancamento(TipoLancamentoFuncionario.VALE, "600.00"),
                lancamento(TipoLancamentoFuncionario.BONIFICACAO, "200.00"),
                lancamento(TipoLancamentoFuncionario.REEMBOLSO, "80.00"),
                lancamento(TipoLancamentoFuncionario.PARCELA_EMPRESTIMO, "250.00"),
                lancamento(TipoLancamentoFuncionario.DESCONTO, "50.00")));

        assertThat(acerto.salarioBase()).isEqualByComparingTo("2500.00");
        assertThat(acerto.bonificacoes()).isEqualByComparingTo("200.00");
        assertThat(acerto.reembolsos()).isEqualByComparingTo("80.00");
        assertThat(acerto.vales()).isEqualByComparingTo("900.00");
        assertThat(acerto.parcelas()).isEqualByComparingTo("250.00");
        assertThat(acerto.descontos()).isEqualByComparingTo("50.00");
        assertThat(acerto.liquido()).isEqualByComparingTo("1580.00");
        assertThat(acerto.isNegativo()).isFalse();
    }

    @Test
    void horaExtraEntraComoBonificacaoECanceladosFicamDeFora() {
        LancamentoFuncionario cancelado = lancamento(TipoLancamentoFuncionario.VALE, "1000.00");
        cancelado.cancelar("Lançado em duplicidade.", "ana", LocalDateTime.of(2026, 10, 10, 9, 0));

        AcertoMensal acerto = CalculoAcerto.calcular(new BigDecimal("2000.00"), List.of(
                lancamento(TipoLancamentoFuncionario.HORA_EXTRA, "150.00"),
                lancamento(TipoLancamentoFuncionario.BONIFICACAO, "50.00"),
                cancelado));

        assertThat(acerto.bonificacoes()).isEqualByComparingTo("200.00");
        assertThat(acerto.vales()).isEqualByComparingTo("0");
        assertThat(acerto.liquido()).isEqualByComparingTo("2200.00");
    }

    @Test
    void liquidoFicaNegativoQuandoOsDescontosPassamDoSalario() {
        AcertoMensal acerto = CalculoAcerto.calcular(new BigDecimal("1000.00"), List.of(
                lancamento(TipoLancamentoFuncionario.VALE, "800.00"),
                lancamento(TipoLancamentoFuncionario.DESCONTO, "300.00")));

        assertThat(acerto.liquido()).isEqualByComparingTo("-100.00");
        assertThat(acerto.isNegativo()).isTrue();
    }

    @Test
    void semSalarioNemLancamentosOAcertoEZero() {
        AcertoMensal acerto = CalculoAcerto.calcular(null, List.of());

        assertThat(acerto.liquido()).isEqualByComparingTo("0");
    }

    private static LancamentoFuncionario lancamento(TipoLancamentoFuncionario tipo, String valor) {
        LancamentoFuncionario lancamento = new LancamentoFuncionario(MARIA, tipo, LocalDate.of(2026, 10, 5), OUTUBRO,
                new BigDecimal(valor));
        if (tipo.isExigeMotivo()) {
            lancamento.setDescricao("Falta não justificada");
        }
        return lancamento;
    }
}
