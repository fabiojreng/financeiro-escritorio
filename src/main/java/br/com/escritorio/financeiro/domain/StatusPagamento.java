package br.com.escritorio.financeiro.domain;

public enum StatusPagamento {

    AGENDADO("Agendado", "text-bg-primary",
            "Situação inicial: o pagamento foi liberado e tem dia marcado para o cliente receber."),
    PAGO("Pago", "text-bg-success",
            "O cliente já recebeu o valor no banco."),
    PENDENTE("Pendente / não pago", "text-bg-warning",
            "O dia passou e o pagamento não saiu, ou há algum problema a resolver com o banco ou o cliente.");

    private final String rotulo;
    private final String classeBadge;
    private final String descricao;

    StatusPagamento(String rotulo, String classeBadge, String descricao) {
        this.rotulo = rotulo;
        this.classeBadge = classeBadge;
        this.descricao = descricao;
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
