package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.CalculoHonorarios;
import br.com.escritorio.financeiro.domain.NegociacaoHonorario;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Original, negociado, recebido e saldo de um honorário, com ou sem acordo. */
public record ValoresHonorario(BigDecimal original, BigDecimal negociado, BigDecimal desconto, BigDecimal recebido,
                               BigDecimal saldo, int quantidadeParcelas, boolean temNegociacao,
                               boolean continuacao, Long origemId, LocalDate dataOrigem, BigDecimal saldoAcordo,
                               boolean origemNegociada) {

    public static ValoresHonorario de(PagamentoAgendado pagamento, NegociacaoHonorario negociacao,
                                      BigDecimal recebido, int quantidadeParcelas) {
        BigDecimal pago = recebido == null ? BigDecimal.ZERO : recebido;
        if (negociacao != null) {
            BigDecimal saldo = CalculoHonorarios.saldo(negociacao.getValorNegociado(), pago);
            return new ValoresHonorario(negociacao.getValorOriginal(), negociacao.getValorNegociado(),
                    negociacao.getDesconto(), pago, saldo, quantidadeParcelas, true, false, null, null, saldo, false);
        }
        BigDecimal original = pagamento.getValorHonorario();
        if (original == null) {
            return vazio();
        }
        BigDecimal saldo = CalculoHonorarios.saldo(original, pago);
        return new ValoresHonorario(original, original, BigDecimal.ZERO, pago, saldo, 0, false, false, null, null,
                saldo, false);
    }

    /**
     * Pagamento que continua o honorário de outro: a dívida fica na origem, aqui só entra o que a
     * cliente pagou ao escritório com este crédito. O saldo é o do acordo inteiro.
     */
    public static ValoresHonorario fatia(PagamentoAgendado pagamento, PagamentoAgendado origem,
                                         ValoresHonorario acordo) {
        return new ValoresHonorario(null, null, BigDecimal.ZERO, CalculoHonorarios.pagoNaContinuacao(pagamento),
                null, 0, acordo.temNegociacao(), true, origem.getId(), origem.getDataRecebimento(), acordo.saldo(),
                acordo.temNegociacao());
    }

    public static ValoresHonorario vazio() {
        return new ValoresHonorario(null, null, BigDecimal.ZERO, BigDecimal.ZERO, null, 0, false, false, null, null,
                null, false);
    }
}
