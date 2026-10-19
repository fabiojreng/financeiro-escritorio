package br.com.escritorio.financeiro.domain;

public enum SituacaoHonorarioPagamento {

    A_RECEBER("A receber", "text-bg-warning",
            "O escritório ainda não recebeu o honorário deste pagamento."),
    NEGOCIADO("Negociado", "text-bg-info",
            "Há um acordo de honorários, ainda sem nenhum recebimento."),
    PARCIAL("Parcial", "text-bg-primary",
            "Parte do valor negociado já entrou, mas ainda há saldo."),
    EM_ATRASO("Em atraso", "text-bg-danger",
            "Há parcela vencida sem o pagamento completo."),
    RECEBIDO("Recebido", "text-bg-success",
            "O cliente já pagou o honorário ao escritório.");

    private final String rotulo;
    private final String classeBadge;
    private final String descricao;

    SituacaoHonorarioPagamento(String rotulo, String classeBadge, String descricao) {
        this.rotulo = rotulo;
        this.classeBadge = classeBadge;
        this.descricao = descricao;
    }

    /** Situações escolhidas na linha, quando não há negociação. */
    public static SituacaoHonorarioPagamento[] lancamentoSimples() {
        return new SituacaoHonorarioPagamento[] {A_RECEBER, RECEBIDO};
    }

    public boolean isLancamentoSimples() {
        return this == A_RECEBER || this == RECEBIDO;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getClasseBadge() {
        return classeBadge;
    }

    public String getDescricao() {
        return descricao;
    }
}
