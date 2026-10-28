package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;

import java.math.BigDecimal;
import java.util.List;

/**
 * Histórico financeiro de um cliente a partir dos pagamentos da agenda. Nos repasses parciais, a parte já paga
 * conta em {@code repassesPagos} e o saldo em {@code repassesPendentes}.
 */
public record HistoricoCliente(Cliente cliente, List<LinhaHonorario> linhas,
                               BigDecimal totalBeneficio, BigDecimal totalHonorarios,
                               BigDecimal honorariosDesconto, BigDecimal honorariosRecebidos,
                               BigDecimal honorariosAReceber,
                               BigDecimal totalRepasses, BigDecimal repassesPagos, BigDecimal repassesPendentes) {

    public static HistoricoCliente de(Cliente cliente, AgendaHonorarios honorarios) {
        List<PagamentoAgendado> pagamentos = honorarios.linhas().stream().map(LinhaHonorario::pagamento).toList();
        AgendaPagamentos agenda = new AgendaPagamentos(pagamentos);
        AgendaHonorarios divida = honorarios.daDivida();
        return new HistoricoCliente(cliente, honorarios.linhas(), agenda.getTotal(), divida.getTotalOriginal(),
                divida.getTotalDesconto(), divida.getTotalRecebido(), divida.getTotalSaldo(),
                agenda.getTotalRepasses(),
                agenda.getTotalRepassesPagos(),
                agenda.getTotalRepassesAPagar());
    }

    public List<PagamentoAgendado> pagamentos() {
        return linhas.stream().map(LinhaHonorario::pagamento).toList();
    }
}
