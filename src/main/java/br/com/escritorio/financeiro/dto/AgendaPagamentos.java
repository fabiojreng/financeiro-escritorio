package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.StatusPagamento;

import java.math.BigDecimal;
import java.util.List;

/** Pagamentos do período filtrado na agenda, com totais gerais e por status. */
public record AgendaPagamentos(List<PagamentoAgendado> pagamentos) {

    public BigDecimal getTotal() {
        return somar(pagamentos);
    }

    public long quantidade(StatusPagamento status) {
        return pagamentos.stream().filter(p -> p.getStatus() == status).count();
    }

    public BigDecimal total(StatusPagamento status) {
        return somar(pagamentos.stream().filter(p -> p.getStatus() == status).toList());
    }

    private static BigDecimal somar(List<PagamentoAgendado> lista) {
        return lista.stream().map(PagamentoAgendado::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
