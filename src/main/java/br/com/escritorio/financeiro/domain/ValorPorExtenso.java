package br.com.escritorio.financeiro.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Escreve valores em reais por extenso, como nos recibos ("mil e quinhentos reais e vinte centavos"). */
public final class ValorPorExtenso {

    private static final String[] UNIDADES = {"zero", "um", "dois", "três", "quatro", "cinco", "seis", "sete",
            "oito", "nove", "dez", "onze", "doze", "treze", "quatorze", "quinze", "dezesseis", "dezessete",
            "dezoito", "dezenove"};
    private static final String[] DEZENAS = {"", "", "vinte", "trinta", "quarenta", "cinquenta", "sessenta",
            "setenta", "oitenta", "noventa"};
    private static final String[] CENTENAS = {"", "cento", "duzentos", "trezentos", "quatrocentos", "quinhentos",
            "seiscentos", "setecentos", "oitocentos", "novecentos"};
    private static final String[][] ESCALAS = {{"", ""}, {"mil", "mil"}, {"milhão", "milhões"},
            {"bilhão", "bilhões"}};
    private static final long LIMITE = 1_000_000_000_000L;

    private ValorPorExtenso() {
    }

    public static String reais(BigDecimal valor) {
        BigDecimal positivo = valor.abs().setScale(2, RoundingMode.HALF_UP);
        long inteiro = positivo.longValue();
        int centavos = positivo.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        if (inteiro >= LIMITE) {
            throw new IllegalArgumentException("Valor grande demais para escrever por extenso.");
        }

        List<String> partes = new ArrayList<>();
        if (inteiro > 0) {
            String moeda = inteiro == 1 ? "real" : "reais";
            boolean milhaoRedondo = inteiro >= 1_000_000 && inteiro % 1_000_000 == 0;
            partes.add(numero(inteiro) + (milhaoRedondo ? " de " : " ") + moeda);
        }
        if (centavos > 0) {
            partes.add(numero(centavos) + (centavos == 1 ? " centavo" : " centavos"));
        }
        if (partes.isEmpty()) {
            return "zero reais";
        }
        return String.join(" e ", partes);
    }

    static String numero(long valor) {
        if (valor == 0) {
            return UNIDADES[0];
        }
        List<Integer> grupos = new ArrayList<>();
        for (long resto = valor; resto > 0; resto /= 1000) {
            grupos.add((int) (resto % 1000));
        }

        List<String> textos = new ArrayList<>();
        int ultimoGrupo = -1;
        for (int escala = grupos.size() - 1; escala >= 0; escala--) {
            int grupo = grupos.get(escala);
            if (grupo == 0) {
                continue;
            }
            String texto;
            if (escala == 1 && grupo == 1) {
                texto = "mil";
            } else if (escala == 0) {
                texto = centena(grupo);
            } else {
                texto = centena(grupo) + " " + ESCALAS[escala][grupo == 1 ? 0 : 1];
            }
            textos.add(texto);
            ultimoGrupo = grupo;
        }

        StringBuilder extenso = new StringBuilder(textos.get(0));
        for (int i = 1; i < textos.size(); i++) {
            boolean ultimo = i == textos.size() - 1;
            boolean usaE = ultimo && (ultimoGrupo < 100 || ultimoGrupo % 100 == 0);
            extenso.append(usaE ? " e " : " ").append(textos.get(i));
        }
        return extenso.toString();
    }

    private static String centena(int valor) {
        if (valor == 100) {
            return "cem";
        }
        int centena = valor / 100;
        int dezena = valor % 100;
        List<String> partes = new ArrayList<>();
        if (centena > 0) {
            partes.add(CENTENAS[centena]);
        }
        if (dezena > 0) {
            if (dezena < 20) {
                partes.add(UNIDADES[dezena]);
            } else {
                String texto = DEZENAS[dezena / 10];
                if (dezena % 10 > 0) {
                    texto += " e " + UNIDADES[dezena % 10];
                }
                partes.add(texto);
            }
        }
        return String.join(" e ", partes);
    }
}
