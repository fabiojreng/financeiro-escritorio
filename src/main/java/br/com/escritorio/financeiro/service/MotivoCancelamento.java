package br.com.escritorio.financeiro.service;

/** Motivo obrigatório para cancelar lançamentos financeiros. */
final class MotivoCancelamento {

    static final int TAMANHO_MINIMO = 5;
    static final int TAMANHO_MAXIMO = 500;

    private MotivoCancelamento() {
    }

    static String validar(String motivo) {
        String limpo = Textos.opcional(motivo);
        if (limpo == null || limpo.length() < TAMANHO_MINIMO) {
            throw new RegraNegocioException("motivo",
                    "Informe o motivo do cancelamento (pelo menos " + TAMANHO_MINIMO + " caracteres).");
        }
        if (limpo.length() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("motivo",
                    "O motivo pode ter no máximo " + TAMANHO_MAXIMO + " caracteres.");
        }
        return limpo;
    }
}
