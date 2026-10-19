package br.com.escritorio.financeiro.domain;

public enum SituacaoParcela {

    ABERTA("Aberta", "text-bg-primary"),
    PAGA("Paga", "text-bg-success"),
    CANCELADA("Cancelada", "text-bg-secondary");

    private final String rotulo;
    private final String classeBadge;

    SituacaoParcela(String rotulo, String classeBadge) {
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
