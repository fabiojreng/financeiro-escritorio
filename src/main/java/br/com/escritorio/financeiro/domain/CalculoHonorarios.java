package br.com.escritorio.financeiro.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Contas dos honorários. Só entram recebimentos não cancelados do próprio acordo: benefício do INSS,
 * datas de liberação e repasses não fazem parte do saldo.
 */
public final class CalculoHonorarios {

    private CalculoHonorarios() {
    }

    public static BigDecimal totalRecebido(Collection<RecebimentoHonorario> recebimentos) {
        return recebimentos.stream()
                .filter(r -> !r.isCancelado())
                .map(RecebimentoHonorario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Saldo pendente; {@code null} enquanto o valor contratado não foi definido. */
    public static BigDecimal saldo(BigDecimal valorContratado, BigDecimal totalRecebido) {
        return valorContratado == null ? null : valorContratado.subtract(totalRecebido);
    }

    /** Divide o total em parcelas iguais, com a diferença de centavos na última. */
    public static List<BigDecimal> dividir(BigDecimal total, int quantidade) {
        if (quantidade < 1) {
            throw new IllegalArgumentException("Quantidade de parcelas deve ser positiva.");
        }
        BigDecimal parcela = total.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.DOWN);
        List<BigDecimal> valores = new ArrayList<>();
        for (int i = 1; i < quantidade; i++) {
            valores.add(parcela);
        }
        valores.add(total.subtract(parcela.multiply(BigDecimal.valueOf(quantidade - 1))));
        return valores;
    }
}
