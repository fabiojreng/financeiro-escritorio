package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.TotaisRepasses;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Histórico financeiro de um cliente, atendimento por atendimento. Os totais do benefício (valores
 * do cliente) e os de honorários e repasses (valores do escritório) ficam separados.
 */
public record HistoricoCliente(Cliente cliente, List<HistoricoAtendimento> atendimentos,
                               BigDecimal beneficioEstimado, BigDecimal beneficioConfirmado,
                               BigDecimal honorariosContratados, BigDecimal honorariosRecebidos,
                               BigDecimal honorariosSaldo, TotaisRepasses repasses) {

    /**
     * @param honorarios {@code null} quando o atendimento não tem acordo de honorários
     */
    public record HistoricoAtendimento(Atendimento atendimento, AgendaMensal previsoes, ResumoHonorarios honorarios,
                                       List<Repasse> repasses, TotaisRepasses totaisRepasses) {
    }

    public static HistoricoCliente de(Cliente cliente, List<HistoricoAtendimento> atendimentos) {
        List<ResumoHonorarios> honorarios = atendimentos.stream().map(HistoricoAtendimento::honorarios)
                .filter(Objects::nonNull).toList();
        return new HistoricoCliente(cliente, atendimentos,
                atendimentos.stream().map(a -> a.previsoes().totalEstimado()).reduce(BigDecimal.ZERO, BigDecimal::add),
                atendimentos.stream().map(a -> a.previsoes().totalConfirmado()).reduce(BigDecimal.ZERO, BigDecimal::add),
                honorarios.stream().map(h -> h.acordo().getValorContratado()).filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                honorarios.stream().map(ResumoHonorarios::totalRecebido).reduce(BigDecimal.ZERO, BigDecimal::add),
                honorarios.stream().map(ResumoHonorarios::saldo).filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                TotaisRepasses.de(atendimentos.stream().flatMap(a -> a.repasses().stream()).toList()));
    }
}
