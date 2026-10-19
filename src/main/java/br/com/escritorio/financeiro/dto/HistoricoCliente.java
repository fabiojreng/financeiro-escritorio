package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;

import java.math.BigDecimal;
import java.util.List;

/** Histórico financeiro de um cliente a partir dos pagamentos da agenda. */
public record HistoricoCliente(Cliente cliente, List<LinhaHonorario> linhas,
                               BigDecimal totalBeneficio, BigDecimal totalHonorarios,
                               BigDecimal honorariosDesconto, BigDecimal honorariosRecebidos,
                               BigDecimal honorariosAReceber,
                               BigDecimal totalRepasses, BigDecimal repassesPagos, BigDecimal repassesPendentes) {

    public static HistoricoCliente de(Cliente cliente, AgendaHonorarios honorarios) {
        List<PagamentoAgendado> pagamentos = honorarios.linhas().stream().map(LinhaHonorario::pagamento).toList();
        AgendaPagamentos agenda = new AgendaPagamentos(pagamentos);
        return new HistoricoCliente(cliente, honorarios.linhas(), agenda.getTotal(), honorarios.getTotalOriginal(),
                honorarios.getTotalDesconto(), honorarios.getTotalRecebido(), honorarios.getTotalSaldo(),
                agenda.getTotalRepasses(),
                agenda.totalRepasses(SituacaoRepassePagamento.PAGO),
                agenda.totalRepasses(SituacaoRepassePagamento.PENDENTE));
    }

    public List<PagamentoAgendado> pagamentos() {
        return linhas.stream().map(LinhaHonorario::pagamento).toList();
    }
}
