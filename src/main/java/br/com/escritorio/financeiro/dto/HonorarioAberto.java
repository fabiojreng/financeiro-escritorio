package br.com.escritorio.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;

/** Honorários raiz ainda em aberto do mesmo cliente e benefício, para o formulário da agenda. */
public record HonorarioAberto(Long id, String dataRecebimento, String dataFormatada, String tipoServico,
                              BigDecimal original, BigDecimal recebido, BigDecimal saldo, boolean negociado) {

    public record Lista(List<HonorarioAberto> origens) {
    }
}
