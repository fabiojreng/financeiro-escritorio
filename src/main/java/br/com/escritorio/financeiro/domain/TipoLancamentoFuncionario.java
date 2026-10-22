package br.com.escritorio.financeiro.domain;

/**
 * Tipo da movimentação do funcionário. O tipo define se o valor aumenta ou reduz o líquido a pagar no acerto.
 */
public enum TipoLancamentoFuncionario {

    VALE("Vale / adiantamento", false, false),
    DESCONTO("Desconto", false, true),
    BONIFICACAO("Bonificação / comissão", true, false),
    HORA_EXTRA("Hora extra", true, false),
    REEMBOLSO("Reembolso", true, false);

    private final String rotulo;
    private final boolean soma;
    private final boolean exigeMotivo;

    TipoLancamentoFuncionario(String rotulo, boolean soma, boolean exigeMotivo) {
        this.rotulo = rotulo;
        this.soma = soma;
        this.exigeMotivo = exigeMotivo;
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

    /** Vales e adiantamentos são dinheiro entregue ao funcionário e pedem forma de pagamento. */
    public boolean isPagoAoFuncionario() {
        return this == VALE || this == REEMBOLSO;
    }

    public String getClasseBadge() {
        return soma ? "selo-sucesso" : "selo-alerta";
    }
}
