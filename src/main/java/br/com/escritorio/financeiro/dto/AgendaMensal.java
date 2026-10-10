package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Resultado da agenda. O total estimado ignora previsões canceladas; o total confirmado soma
 * apenas o que foi liberado pelo INSS.
 */
public record AgendaMensal(List<PrevisaoBeneficio> previsoes, BigDecimal totalEstimado,
                           BigDecimal totalConfirmado) {

    public static AgendaMensal de(List<PrevisaoBeneficio> previsoes) {
        BigDecimal estimado = previsoes.stream()
                .filter(p -> p.getSituacao() != SituacaoPrevisao.CANCELADO)
                .map(PrevisaoBeneficio::getValorEstimado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal confirmado = previsoes.stream()
                .filter(PrevisaoBeneficio::isLiberada)
                .map(PrevisaoBeneficio::getValorConfirmado)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AgendaMensal(previsoes, estimado, confirmado);
    }
}
