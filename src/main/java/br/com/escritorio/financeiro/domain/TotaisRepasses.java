package br.com.escritorio.financeiro.domain;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Totais próprios dos repasses. Repasses cancelados ficam de fora; nada de honorários ou do
 * benefício do INSS entra aqui.
 *
 * @param previsto soma do valor previsto dos repasses pendentes e pagos
 * @param pago     soma do valor efetivamente pago
 * @param pendente soma do valor previsto dos repasses ainda não pagos
 */
public record TotaisRepasses(BigDecimal previsto, BigDecimal pago, BigDecimal pendente) {

    public static TotaisRepasses de(Collection<Repasse> repasses) {
        return new TotaisRepasses(
                somar(repasses, r -> !r.isCancelado(), Repasse::getValorPrevisto),
                somar(repasses, Repasse::isPago, Repasse::getValorPago),
                somar(repasses, Repasse::isPendente, Repasse::getValorPrevisto));
    }

    private static BigDecimal somar(Collection<Repasse> repasses, Predicate<Repasse> filtro,
                                    Function<Repasse, BigDecimal> valor) {
        return repasses.stream().filter(filtro).map(valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
