package br.com.escritorio.financeiro.domain;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Conta do acerto mensal do funcionário. Só entram lançamentos ativos; o líquido pode ficar negativo quando
 * os vales e descontos passam do salário.
 */
public final class CalculoAcerto {

    private CalculoAcerto() {
    }

    public static AcertoMensal calcular(BigDecimal salarioBase, Collection<LancamentoFuncionario> lancamentos) {
        BigDecimal base = salarioBase == null ? BigDecimal.ZERO : salarioBase;
        BigDecimal bonificacoes = total(lancamentos, TipoLancamentoFuncionario.BONIFICACAO)
                .add(total(lancamentos, TipoLancamentoFuncionario.HORA_EXTRA));
        BigDecimal reembolsos = total(lancamentos, TipoLancamentoFuncionario.REEMBOLSO);
        BigDecimal vales = total(lancamentos, TipoLancamentoFuncionario.VALE);
        BigDecimal parcelas = total(lancamentos, TipoLancamentoFuncionario.PARCELA_EMPRESTIMO);
        BigDecimal descontos = total(lancamentos, TipoLancamentoFuncionario.DESCONTO);
        BigDecimal liquido = base.add(bonificacoes).add(reembolsos)
                .subtract(vales).subtract(parcelas).subtract(descontos);
        return new AcertoMensal(base, bonificacoes, reembolsos, vales, parcelas, descontos, liquido);
    }

    private static BigDecimal total(Collection<LancamentoFuncionario> lancamentos, TipoLancamentoFuncionario tipo) {
        return lancamentos.stream()
                .filter(LancamentoFuncionario::isAtivo)
                .filter(l -> l.getTipo() == tipo)
                .map(LancamentoFuncionario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
