package br.com.escritorio.financeiro.domain;

public enum SituacaoAtendimento {

    ABERTO("Aberto", "text-bg-primary"),
    EM_ANDAMENTO("Em andamento", "text-bg-info"),
    CONCLUIDO("Concluído", "text-bg-success"),
    CANCELADO("Cancelado", "text-bg-secondary");

    private final String rotulo;
    private final String classeBadge;

    SituacaoAtendimento(String rotulo, String classeBadge) {
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
