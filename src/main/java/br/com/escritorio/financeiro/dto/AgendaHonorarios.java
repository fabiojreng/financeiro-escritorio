package br.com.escritorio.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Honorários do período, com totais de original, desconto, recebido e saldo. */
public record AgendaHonorarios(List<LinhaHonorario> linhas) {

    public long getQuantidadeComHonorario() {
        return linhas.stream().filter(l -> l.pagamento().getValorHonorario() != null).count();
    }

    public BigDecimal getTotalOriginal() {
        return somar(ValoresHonorario::original);
    }

    public BigDecimal getTotalDesconto() {
        return somar(ValoresHonorario::desconto);
    }

    public BigDecimal getTotalNegociado() {
        return somar(ValoresHonorario::negociado);
    }

    public BigDecimal getTotalRecebido() {
        return somar(ValoresHonorario::recebido);
    }

    public BigDecimal getTotalSaldo() {
        return somar(ValoresHonorario::saldo);
    }

    private BigDecimal somar(Function<ValoresHonorario, BigDecimal> valor) {
        return linhas.stream().map(LinhaHonorario::valores).map(valor).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
