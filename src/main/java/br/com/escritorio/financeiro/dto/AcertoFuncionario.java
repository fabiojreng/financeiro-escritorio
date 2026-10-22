package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.AcertoMensal;
import br.com.escritorio.financeiro.domain.FechamentoFuncionario;
import br.com.escritorio.financeiro.domain.Funcionario;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Acerto de um funcionário numa competência. Quando já está pago, os valores são o retrato gravado no
 * pagamento; senão são calculados com os lançamentos atuais.
 */
public record AcertoFuncionario(Funcionario funcionario, YearMonth competencia, AcertoMensal acerto,
                                FechamentoFuncionario fechamento) {

    public boolean isPago() {
        return fechamento != null && fechamento.isPago();
    }

    /** Valor que o escritório gastou no mês: salário, extras e reembolsos, menos os descontos. */
    public BigDecimal getCusto() {
        return acerto.salarioBase().add(acerto.bonificacoes()).add(acerto.reembolsos()).subtract(acerto.descontos());
    }
}
