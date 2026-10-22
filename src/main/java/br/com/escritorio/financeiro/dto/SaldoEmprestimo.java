package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.EmprestimoFuncionario;

import java.math.BigDecimal;

/**
 * Empréstimo com o saldo devedor: soma das parcelas ativas de competências cujo acerto ainda não foi pago.
 */
public record SaldoEmprestimo(EmprestimoFuncionario emprestimo, BigDecimal saldo, int parcelasRestantes) {

    /** Em aberto, mas com todas as parcelas já descontadas em acertos pagos. */
    public boolean isTotalmenteDescontado() {
        return emprestimo.isEmAberto() && saldo.signum() == 0;
    }
}
