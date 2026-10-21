package br.com.escritorio.financeiro.domain;

import java.util.Arrays;
import java.util.List;

/** Área do sistema usada para agrupar as permissões na tela de perfis. */
public enum ModuloPermissao {

    CLIENTES("Clientes"),
    CADASTROS("Cadastros"),
    AGENDA("Agenda de pagamentos"),
    HONORARIOS("Honorários"),
    REPASSES("Repasses"),
    DESPESAS("Despesas do escritório"),
    RELATORIOS("Relatórios"),
    ADMINISTRACAO("Administração");

    private final String rotulo;

    ModuloPermissao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public List<Permissao> getPermissoes() {
        return Arrays.stream(Permissao.values())
                .filter(permissao -> permissao.getModulo() == this)
                .toList();
    }
}
