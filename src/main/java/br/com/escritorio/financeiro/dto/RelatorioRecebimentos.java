package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.RecebimentoHonorario;

import java.math.BigDecimal;
import java.util.List;

/**
 * Recebimentos de honorários do período.
 *
 * @param totalConfirmado soma dos recebimentos não cancelados: o que entrou no caixa
 * @param totalCancelado  soma dos recebimentos cancelados, mostrada à parte
 */
public record RelatorioRecebimentos(List<RecebimentoHonorario> recebimentos, BigDecimal totalConfirmado,
                                    BigDecimal totalCancelado) {

    public static RelatorioRecebimentos de(List<RecebimentoHonorario> recebimentos) {
        BigDecimal confirmado = BigDecimal.ZERO;
        BigDecimal cancelado = BigDecimal.ZERO;
        for (RecebimentoHonorario r : recebimentos) {
            if (r.isCancelado()) {
                cancelado = cancelado.add(r.getValor());
            } else {
                confirmado = confirmado.add(r.getValor());
            }
        }
        return new RelatorioRecebimentos(recebimentos, confirmado, cancelado);
    }
}
