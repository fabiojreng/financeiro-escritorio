package br.com.escritorio.financeiro.domain;

public enum TipoPagamento {

    PRIMEIRO_PAGAMENTO("Primeiro pagamento",
            "Primeiro valor liberado depois que o benefício é concedido."),
    RETROATIVO("Retroativo / atrasados",
            "Valores acumulados desde o início do direito, pagos de uma vez."),
    COMPETENCIA_MENSAL("Competência mensal",
            "Pagamento regular de um mês do benefício."),
    OUTRO("Outro",
            "Qualquer outro tipo de pagamento. Explique nas observações.");

    private final String rotulo;
    private final String descricao;

    TipoPagamento(String rotulo, String descricao) {
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
