package br.com.escritorio.financeiro.web;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CsvTest {

    @Test
    void geraUtf8ComBomSeparadorPontoEVirgulaEFimDeLinhaDoWindows() {
        Csv csv = new Csv("Data", "Cliente", "Valor (R$)");
        csv.data(LocalDate.of(2026, 10, 5)).texto("Maria José").valor(new BigDecimal("1234.5")).fimDaLinha();

        byte[] bytes = csv.bytes();

        assertThat(bytes).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
        assertThat(new String(bytes, StandardCharsets.UTF_8))
                .isEqualTo("\uFEFFData;Cliente;Valor (R$)\r\n05/10/2026;Maria José;1234,50\r\n");
        assertThat(csv.getLinhas()).isEqualTo(1);
    }

    @Test
    void valoresVaziosFicamEmBranco() {
        Csv csv = new Csv("A", "B", "C");
        csv.texto(null).valor(null).data(null).fimDaLinha();

        assertThat(texto(csv)).endsWith("\r\n;;\r\n");
    }

    @Test
    void textoComSeparadorAspasOuQuebraDeLinhaVaiEntreAspas() {
        Csv csv = new Csv("Observação");
        csv.texto("Pagou; em \"duas\" vezes\nconforme combinado").fimDaLinha();

        assertThat(texto(csv)).contains("\"Pagou; em \"\"duas\"\" vezes\nconforme combinado\"");
    }

    @Test
    void textoQueComecaComoFormulaNaoEExecutadoPelaPlanilha() {
        Csv csv = new Csv("Nome");
        csv.texto("=HYPERLINK(\"http://exemplo\")").fimDaLinha();
        csv.texto("+55 11 9999").fimDaLinha();
        csv.texto("@soma").fimDaLinha();

        assertThat(texto(csv))
                .contains("'=HYPERLINK")
                .contains("'+55 11 9999")
                .contains("'@soma");
    }

    private static String texto(Csv csv) {
        return new String(csv.bytes(), StandardCharsets.UTF_8);
    }
}
