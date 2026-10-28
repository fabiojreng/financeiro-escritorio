package br.com.escritorio.financeiro.dto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Honorários do período, com totais de original, desconto, recebido e saldo. A linha da origem já
 * soma o que foi pago nas continuações; continuações cuja origem está fora do período entram no
 * recebido pelo que foi pago nelas e no saldo uma vez por acordo.
 */
public record AgendaHonorarios(List<LinhaHonorario> linhas) {

    public long getQuantidadeComHonorario() {
        return linhas.stream()
                .filter(l -> l.pagamento().getValorHonorario() != null || l.valores().continuacao())
                .count();
    }

    /** Linhas da dívida raiz, sem as continuações, para totais do histórico da cliente. */
    public AgendaHonorarios daDivida() {
        return new AgendaHonorarios(linhas.stream().filter(l -> !l.valores().continuacao()).toList());
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
        BigDecimal continuacoes = continuacoesSemOrigem().stream()
                .map(ValoresHonorario::recebido)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return somar(ValoresHonorario::recebido).add(continuacoes);
    }

    public BigDecimal getTotalSaldo() {
        Map<Long, BigDecimal> saldoPorAcordo = new LinkedHashMap<>();
        continuacoesSemOrigem().stream()
                .filter(v -> v.saldoAcordo() != null)
                .forEach(v -> saldoPorAcordo.putIfAbsent(v.origemId(), v.saldoAcordo()));
        return saldoPorAcordo.values().stream().reduce(somar(ValoresHonorario::saldo), BigDecimal::add);
    }

    private List<ValoresHonorario> continuacoesSemOrigem() {
        Set<Long> ids = linhas.stream().map(l -> l.pagamento().getId()).collect(Collectors.toSet());
        return linhas.stream()
                .map(LinhaHonorario::valores)
                .filter(v -> v.continuacao() && !ids.contains(v.origemId()))
                .toList();
    }

    private BigDecimal somar(Function<ValoresHonorario, BigDecimal> valor) {
        return linhas.stream().map(LinhaHonorario::valores)
                .filter(v -> !v.continuacao())
                .map(valor).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
