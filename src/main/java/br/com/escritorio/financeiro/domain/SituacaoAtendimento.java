package br.com.escritorio.financeiro.domain;

public enum SituacaoAtendimento {

    ABERTO("Aberto", "text-bg-primary",
            "Atendimento recém-iniciado, ainda sem pedido ou processo em curso."),
    EM_ANDAMENTO("Em andamento", "text-bg-info",
            "O pedido no INSS ou o processo está em curso."),
    CONCLUIDO("Concluído", "text-bg-success",
            "O serviço terminou. Ainda é possível lançar previsões, honorários e repasses."),
    CANCELADO("Cancelado", "text-bg-secondary",
            "O serviço foi interrompido. Bloqueia novas previsões, acordos de honorários e repasses; "
                    + "o que já foi lançado continua registrado.");

    private final String rotulo;
    private final String classeBadge;
    private final String descricao;

    SituacaoAtendimento(String rotulo, String classeBadge, String descricao) {
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
