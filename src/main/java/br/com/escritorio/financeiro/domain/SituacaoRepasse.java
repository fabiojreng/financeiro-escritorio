package br.com.escritorio.financeiro.domain;

public enum SituacaoRepasse {

    PENDENTE("Pendente", "text-bg-warning"),
    PAGO("Pago", "text-bg-success"),
    CANCELADO("Cancelado", "text-bg-secondary");

    private final String rotulo;
    private final String classeBadge;

    SituacaoRepasse(String rotulo, String classeBadge) {
        this.rotulo = rotulo;
        this.classeBadge = classeBadge;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getClasseBadge() {
        return classeBadge;
    }
}
