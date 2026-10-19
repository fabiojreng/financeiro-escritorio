package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/** Pagamentos do período filtrado na agenda, com totais gerais, por status, de honorários e de repasses. */
public record AgendaPagamentos(List<PagamentoAgendado> pagamentos) {

    public BigDecimal getTotal() {
        return somar(PagamentoAgendado::getValor, p -> true);
    }

    public long quantidade(StatusPagamento status) {
        return pagamentos.stream().filter(p -> p.getStatus() == status).count();
    }

    public BigDecimal total(StatusPagamento status) {
        return somar(PagamentoAgendado::getValor, p -> p.getStatus() == status);
    }

    public long getQuantidadeComHonorario() {
        return pagamentos.stream().filter(p -> p.getValorHonorario() != null).count();
    }

    public long getQuantidadeComRepasse() {
        return pagamentos.stream().filter(p -> p.getValorRepasse() != null).count();
    }

    public BigDecimal getTotalHonorarios() {
        return somar(PagamentoAgendado::getValorHonorario, p -> true);
    }

    public BigDecimal totalHonorarios(SituacaoHonorarioPagamento situacao) {
        return somar(PagamentoAgendado::getValorHonorario, p -> p.getSituacaoHonorario() == situacao);
    }

    public BigDecimal getTotalRepasses() {
        return somar(PagamentoAgendado::getValorRepasse, p -> true);
    }

    public BigDecimal totalRepasses(SituacaoRepassePagamento situacao) {
        return somar(PagamentoAgendado::getValorRepasse, p -> p.getSituacaoRepasse() == situacao);
    }

    private BigDecimal somar(Function<PagamentoAgendado, BigDecimal> valor, Predicate<PagamentoAgendado> filtro) {
        return pagamentos.stream().filter(filtro).map(valor).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
