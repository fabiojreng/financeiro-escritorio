package br.com.escritorio.financeiro.domain;

public enum SituacaoHonorarioPagamento {

    A_RECEBER("A receber", "text-bg-warning",
            "O escritório ainda não recebeu o honorário deste pagamento."),
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
