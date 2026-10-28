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

    /**
     * Quanto o lançamento simples deste pagamento já entrou no caixa: o total quando está
     * "Recebido", o valor pago quando está "Parcial", zero nas demais.
     */
    public static BigDecimal pagoDoLancamento(PagamentoAgendado pagamento) {
        if (pagamento.getSituacaoHonorario() == SituacaoHonorarioPagamento.RECEBIDO) {
            return pagamento.getValorHonorario() == null ? BigDecimal.ZERO : pagamento.getValorHonorario();
        }
        if (pagamento.getSituacaoHonorario() == SituacaoHonorarioPagamento.PARCIAL) {
            return pagamento.getValorHonorarioPago() == null ? BigDecimal.ZERO : pagamento.getValorHonorarioPago();
        }
        return BigDecimal.ZERO;
    }

    /** Quanto a cliente pagou ao escritório com o crédito de um pagamento que continua outro honorário. */
    public static BigDecimal pagoNaContinuacao(PagamentoAgendado continuacao) {
        return continuacao.getValorHonorarioPago() == null ? BigDecimal.ZERO : continuacao.getValorHonorarioPago();
    }

    /**
     * Quanto foi pago no próprio pagamento de origem. Sem continuações, "Recebido" sem valor pago
     * guardado significa o honorário inteiro.
     */
    public static BigDecimal pagoNaOrigem(PagamentoAgendado origem, boolean temContinuacoes) {
        if (origem.getValorHonorarioPago() != null) {
            return origem.getValorHonorarioPago();
        }
        if (!temContinuacoes && origem.getSituacaoHonorario() == SituacaoHonorarioPagamento.RECEBIDO
                && origem.getValorHonorario() != null) {
            return origem.getValorHonorario();
        }
        return BigDecimal.ZERO;
    }

    public static BigDecimal pagoNasContinuacoes(Collection<PagamentoAgendado> continuacoes) {
        return continuacoes.stream()
                .map(CalculoHonorarios::pagoNaContinuacao)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Total já recebido da dívida da origem: parcelas da negociação, o que foi pago na própria origem e,
     * sem negociação, o que foi pago nos pagamentos que continuam aquele honorário.
     */
    public static BigDecimal recebidoDaOrigem(PagamentoAgendado origem, Collection<PagamentoAgendado> continuacoes,
                                              BigDecimal recebimentosParcelas, boolean temNegociacao) {
        BigDecimal parcelas = recebimentosParcelas == null ? BigDecimal.ZERO : recebimentosParcelas;
        if (temNegociacao) {
            return parcelas.add(origem.getValorHonorarioPago() == null
                    ? BigDecimal.ZERO : origem.getValorHonorarioPago());
        }
        return parcelas.add(pagoNaOrigem(origem, !continuacoes.isEmpty())).add(pagoNasContinuacoes(continuacoes));
    }

    public static SituacaoHonorarioPagamento situacao(BigDecimal valorHonorario,
                                                      SituacaoHonorarioPagamento situacaoAtual,
                                                      boolean temNegociacao, BigDecimal valorNegociado,
                                                      BigDecimal recebido, boolean temParcelaVencida) {
        return situacao(valorHonorario, situacaoAtual, temNegociacao, valorNegociado, recebido, temParcelaVencida,
                false);
    }

    /**
     * @param recalcularPorRecebido quando a origem tem continuações, a situação simples acompanha o
     *                              total recebido em vez de preservar o lançamento manual
     */
    public static SituacaoHonorarioPagamento situacao(BigDecimal valorHonorario,
                                                      SituacaoHonorarioPagamento situacaoAtual,
                                                      boolean temNegociacao, BigDecimal valorNegociado,
                                                      BigDecimal recebido, boolean temParcelaVencida,
                                                      boolean recalcularPorRecebido) {
        if (!temNegociacao) {
            if (valorHonorario == null) {
                return SituacaoHonorarioPagamento.A_RECEBER;
            }
            if (recalcularPorRecebido) {
                BigDecimal pago = recebido == null ? BigDecimal.ZERO : recebido;
                if (pago.compareTo(valorHonorario) >= 0) {
                    return SituacaoHonorarioPagamento.RECEBIDO;
                }
                if (pago.signum() > 0) {
                    return SituacaoHonorarioPagamento.PARCIAL;
                }
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
