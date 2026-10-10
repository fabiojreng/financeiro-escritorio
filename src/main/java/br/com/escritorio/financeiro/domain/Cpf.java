package br.com.escritorio.financeiro.domain;

/**
 * Normalização, validação dos dígitos verificadores e formatação de CPF.
 * O CPF é armazenado apenas com os 11 dígitos.
 */
public final class Cpf {

    private Cpf() {
    }

    /**
     * Remove tudo que não for dígito. Retorna {@code null} para valores vazios.
     */
    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    public static boolean valido(String digitos) {
        if (digitos == null || !digitos.matches("\\d{11}") || digitos.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(digitos, 9) == digitos.charAt(9) - '0'
                && digitoVerificador(digitos, 10) == digitos.charAt(10) - '0';
    }

    public static String formatar(String digitos) {
        if (digitos == null || digitos.length() != 11) {
            return digitos;
        }
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }

    private static int digitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
