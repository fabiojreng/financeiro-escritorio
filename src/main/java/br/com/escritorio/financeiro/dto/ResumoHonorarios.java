package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import br.com.escritorio.financeiro.domain.CalculoHonorarios;
import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.RecebimentoHonorario;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Situação financeira de um acordo de honorários: parcelas, recebimentos, total recebido e saldo. */
public record ResumoHonorarios(AcordoHonorarios acordo, List<ParcelaComRecebido> parcelas,
                               List<RecebimentoHonorario> recebimentos, BigDecimal totalRecebido,
                               BigDecimal saldo, BigDecimal totalParcelasAtivas) {

    public static ResumoHonorarios de(AcordoHonorarios acordo, List<ParcelaHonorario> parcelas,
                                      List<RecebimentoHonorario> recebimentos) {
        BigDecimal totalRecebido = CalculoHonorarios.totalRecebido(recebimentos);
        List<ParcelaComRecebido> comRecebido = parcelas.stream()
                .map(p -> new ParcelaComRecebido(p, CalculoHonorarios.totalRecebido(recebimentos.stream()
                        .filter(r -> r.getParcela() != null && Objects.equals(r.getParcela().getId(), p.getId()))
                        .toList())))
                .toList();
        BigDecimal totalParcelas = parcelas.stream()
                .filter(ParcelaHonorario::isAtiva)
                .map(ParcelaHonorario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumoHonorarios(acordo, comRecebido, recebimentos, totalRecebido,
                CalculoHonorarios.saldo(acordo.getValorContratado(), totalRecebido), totalParcelas);
    }

    public boolean isPossuiParcelasAtivas() {
        return parcelas.stream().anyMatch(p -> p.parcela().isAtiva());
    }

    /** Parcelas ativas cuja soma difere do valor contratado merecem aviso na tela. */
    public boolean isParcelasDivergentes() {
        return acordo.getValorContratado() != null && isPossuiParcelasAtivas()
                && totalParcelasAtivas.compareTo(acordo.getValorContratado()) != 0;
    }

    public record ParcelaComRecebido(ParcelaHonorario parcela, BigDecimal recebido) {

        public BigDecimal getPendente() {
            BigDecimal pendente = parcela.getValor().subtract(recebido);
            return pendente.signum() > 0 ? pendente : BigDecimal.ZERO;
        }
    }
}
