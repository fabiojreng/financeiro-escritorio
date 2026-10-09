package br.com.escritorio.financeiro.service;

/**
 * Violação de regra de negócio com mensagem pronta para o usuário.
 * Quando {@code campo} é informado, a tela exibe a mensagem junto ao campo do formulário.
 */
public class RegraNegocioException extends RuntimeException {

    private final String campo;

    public RegraNegocioException(String mensagem) {
        this(null, mensagem);
    }

    public RegraNegocioException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
