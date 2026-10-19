package br.com.escritorio.financeiro.domain;

public enum SituacaoNegociacao {

    ATIVA("Ativa"),
    SUBSTITUIDA("Substituída");

    private final String rotulo;

    SituacaoNegociacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
