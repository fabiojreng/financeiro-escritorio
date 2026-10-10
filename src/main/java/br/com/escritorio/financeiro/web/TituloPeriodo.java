package br.com.escritorio.financeiro.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Título legível do período filtrado, como "Outubro de 2026" ou "De 01/10/2026 a 15/10/2026". */
final class TituloPeriodo {

    private static final Locale BRASIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", BRASIL);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TituloPeriodo() {
    }

    static String mes(YearMonth mes) {
        String texto = MES_ANO.format(mes);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    static String periodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null) {
            return "Até " + DATA.format(fim);
        }
        if (fim == null) {
            return "A partir de " + DATA.format(inicio);
        }
        return "De " + DATA.format(inicio) + " a " + DATA.format(fim);
    }
}
