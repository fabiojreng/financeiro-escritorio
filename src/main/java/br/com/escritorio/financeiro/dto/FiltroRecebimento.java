package br.com.escritorio.financeiro.dto;

public enum FiltroRecebimento {

    CONFIRMADOS("Confirmados"),
    CANCELADOS("Cancelados");

    private final String rotulo;

    FiltroRecebimento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
