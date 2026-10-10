package br.com.escritorio.financeiro.domain;

public enum TipoPagamento {

    PRIMEIRO_PAGAMENTO("Primeiro pagamento"),
    RETROATIVO("Retroativo / atrasados"),
    COMPETENCIA_MENSAL("Competência mensal"),
    OUTRO("Outro");

    private final String rotulo;

    TipoPagamento(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
