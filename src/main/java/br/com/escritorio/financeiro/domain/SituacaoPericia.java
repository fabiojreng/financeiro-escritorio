package br.com.escritorio.financeiro.domain;

/** O que aconteceu com a perícia depois de anotada. Só a agendada ainda pode mudar. */
public enum SituacaoPericia {

    AGENDADA("Agendada", "selo-andamento",
            "A perícia ainda vai acontecer ou aguarda o INSS marcar a data."),
    REALIZADA("Realizada", "selo-sucesso",
            "O cliente compareceu e a perícia foi feita."),
    FALTOU("Faltou", "selo-alerta",
            "O cliente não compareceu à perícia."),
    REMARCADA("Remarcada", "selo-parcial",
            "A perícia foi remarcada; a nova data está em outro agendamento."),
    CANCELADA("Cancelada", "selo-neutro",
            "A perícia não vai mais acontecer.");

    private final String rotulo;
    private final String classeBadge;
    private final String descricao;

    SituacaoPericia(String rotulo, String classeBadge, String descricao) {
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
