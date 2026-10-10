package br.com.escritorio.financeiro.domain;

public enum FormaCobranca {

    VALOR_FIXO("Valor fixo"),
    PERCENTUAL("Percentual"),
    OUTRA("Outra condição");

    private final String rotulo;

    FormaCobranca(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
