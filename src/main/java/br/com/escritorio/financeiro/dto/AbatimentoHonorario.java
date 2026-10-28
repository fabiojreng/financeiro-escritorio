package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.PagamentoAgendado;

import java.math.BigDecimal;

/** Crédito da agenda que entrou no honorário da origem: o lançamento inicial e cada continuação. */
public record AbatimentoHonorario(PagamentoAgendado pagamento, BigDecimal valorPago, boolean raiz) {
}
