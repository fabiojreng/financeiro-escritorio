package br.com.escritorio.financeiro.domain;

public enum SituacaoEmprestimo {

    EM_ABERTO("Em aberto", "selo-pendente"),
    QUITADO("Quitado", "selo-sucesso"),
    CANCELADO("Cancelado", "selo-neutro");

    private final String rotulo;
    private final String classeBadge;

    SituacaoEmprestimo(String rotulo, String classeBadge) {
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
