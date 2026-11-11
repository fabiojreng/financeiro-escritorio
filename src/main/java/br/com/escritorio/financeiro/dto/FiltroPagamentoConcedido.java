package br.com.escritorio.financeiro.dto;

/** Se o cliente do benefício concedido já tem pagamento daquele mesmo benefício na agenda. */
public enum FiltroPagamentoConcedido {

    TODOS("Todos", "TODOS"),
    COM_PAGAMENTO("Com pagamento", "COM"),
    SEM_PAGAMENTO("Sem pagamento", "SEM");

    private final String rotulo;
    private final String consulta;

    FiltroPagamentoConcedido(String rotulo, String consulta) {
        this.rotulo = rotulo;
        this.consulta = consulta;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Valor usado na consulta: TODOS, COM ou SEM. */
    public String getConsulta() {
        return consulta;
    }
}
