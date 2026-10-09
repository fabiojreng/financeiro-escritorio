package br.com.escritorio.financeiro.domain;

public enum Perfil {

    ADMIN("Administrador"),
    OPERADOR("Operador");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
