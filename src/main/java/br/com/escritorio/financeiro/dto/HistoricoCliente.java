package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;

import java.math.BigDecimal;
import java.util.List;

/** Histórico financeiro de um cliente a partir dos pagamentos da agenda. */
public record HistoricoCliente(Cliente cliente, List<PagamentoAgendado> pagamentos,
                               BigDecimal totalBeneficio, BigDecimal totalHonorarios,
                               BigDecimal honorariosRecebidos, BigDecimal honorariosAReceber,
                               BigDecimal totalRepasses, BigDecimal repassesPagos, BigDecimal repassesPendentes) {

    public static HistoricoCliente de(Cliente cliente, List<PagamentoAgendado> pagamentos) {
        AgendaPagamentos agenda = new AgendaPagamentos(pagamentos);
        return new HistoricoCliente(cliente, pagamentos, agenda.getTotal(), agenda.getTotalHonorarios(),
                agenda.totalHonorarios(SituacaoHonorarioPagamento.RECEBIDO),
                agenda.totalHonorarios(SituacaoHonorarioPagamento.A_RECEBER),
                agenda.getTotalRepasses(),
                agenda.totalRepasses(SituacaoRepassePagamento.PAGO),
                agenda.totalRepasses(SituacaoRepassePagamento.PENDENTE));
    }
}
