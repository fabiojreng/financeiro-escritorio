package br.com.escritorio.financeiro.domain;

public enum Perfil {

    ADMIN("Administrador",
            "Faz tudo o que o operador faz e também gerencia usuários e vê o histórico de operações."),
    OPERADOR("Operador",
            "Faz a rotina do escritório: cadastros, agenda de pagamentos, honorários, repasses e relatórios.");

    private final String rotulo;
    private final String descricao;

    Perfil(String rotulo, String descricao) {
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
