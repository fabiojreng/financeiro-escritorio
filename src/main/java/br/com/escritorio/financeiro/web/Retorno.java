package br.com.escritorio.financeiro.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Endereço de retorno após uma ação. Só aceita caminhos internos da aplicação, para que o
 * parâmetro não possa redirecionar o usuário a outro site.
 */
final class Retorno {

    private Retorno() {
    }

    static String seguro(String voltar, String padrao) {
        if (voltar == null || !voltar.startsWith("/") || voltar.startsWith("//") || voltar.contains("\\")
                || voltar.chars().anyMatch(Character::isISOControl)) {
            return padrao;
        }
        return voltar;
    }

    static String urlAtual(HttpServletRequest request) {
        String consulta = request.getQueryString();
        return request.getRequestURI() + (consulta == null ? "" : "?" + consulta);
    }
}
