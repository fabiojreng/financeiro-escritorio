package br.com.escritorio.financeiro.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ValorPorExtensoTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0        | zero reais",
            "0.01     | um centavo",
            "1        | um real",
            "1.50     | um real e cinquenta centavos",
            "100      | cem reais",
            "101      | cento e um reais",
            "300      | trezentos reais",
            "1000     | mil reais",
            "1001     | mil e um reais",
            "1100     | mil e cem reais",
            "1500.20  | mil e quinhentos reais e vinte centavos",
            "1580     | mil quinhentos e oitenta reais",
            "2345.67  | dois mil trezentos e quarenta e cinco reais e sessenta e sete centavos",
            "21000    | vinte e um mil reais",
            "1000000  | um milhão de reais",
            "2000000  | dois milhões de reais",
            "1200000  | um milhão e duzentos mil reais",
            "1000015  | um milhão e quinze reais"
    })
    void escreveOValorEmReais(String valor, String esperado) {
        assertThat(ValorPorExtenso.reais(new BigDecimal(valor))).isEqualTo(esperado);
    }
}
