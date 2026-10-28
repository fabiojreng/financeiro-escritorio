package br.com.escritorio.financeiro.web;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableStyleInfo;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Leiaute de impressão comum às planilhas do sistema, no padrão do modelo Domínio: tabela a partir de A1,
 * A4 paisagem, ajuste à largura da página, cabeçalho repetido em cada folha, conteúdo centralizado em fonte
 * maior e texto longo quebrando linha dentro da coluna.
 */
final class LeiauteImpressaoExcel {

    static final short TAMANHO_FONTE = 13;
    static final int LINHA_CABECALHO = 0;
    static final float ALTURA_LINHA = 21f;
    static final float ALTURA_CABECALHO = 36f;
    static final DecimalFormat MOEDA =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));

    /** Quanto cada letra na fonte maior ocupa em relação à fonte padrão, que é a unidade da largura da coluna. */
    private static final double FATOR_FONTE = 1.2;
    /** Espaço do botão de filtro da tabela, que fica sobre o cabeçalho. */
    private static final int FOLGA_FILTRO = 3;

    private LeiauteImpressaoExcel() {
    }

    static void escreverCabecalho(XSSFSheet aba, List<String> titulos, CellStyle estilo) {
        Row cabecalho = aba.createRow(LINHA_CABECALHO);
        cabecalho.setHeightInPoints(ALTURA_CABECALHO);
        for (int i = 0; i < titulos.size(); i++) {
            Cell celula = cabecalho.createCell(i);
            celula.setCellValue(titulos.get(i));
            celula.setCellStyle(estilo);
        }
    }

    /** Alarga a coluna para caber o texto, se ele for maior que o já visto. */
    static void ajustar(int[] larguras, int coluna, String texto) {
        if (texto != null) {
            larguras[coluna] = Math.max(larguras[coluna], (int) Math.ceil(texto.length() * FATOR_FONTE) + 2);
        }
    }

    /**
     * Limita cada largura à máxima (o texto maior quebra linha), mas nunca abaixo do cabeçalho, que em negrito
     * e maiúsculas ocupa cerca de 20% a mais por letra.
     */
    static int[] limitar(int[] larguras, List<String> cabecalho, int[] maximas) {
        return limitar(larguras, cabecalho, maximas, false);
    }

    /**
     * Como {@link #limitar(int[], List, int[])}, mas o cabeçalho pode quebrar linha: a largura mínima é a da
     * maior palavra dele, e não a do título inteiro.
     */
    static int[] limitarQuebrandoCabecalho(int[] larguras, List<String> cabecalho, int[] maximas) {
        return limitar(larguras, cabecalho, maximas, true);
    }

    private static int[] limitar(int[] larguras, List<String> cabecalho, int[] maximas, boolean quebraCabecalho) {
        for (int i = 0; i < larguras.length; i++) {
            int letras = cabecalho.get(i).length();
            if (quebraCabecalho) {
                letras = 0;
                for (String palavra : cabecalho.get(i).trim().split("\\s+")) {
                    letras = Math.max(letras, palavra.length());
                }
            }
            int minima = (int) Math.ceil(letras * 1.2 * FATOR_FONTE) + FOLGA_FILTRO;
            larguras[i] = Math.max(minima, Math.min(larguras[i], maximas[i]));
        }
        return larguras;
    }

    /** Aumenta a linha do cabeçalho quando algum título precisa quebrar em mais de duas linhas. */
    static void ajustarAlturaCabecalho(XSSFSheet aba, int[] larguras, List<String> cabecalho) {
        int linhas = 1;
        for (int i = 0; i < larguras.length; i++) {
            int caberPorLinha = Math.max(1, (int) ((larguras[i] - FOLGA_FILTRO) / (1.2 * FATOR_FONTE)));
            linhas = Math.max(linhas, (cabecalho.get(i).length() + caberPorLinha - 1) / caberPorLinha);
        }
        aba.getRow(LINHA_CABECALHO).setHeightInPoints(Math.max(ALTURA_CABECALHO, ALTURA_LINHA * linhas));
    }

    static void aplicarLarguras(XSSFSheet aba, int[] larguras) {
        for (int i = 0; i < larguras.length; i++) {
            aba.setColumnWidth(i, larguras[i] * 256);
        }
    }

    /**
     * Linhas que o texto mais longo da linha ocupa quando quebrado na largura da coluna.
     *
     * @param textos um por coluna, na ordem das colunas; {@code null} nas que não quebram
     */
    static int linhasNecessarias(int[] larguras, String... textos) {
        int linhas = 1;
        for (int i = 0; i < textos.length; i++) {
            if (textos[i] != null) {
                int caberPorLinha = Math.max(1, (int) ((larguras[i] - 2) / FATOR_FONTE));
                int linhasDoTexto = 0;
                for (String parte : textos[i].split("\n")) {
                    linhasDoTexto += Math.max(1, (parte.length() + caberPorLinha - 1) / caberPorLinha);
                }
                linhas = Math.max(linhas, linhasDoTexto);
            }
        }
        return linhas;
    }

    static void configurarImpressao(XSSFWorkbook planilha, XSSFSheet aba, String tituloPagina, int colunas,
                                    int ultimaLinha) {
        PrintSetup impressao = aba.getPrintSetup();
        impressao.setLandscape(true);
        impressao.setPaperSize(PrintSetup.A4_PAPERSIZE);
        impressao.setFitWidth((short) 1);
        impressao.setFitHeight((short) 0);

        aba.setFitToPage(true);
        aba.setAutobreaks(true);
        aba.setHorizontallyCenter(true);

        // Margens do modelo Domínio (polegadas).
        aba.setMargin(XSSFSheet.LeftMargin, 0.24);
        aba.setMargin(XSSFSheet.RightMargin, 0.24);
        aba.setMargin(XSSFSheet.TopMargin, 0.75);
        aba.setMargin(XSSFSheet.BottomMargin, 0.75);
        aba.setMargin(XSSFSheet.HeaderMargin, 0.31);
        aba.setMargin(XSSFSheet.FooterMargin, 0.31);

        aba.getHeader().setCenter(tituloPagina);
        aba.getFooter().setRight("Página &P de &N");

        aba.setRepeatingRows(CellRangeAddress.valueOf("1:1"));
        planilha.setPrintArea(planilha.getSheetIndex(aba), 0, colunas - 1, 0, ultimaLinha);
    }

    static void criarTabela(XSSFSheet aba, String nome, int colunas, int ultimaLinha) {
        AreaReference area = new AreaReference(
                new CellReference(LINHA_CABECALHO, 0),
                new CellReference(ultimaLinha, colunas - 1),
                SpreadsheetVersion.EXCEL2007);
        XSSFTable tabela = aba.createTable(area);
        tabela.setName(nome);
        tabela.setDisplayName(nome);
        tabela.getCTTable().addNewAutoFilter().setRef(area.formatAsString());
        tabela.getCTTable().addNewTableStyleInfo();
        XSSFTableStyleInfo estilo = (XSSFTableStyleInfo) tabela.getStyle();
        estilo.setName("TableStyleMedium3");
        estilo.setShowRowStripes(true);
        estilo.setShowColumnStripes(false);
        estilo.setFirstColumn(false);
        estilo.setLastColumn(false);
        tabela.updateHeaders();
    }

    static Cell texto(Row row, int coluna, String valor, CellStyle estilo) {
        Cell celula = row.createCell(coluna);
        if (valor != null && !valor.isBlank()) {
            celula.setCellValue(valor);
        }
        celula.setCellStyle(estilo);
        return celula;
    }

    static Cell data(Row row, int coluna, LocalDate valor, CellStyle estilo) {
        Cell celula = row.createCell(coluna);
        if (valor != null) {
            celula.setCellValue(valor);
        }
        celula.setCellStyle(estilo);
        return celula;
    }

    static Cell valor(Row row, int coluna, BigDecimal valor, CellStyle estilo) {
        Cell celula = row.createCell(coluna);
        if (valor != null) {
            celula.setCellValue(valor.doubleValue());
        }
        celula.setCellStyle(estilo);
        return celula;
    }

    /** Estilos centralizados, com bordas e quebra de linha, criados uma vez por planilha. */
    static final class Estilos {

        final CellStyle cabecalho;
        final CellStyle texto;
        final CellStyle data;
        final CellStyle valor;
        final CellStyle totalRotulo;
        final CellStyle totalValor;
        private final XSSFWorkbook planilha;

        Estilos(XSSFWorkbook planilha, String formatoValor) {
            this.planilha = planilha;
            XSSFFont negrito = fonte(true);
            negrito.setColor(IndexedColors.WHITE.getIndex());

            cabecalho = centralizado(negrito);
            cabecalho.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFFont normal = fonte(false);
            texto = centralizado(normal);

            short formatoData = planilha.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy");
            data = centralizado(normal);
            data.setDataFormat(formatoData);

            short formatoMoeda = planilha.getCreationHelper().createDataFormat().getFormat(formatoValor);
            valor = centralizado(normal);
            valor.setDataFormat(formatoMoeda);

            totalRotulo = centralizado(fonte(true));
            totalRotulo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalRotulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            totalValor = planilha.createCellStyle();
            totalValor.cloneStyleFrom(totalRotulo);
            totalValor.setDataFormat(formatoMoeda);
        }

        /** Célula em negrito com fundo e letra coloridos, em hexadecimal (ex.: "C6EFCE"). */
        CellStyle destaque(String fundo, String letra) {
            XSSFFont fonte = fonte(true);
            fonte.setColor(cor(letra));

            XSSFCellStyle estilo = centralizado(fonte);
            estilo.setFillForegroundColor(cor(fundo));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return estilo;
        }

        private XSSFFont fonte(boolean negrito) {
            XSSFFont fonte = planilha.createFont();
            fonte.setFontHeightInPoints(TAMANHO_FONTE);
            fonte.setBold(negrito);
            return fonte;
        }

        private XSSFCellStyle centralizado(XSSFFont fonte) {
            XSSFCellStyle estilo = planilha.createCellStyle();
            estilo.setFont(fonte);
            estilo.setAlignment(HorizontalAlignment.CENTER);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);
            estilo.setWrapText(true);
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
            return estilo;
        }

        private static XSSFColor cor(String hexadecimal) {
            return new XSSFColor(HexFormat.of().parseHex(hexadecimal), null);
        }
    }
}
