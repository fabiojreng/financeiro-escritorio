package br.com.escritorio.financeiro.domain;

public enum DestaquePrevisao {

    ATRASADA("Atrasada", "text-bg-danger", "bi-exclamation-triangle"),
    HOJE("Prevista para hoje", "text-bg-warning", "bi-calendar-event"),
    PROXIMA("Próximos 7 dias", "text-bg-light border", "bi-clock");

    private final String rotulo;
    private final String classeBadge;
    private final String icone;

    DestaquePrevisao(String rotulo, String classeBadge, String icone) {
        this.rotulo = rotulo;
        this.classeBadge = classeBadge;
        this.icone = icone;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getClasseBadge() {
        return classeBadge;
    }

    public String getIcone() {
        return icone;
    }
}
