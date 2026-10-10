package br.com.escritorio.financeiro.dto;

public enum FiltroSaldo {

    PENDENTES("Com saldo pendente"),
    QUITADOS("Quitados"),
    SEM_VALOR("Valor ainda não definido"),
    TODOS("Todos");

    private final String rotulo;

    FiltroSaldo(String rotulo) {
        this.rotulo = rotulo;
    }

    public boolean aceita(AcordoComTotais linha) {
        return switch (this) {
            case PENDENTES -> linha.saldo() != null && linha.saldo().signum() > 0;
            case QUITADOS -> linha.isQuitado();
            case SEM_VALOR -> linha.saldo() == null;
            case TODOS -> true;
        };
    }

    public String getRotulo() {
        return rotulo;
    }
}
