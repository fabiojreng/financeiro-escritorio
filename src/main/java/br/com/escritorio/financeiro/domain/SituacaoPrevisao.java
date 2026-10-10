package br.com.escritorio.financeiro.domain;

import java.util.Arrays;
import java.util.List;

public enum SituacaoPrevisao {

    PREVISTO("Previsto", "text-bg-primary"),
    AGUARDANDO_CONFIRMACAO("Aguardando confirmação", "text-bg-warning"),
    LIBERADO("Liberado pelo INSS", "text-bg-success"),
    ADIADO("Adiado / não liberado", "text-bg-info"),
    CANCELADO("Cancelado", "text-bg-secondary");

    /** Situações que ainda esperam a confirmação de liberação. */
    public static final List<SituacaoPrevisao> PENDENTES = List.of(PREVISTO, AGUARDANDO_CONFIRMACAO);

    private final String rotulo;
    private final String classeBadge;

    SituacaoPrevisao(String rotulo, String classeBadge) {
        this.rotulo = rotulo;
        this.classeBadge = classeBadge;
    }

    /** Situações que o usuário escolhe diretamente; {@link #LIBERADO} só entra pela confirmação. */
    public static List<SituacaoPrevisao> editaveis() {
        return Arrays.stream(values()).filter(s -> s != LIBERADO).toList();
    }

    public boolean isPendente() {
        return PENDENTES.contains(this);
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getClasseBadge() {
        return classeBadge;
    }
}
