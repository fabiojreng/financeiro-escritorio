package br.com.escritorio.financeiro.domain;

public enum FormaPagamento {

    DINHEIRO("Dinheiro"),
    PIX("PIX"),
    TRANSFERENCIA("Transferência"),
    BOLETO("Boleto"),
    OUTRO("Outro");

    private final String rotulo;

    FormaPagamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
