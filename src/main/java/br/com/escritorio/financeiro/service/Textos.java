package br.com.escritorio.financeiro.service;

import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

final class Textos {

    private Textos() {
    }

    static String opcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    /**
     * Padrão LIKE "contém", em minúsculas e sem acentos, para comparar com {@code lower(unaccent(coluna))}.
     */
    static String padraoBusca(String termo) {
        String limpo = termo == null ? "" : termo.trim();
        String semAcentos = Normalizer.normalize(limpo, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return "%" + semAcentos.toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
    }
}
