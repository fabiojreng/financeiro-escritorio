package br.com.escritorio.financeiro.domain;

import java.math.BigDecimal;

/**
 * Quadro do acerto de um funcionário numa competência: salário base, mais bonificações, horas extras e
 * reembolsos, menos vales e descontos.
 */
public record AcertoMensal(BigDecimal salarioBase,
                           BigDecimal bonificacoes,
                           BigDecimal reembolsos,
                           BigDecimal vales,
                           BigDecimal descontos,
                           BigDecimal liquido) {

    public static final AcertoMensal ZERADO = new AcertoMensal(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public boolean isNegativo() {
        return liquido.signum() < 0;
    }

    public AcertoMensal somar(AcertoMensal outro) {
        return new AcertoMensal(salarioBase.add(outro.salarioBase), bonificacoes.add(outro.bonificacoes),
                reembolsos.add(outro.reembolsos), vales.add(outro.vales), descontos.add(outro.descontos),
                liquido.add(outro.liquido));
    }
}
