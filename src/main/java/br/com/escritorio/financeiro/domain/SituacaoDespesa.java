package br.com.escritorio.financeiro.domain;

public enum SituacaoDespesa {

    ATIVA("Ativa", "selo-sucesso"),
    CANCELADA("Cancelada", "selo-neutro");

    private final String rotulo;
    private final String classeBadge;

    SituacaoDespesa(String rotulo, String classeBadge) {
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
