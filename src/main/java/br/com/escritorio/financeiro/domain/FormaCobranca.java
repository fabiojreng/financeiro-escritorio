package br.com.escritorio.financeiro.domain;

public enum FormaCobranca {

    VALOR_FIXO("Valor fixo",
            "Valor fechado em reais. O valor total contratado é obrigatório."),
    PERCENTUAL("Percentual",
            "Percentual sobre uma base combinada, como os atrasados. Informe o percentual e a base; "
                    + "o valor em reais pode ficar em branco até ser conhecido. O sistema não calcula esse valor."),
    OUTRA("Outra condição",
            "Qualquer outro combinado. Descreva nas condições; o valor pode ficar em branco até ser conhecido.");

    private final String rotulo;
    private final String descricao;

    FormaCobranca(String rotulo, String descricao) {
        this.rotulo = rotulo;
        this.descricao = descricao;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getDescricao() {
        return descricao;
    }
}
