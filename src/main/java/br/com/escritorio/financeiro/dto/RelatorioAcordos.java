package br.com.escritorio.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Acordos de honorários firmados no período, com o recebido e o saldo de hoje.
 *
 * @param contratado      soma do valor contratado (acordos com valor a definir não entram)
 * @param recebido        soma do recebido nesses acordos, sem os recebimentos cancelados
 * @param saldo           soma dos saldos pendentes dos acordos com valor definido
 * @param acordosSemValor quantidade de acordos com valor ainda a definir
 */
public record RelatorioAcordos(List<AcordoComTotais> acordos, BigDecimal contratado, BigDecimal recebido,
                               BigDecimal saldo, long acordosSemValor) {

    public static RelatorioAcordos de(List<AcordoComTotais> acordos) {
        return new RelatorioAcordos(acordos,
                acordos.stream().map(a -> a.acordo().getValorContratado()).filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                acordos.stream().map(AcordoComTotais::recebido).reduce(BigDecimal.ZERO, BigDecimal::add),
                acordos.stream().map(AcordoComTotais::saldo).filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                acordos.stream().filter(a -> a.saldo() == null).count());
    }
}
