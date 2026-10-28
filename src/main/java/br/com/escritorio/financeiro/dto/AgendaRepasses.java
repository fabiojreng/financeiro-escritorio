package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.ResponsavelPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Repasses do período filtrado, uma linha por responsável de cada pagamento, com totais por situação. */
public record AgendaRepasses(List<ResponsavelPagamento> repasses) {

    public long getQuantidadeComRepasse() {
        return repasses.stream().filter(r -> r.getValorRepasse() != null).count();
    }

    public BigDecimal getTotalRepasses() {
        return somar(r -> true);
    }

    public BigDecimal totalRepasses(SituacaoRepassePagamento situacao) {
        return somar(r -> r.getSituacaoRepasse() == situacao);
    }

    /** Quanto já chegou aos responsáveis: os repasses pagos e a parte paga dos parciais. */
    public BigDecimal getTotalPago() {
        return repasses.stream().map(ResponsavelPagamento::getValorPago).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Quanto ainda falta pagar aos responsáveis: os repasses pendentes e o saldo dos parciais. */
    public BigDecimal getTotalAPagar() {
        return repasses.stream().map(ResponsavelPagamento::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somar(Predicate<ResponsavelPagamento> filtro) {
        return repasses.stream().filter(filtro).map(ResponsavelPagamento::getValorRepasse).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
