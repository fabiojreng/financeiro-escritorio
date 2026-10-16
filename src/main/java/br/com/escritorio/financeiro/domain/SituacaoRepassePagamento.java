package br.com.escritorio.financeiro.domain;

public enum SituacaoRepassePagamento {

    PENDENTE("Pendente", "text-bg-warning",
            "O repasse ao responsável ainda não foi pago."),
    PAGO("Pago", "text-bg-success",
            "O responsável já recebeu o repasse deste pagamento.");

    private final String rotulo;
    private final String classeBadge;
    private final String descricao;

    SituacaoRepassePagamento(String rotulo, String classeBadge, String descricao) {
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
