package br.com.escritorio.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PendenciaParcelaHonorario(Long pagamentoId, String clienteNome, int numero, int totalParcelas,
                                        LocalDate vencimento, BigDecimal valor, BigDecimal saldo, boolean vencida) {
}
