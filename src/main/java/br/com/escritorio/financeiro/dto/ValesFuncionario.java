package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;

import java.math.BigDecimal;
import java.util.List;

/** Vales e adiantamentos de um funcionário na competência. */
public record ValesFuncionario(Funcionario funcionario, List<LancamentoFuncionario> vales) {

    public BigDecimal getTotal() {
        return vales.stream()
                .map(LancamentoFuncionario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
