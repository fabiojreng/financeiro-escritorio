package br.com.escritorio.financeiro.domain;

public enum SituacaoLancamentoFuncionario {

    ATIVO("Ativo", "selo-sucesso"),
    CANCELADO("Cancelado", "selo-neutro");

    private final String rotulo;
    private final String classeBadge;

    SituacaoLancamentoFuncionario(String rotulo, String classeBadge) {
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
