package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.RecebimentoHonorario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ParcelaComRecebimentos(ParcelaHonorario parcela, BigDecimal recebido, BigDecimal saldo,
                                     List<RecebimentoHonorario> recebimentos) {

    public boolean isVencida(LocalDate hoje) {
        return parcela.isVencida(hoje);
    }
}
