package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.FechamentoFuncionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;

import java.math.BigDecimal;
import java.util.List;

/** Lançamentos ativos e acertos pagos no período, pela data. */
public record ExtratoFuncionario(List<LancamentoFuncionario> lancamentos, List<FechamentoFuncionario> acertosPagos) {

    public BigDecimal getTotalCreditos() {
        return lancamentos.stream()
                .filter(l -> l.getTipo().isSoma())
                .map(LancamentoFuncionario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalDebitos() {
        return lancamentos.stream()
                .filter(l -> !l.getTipo().isSoma())
                .map(LancamentoFuncionario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalAcertosPagos() {
        return acertosPagos.stream()
                .map(FechamentoFuncionario::getLiquido)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
