package br.com.escritorio.financeiro.domain;

/** Se o cliente já recebeu o benefício no banco ou ainda está aguardando ("Cliente recebeu?" na tela). */
public enum StatusPagamento {

    AGENDADO("Agendado", "selo-andamento",
            "O cliente ainda está aguardando receber o benefício no banco."),
    RECEBIDO("Recebeu", "selo-sucesso",
            "O cliente já recebeu o benefício no banco.");

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
