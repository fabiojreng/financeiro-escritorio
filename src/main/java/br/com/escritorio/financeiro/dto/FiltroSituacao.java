package br.com.escritorio.financeiro.dto;

import java.util.List;

public enum FiltroSituacao {

    ATIVOS("Ativos", List.of(true)),
    INATIVOS("Inativos", List.of(false)),
    TODOS("Todos", List.of(true, false));

    private final String rotulo;
    private final List<Boolean> situacoes;

    FiltroSituacao(String rotulo, List<Boolean> situacoes) {
        this.rotulo = rotulo;
        this.situacoes = situacoes;
    }

    public String getRotulo() {
        return rotulo;
    }

    public List<Boolean> getSituacoes() {
        return situacoes;
    }
}
