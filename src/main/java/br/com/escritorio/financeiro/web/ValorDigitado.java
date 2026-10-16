package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.config.DecimalBrasileiroFormatter;
import br.com.escritorio.financeiro.service.RegraNegocioException;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

/** Valor em reais digitado direto na linha de uma lista, onde não há formulário com mensagens por campo. */
final class ValorDigitado {

    private static final DecimalBrasileiroFormatter FORMATO = new DecimalBrasileiroFormatter();
    private static final Locale BRASIL = Locale.of("pt", "BR");

    private ValorDigitado() {
    }

    static BigDecimal opcional(String texto, String nome) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return FORMATO.parse(texto.trim(), BRASIL);
        } catch (ParseException e) {
            throw new RegraNegocioException("valor", nome + " não é um valor válido. Use, por exemplo, 1.234,56.");
        }
    }
}
