package br.com.escritorio.financeiro.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Título legível do período filtrado, como "Outubro de 2026" ou "De 01/10/2026 a 15/10/2026". */
final class TituloPeriodo {

    private static final Locale BRASIL = Locale.of("pt", "BR");
    private static final DateTimeFormatter MES_ANO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", BRASIL);
    private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", BRASIL);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TituloPeriodo() {
    }

    static String mes(YearMonth mes) {
        return maiuscula(MES_ANO.format(mes));
    }

    static String dia(LocalDate dia) {
        return maiuscula(DIA_SEMANA.format(dia));
    }

    static String semana(LocalDate inicio, LocalDate fim) {
        return "Semana de " + DATA.format(inicio) + " a " + DATA.format(fim);
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

    private static String maiuscula(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
