package br.com.escritorio.financeiro.dto;

/** Períodos da agenda de pagamentos, com os textos dos botões que navegam de um período para o outro. */
public enum PeriodoPagamentos {

    DIA("Dia", "Dia anterior", "Hoje", "Próximo dia"),
    SEMANA("Semana (segunda a domingo)", "Semana anterior", "Semana atual", "Próxima semana"),
    MES("Mês", "Mês anterior", "Mês atual", "Próximo mês"),
    PERSONALIZADO("Personalizado (de/até)", null, null, null),
    TODOS("Todo o período", null, null, null);

    private final String rotulo;
    private final String rotuloAnterior;
    private final String rotuloAtual;
    private final String rotuloSeguinte;

    PeriodoPagamentos(String rotulo, String rotuloAnterior, String rotuloAtual, String rotuloSeguinte) {
        this.rotulo = rotulo;
        this.rotuloAnterior = rotuloAnterior;
        this.rotuloAtual = rotuloAtual;
        this.rotuloSeguinte = rotuloSeguinte;
    }

    public boolean isNavegavel() {
        return this != PERSONALIZADO && this != TODOS;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getRotuloAnterior() {
        return rotuloAnterior;
    }

    public String getRotuloAtual() {
        return rotuloAtual;
    }

    public String getRotuloSeguinte() {
        return rotuloSeguinte;
    }
}
