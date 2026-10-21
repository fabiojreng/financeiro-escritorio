package br.com.escritorio.financeiro.domain;

public enum AcaoAuditoria {

    CRIACAO("Cadastro"),
    DUPLICACAO("Duplicação"),
    ALTERACAO("Alteração"),
    ATIVACAO("Ativação"),
    DESATIVACAO("Desativação"),
    CONFIRMACAO("Confirmação"),
    CANCELAMENTO("Cancelamento"),
    EXPORTACAO("Exportação");

    private final String rotulo;

    AcaoAuditoria(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
