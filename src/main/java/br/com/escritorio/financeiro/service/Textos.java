package br.com.escritorio.financeiro.service;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class Textos {

    private static final Locale BRASIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Textos() {
    }

    static String moeda(BigDecimal valor) {
        return valor == null ? "não informado"
                : NumberFormat.getCurrencyInstance(BRASIL).format(valor).replace('\u00A0', ' ');
    }

    static String data(LocalDate data) {
        return data == null ? "não informada" : DATA.format(data);
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
