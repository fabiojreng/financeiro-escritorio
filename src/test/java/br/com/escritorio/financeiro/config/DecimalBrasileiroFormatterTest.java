package br.com.escritorio.financeiro.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecimalBrasileiroFormatterTest {

    private final DecimalBrasileiroFormatter formatter = new DecimalBrasileiroFormatter();
    private final Locale brasil = Locale.of("pt", "BR");

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "1.234,56   | 1234.56",
            "1234,56    | 1234.56",
            "R$ 1.234,5 | 1234.5",
            "1234.56    | 1234.56",
            "1.234      | 1234",
            "1.234.567  | 1234567",
            "0,99       | 0.99",
            "1500       | 1500",
            "-10,00     | -10.00"
    })
    void entendeValoresNoPadraoBrasileiro(String texto, String esperado) throws ParseException {
        assertThat(formatter.parse(texto, brasil)).isEqualByComparingTo(new BigDecimal(esperado));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1,2,3", "1,234.56", "1.2.3", "12a"})
    void rejeitaTextoQueNaoEValor(String texto) {
        assertThatThrownBy(() -> formatter.parse(texto, brasil)).isInstanceOf(ParseException.class);
    }

    @Test
    void exibeComVirgulaEDuasCasas() {
        assertThat(formatter.print(new BigDecimal("1234.5"), brasil)).isEqualTo("1.234,50");
    }
}
