package br.com.escritorio.financeiro.domain;

import java.util.Arrays;
import java.util.List;

public enum SituacaoPrevisao {

    PREVISTO("Previsto", "text-bg-primary",
            "Situação inicial: a data prevista ainda não chegou. Quando ela chega, o sistema passa a "
                    + "previsão sozinho para \"Aguardando confirmação\"."),
    AGUARDANDO_CONFIRMACAO("Aguardando confirmação", "text-bg-warning",
            "A data chegou ou passou e falta conferir com o INSS ou com o cliente se o valor foi liberado. "
                    + "Entra automaticamente quando a data chega e aparece como atrasada até ser resolvida. "
                    + "Se a data for remarcada para o futuro, volta sozinha para \"Previsto\"."),
    LIBERADO("Liberado pelo INSS", "text-bg-success",
            "O INSS liberou o benefício. Só é marcado pela ação \"Confirmar liberação\", "
                    + "informando a data e o valor reais."),
    ADIADO("Adiado / não liberado", "text-bg-info",
            "O INSS não liberou na data prevista. Se houver uma nova data, edite a previsão. "
                    + "Não entra no alerta de atrasadas."),
    CANCELADO("Cancelado", "text-bg-secondary",
            "A previsão não vale mais (por exemplo, o pedido foi negado). "
                    + "Continua registrada, mas sai dos totais estimados.");

    /** Situações que ainda esperam a confirmação de liberação. */
    public static final List<SituacaoPrevisao> PENDENTES = List.of(PREVISTO, AGUARDANDO_CONFIRMACAO);

    private final String rotulo;
    private final String classeBadge;
    private final String descricao;

    SituacaoPrevisao(String rotulo, String classeBadge, String descricao) {
        this.rotulo = rotulo;
        this.classeBadge = classeBadge;
        this.descricao = descricao;
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

    public String getDescricao() {
        return descricao;
    }
}
