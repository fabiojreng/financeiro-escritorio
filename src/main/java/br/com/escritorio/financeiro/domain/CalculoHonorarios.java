package br.com.escritorio.financeiro.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contas dos honorários. Só entram recebimentos não cancelados; o valor original combinado não
 * muda quando há negociação.
 */
public final class CalculoHonorarios {

    private CalculoHonorarios() {
    }

    public static BigDecimal totalRecebido(Collection<RecebimentoHonorario> recebimentos) {
        return recebimentos.stream()
                .filter(r -> !r.isCancelado())
                .map(RecebimentoHonorario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal saldo(BigDecimal devido, BigDecimal totalRecebido) {
        if (devido == null) {
            return null;
        }
        BigDecimal saldo = devido.subtract(totalRecebido == null ? BigDecimal.ZERO : totalRecebido);
        return saldo.signum() < 0 ? BigDecimal.ZERO : saldo;
    }

    /** Divide o total em parcelas iguais, com a diferença de centavos na última. */
    public static List<BigDecimal> dividir(BigDecimal total, int quantidade) {
        if (quantidade < 1) {
            throw new IllegalArgumentException("Quantidade de parcelas deve ser positiva.");
        }
        BigDecimal parcela = total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.DOWN);
        List<BigDecimal> valores = new ArrayList<>();
        for (int i = 1; i < quantidade; i++) {
            valores.add(parcela);
        }
        valores.add(total.subtract(parcela.multiply(BigDecimal.valueOf(quantidade - 1))));
        return valores;
    }

    public static SituacaoHonorarioPagamento situacao(BigDecimal valorHonorario,
                                                      SituacaoHonorarioPagamento situacaoAtual,
                                                      boolean temNegociacao, BigDecimal valorNegociado,
                                                      BigDecimal recebido, boolean temParcelaVencida) {
        if (!temNegociacao) {
            if (valorHonorario == null) {
                return SituacaoHonorarioPagamento.A_RECEBER;
            }
            return situacaoAtual == SituacaoHonorarioPagamento.RECEBIDO
                    || situacaoAtual == SituacaoHonorarioPagamento.PARCIAL
                    ? situacaoAtual
                    : SituacaoHonorarioPagamento.A_RECEBER;
        }
        BigDecimal pago = recebido == null ? BigDecimal.ZERO : recebido;
        if (pago.compareTo(valorNegociado) >= 0) {
            return SituacaoHonorarioPagamento.RECEBIDO;
        }
        if (temParcelaVencida) {
            return SituacaoHonorarioPagamento.EM_ATRASO;
        }
        if (pago.signum() > 0) {
            return SituacaoHonorarioPagamento.PARCIAL;
        }
        return SituacaoHonorarioPagamento.NEGOCIADO;
    }
}
