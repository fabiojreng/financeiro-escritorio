package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.CalculoHonorarios;
import br.com.escritorio.financeiro.domain.NegociacaoHonorario;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;

import java.math.BigDecimal;

/** Original, negociado, recebido e saldo de um honorário, com ou sem acordo. */
public record ValoresHonorario(BigDecimal original, BigDecimal negociado, BigDecimal desconto, BigDecimal recebido,
                               BigDecimal saldo, int quantidadeParcelas, boolean temNegociacao) {

    public static ValoresHonorario de(PagamentoAgendado pagamento, NegociacaoHonorario negociacao,
                                      BigDecimal recebido, int quantidadeParcelas) {
        BigDecimal pago = recebido == null ? BigDecimal.ZERO : recebido;
        if (negociacao != null) {
            return new ValoresHonorario(negociacao.getValorOriginal(), negociacao.getValorNegociado(),
                    negociacao.getDesconto(), pago,
                    CalculoHonorarios.saldo(negociacao.getValorNegociado(), pago), quantidadeParcelas, true);
        }
        BigDecimal original = pagamento.getValorHonorario();
        if (original == null) {
            return new ValoresHonorario(null, null, BigDecimal.ZERO, BigDecimal.ZERO, null, 0, false);
        }
        boolean quitado = pagamento.getSituacaoHonorario() == SituacaoHonorarioPagamento.RECEBIDO;
        BigDecimal valorRecebido = quitado ? original : BigDecimal.ZERO;
        return new ValoresHonorario(original, original, BigDecimal.ZERO, valorRecebido,
                CalculoHonorarios.saldo(original, valorRecebido), 0, false);
    }
}
