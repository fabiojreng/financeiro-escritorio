package br.com.escritorio.financeiro.domain;

public enum SituacaoHonorarioPagamento {

    A_RECEBER("A receber", "selo-pendente",
            "O escritório ainda não recebeu o honorário deste pagamento."),
    NEGOCIADO("Negociado", "selo-acordo",
            "Há um acordo de honorários, ainda sem nenhum recebimento."),
    PARCIAL("Parcial", "selo-parcial",
            "Parte do honorário já foi paga, mas ainda há saldo."),
    EM_ATRASO("Em atraso", "selo-alerta",
            "Há parcela vencida sem o pagamento completo."),
    RECEBIDO("Recebido", "selo-sucesso",
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
        return new SituacaoHonorarioPagamento[] {A_RECEBER, PARCIAL, RECEBIDO};
    }

    public boolean isLancamentoSimples() {
        return this == A_RECEBER || this == PARCIAL || this == RECEBIDO;
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
