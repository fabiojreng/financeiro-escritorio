package br.com.escritorio.financeiro.domain;

public enum SituacaoParcela {

    ABERTA("Aberta", "selo-andamento"),
    PAGA("Paga", "selo-sucesso"),
    CANCELADA("Cancelada", "selo-neutro");

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
