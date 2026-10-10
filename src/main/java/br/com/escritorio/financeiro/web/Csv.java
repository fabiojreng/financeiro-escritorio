package br.com.escritorio.financeiro.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Planilha CSV para abrir direto no Excel em português: UTF-8 com BOM, separador ";", vírgula
 * decimal sem separador de milhar e datas dd/MM/aaaa. Textos que começam com caracteres de
 * fórmula recebem um apóstrofo para não serem executados pela planilha.
 */
final class Csv {

    private static final char SEPARADOR = ';';
    private static final String FIM_DE_LINHA = "\r\n";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StringBuilder conteudo = new StringBuilder("\uFEFF");
    private boolean inicioDaLinha = true;
    private int linhas;

    Csv(String... cabecalho) {
        for (String coluna : cabecalho) {
            texto(coluna);
        }
        conteudo.append(FIM_DE_LINHA);
        inicioDaLinha = true;
    }

    Csv texto(String valor) {
        return celula(protegerFormula(valor));
    }

    Csv valor(BigDecimal valor) {
        return celula(valor == null ? "" : valor.setScale(2, RoundingMode.HALF_EVEN).toPlainString().replace('.', ','));
    }

    Csv data(LocalDate data) {
        return celula(data == null ? "" : DATA.format(data));
    }

    Csv numero(long numero) {
        return celula(Long.toString(numero));
    }

    void fimDaLinha() {
        conteudo.append(FIM_DE_LINHA);
        inicioDaLinha = true;
        linhas++;
    }

    int getLinhas() {
        return linhas;
    }

    byte[] bytes() {
        return conteudo.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Csv celula(String valor) {
        if (!inicioDaLinha) {
            conteudo.append(SEPARADOR);
        }
        conteudo.append(escapar(valor));
        inicioDaLinha = false;
        return this;
    }

    private static String escapar(String valor) {
        if (valor.indexOf(SEPARADOR) >= 0 || valor.indexOf('"') >= 0 || valor.indexOf('\n') >= 0
                || valor.indexOf('\r') >= 0) {
            return '"' + valor.replace("\"", "\"\"") + '"';
        }
        return valor;
    }

    private static String protegerFormula(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        return "=+-@\t\r".indexOf(valor.charAt(0)) >= 0 ? "'" + valor : valor;
    }
}
