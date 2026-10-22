package br.com.escritorio.financeiro.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Tipo da movimentação do funcionário. O tipo define se o valor aumenta ou reduz o líquido a pagar no acerto.
 */
public enum TipoLancamentoFuncionario {

    VALE("Vale / adiantamento", false, false, true),
    PARCELA_EMPRESTIMO("Parcela de empréstimo", false, false, false),
    DESCONTO("Desconto", false, true, true),
    BONIFICACAO("Bonificação / comissão", true, false, true),
    HORA_EXTRA("Hora extra", true, false, true),
    REEMBOLSO("Reembolso", true, false, true);

    private final String rotulo;
    private final boolean soma;
    private final boolean exigeMotivo;
    private final boolean manual;

    TipoLancamentoFuncionario(String rotulo, boolean soma, boolean exigeMotivo, boolean manual) {
        this.rotulo = rotulo;
        this.soma = soma;
        this.exigeMotivo = exigeMotivo;
        this.manual = manual;
    }

    /** Tipos que podem ser lançados à mão; a parcela de empréstimo só nasce de um empréstimo. */
    public static List<TipoLancamentoFuncionario> manuais() {
        return Arrays.stream(values()).filter(TipoLancamentoFuncionario::isManual).toList();
    }

    public String getRotulo() {
        return rotulo;
    }

    /** {@code true} quando o valor aumenta o líquido a pagar; {@code false} quando reduz. */
    public boolean isSoma() {
        return soma;
    }

    public boolean isExigeMotivo() {
        return exigeMotivo;
    }

    public boolean isManual() {
        return manual;
    }

    /** Vales e adiantamentos são dinheiro entregue ao funcionário e pedem forma de pagamento. */
    public boolean isPagoAoFuncionario() {
        return this == VALE || this == REEMBOLSO;
    }

    public String getClasseBadge() {
        return soma ? "selo-sucesso" : "selo-alerta";
    }
}
