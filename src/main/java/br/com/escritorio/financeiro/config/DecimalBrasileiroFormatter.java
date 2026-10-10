package br.com.escritorio.financeiro.config;

import org.springframework.format.Formatter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Converte valores digitados no padrão brasileiro ("1.234,56", "R$ 1234,56").
 * Sem vírgula, o ponto só é separador de milhar quando separa grupos de três dígitos ("1.234");
 * nos demais casos é decimal ("1234.56"), para não multiplicar por cem um valor digitado com ponto.
 */
public class DecimalBrasileiroFormatter implements Formatter<BigDecimal> {

    private static final Locale BRASIL = Locale.of("pt", "BR");
    private static final Pattern CARACTERES_VALIDOS = Pattern.compile("-?[0-9.,]+");
    private static final Pattern SO_MILHARES = Pattern.compile("-?\\d{1,3}(\\.\\d{3})+");

    @Override
    public BigDecimal parse(String texto, Locale locale) throws ParseException {
        String limpo = texto.replace("R$", "").replaceAll("[\\s\\u00A0]", "");
        if (!CARACTERES_VALIDOS.matcher(limpo).matches()) {
            throw new ParseException(texto, 0);
        }
        String normalizado;
        int virgula = limpo.indexOf(',');
        if (virgula >= 0) {
            if (virgula != limpo.lastIndexOf(',') || limpo.indexOf('.', virgula) >= 0) {
                throw new ParseException(texto, virgula);
            }
            normalizado = limpo.replace(".", "").replace(',', '.');
        } else if (SO_MILHARES.matcher(limpo).matches()) {
            normalizado = limpo.replace(".", "");
        } else {
            normalizado = limpo;
        }
        try {
            return new BigDecimal(normalizado);
        } catch (NumberFormatException e) {
            throw new ParseException(texto, 0);
        }
    }

    @Override
    public String print(BigDecimal valor, Locale locale) {
        NumberFormat formato = NumberFormat.getNumberInstance(BRASIL);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return formato.format(valor);
    }
}
