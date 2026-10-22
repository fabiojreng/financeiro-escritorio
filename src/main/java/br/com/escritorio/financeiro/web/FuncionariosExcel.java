package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.AcertoMensal;
import br.com.escritorio.financeiro.dto.AcertoFuncionario;
import br.com.escritorio.financeiro.dto.ResumoAcertosMes;
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
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableStyleInfo;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Planilha do total gasto com funcionários: uma linha por funcionário e competência, com o quadro do acerto,
 * o custo do mês (salário, extras e reembolsos, menos descontos) e a situação do pagamento.
 */
final class FuncionariosExcel {

    static final String ABA = "Funcionários";
    static final String TABELA = "Funcionarios";
    static final List<String> CABECALHO = List.of(
            "COMPETÊNCIA", "FUNCIONÁRIO", "SALÁRIO BASE", "BONIFICAÇÕES / EXTRAS", "REEMBOLSOS", "VALES",
            "EMPRÉSTIMO", "DESCONTOS", "LÍQUIDO", "CUSTO DO MÊS", "SITUAÇÃO", "DATA PAGTO");

    private static final int LINHA_CABECALHO = 0;
    private static final int PRIMEIRA_COLUNA_VALOR = 2;
    private static final int ULTIMA_COLUNA_VALOR = 9;
    private static final int[] LARGURAS = {14, 36, 16, 24, 15, 15, 15, 15, 16, 16, 14, 14};

    private FuncionariosExcel() {
    }

    static byte[] gerar(String tituloPeriodo, List<ResumoAcertosMes> meses) {
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            XSSFSheet aba = planilha.createSheet(ABA);
            Estilos estilos = new Estilos(planilha);

            Row cabecalho = aba.createRow(LINHA_CABECALHO);
            cabecalho.setHeightInPoints(30f);
            for (int i = 0; i < CABECALHO.size(); i++) {
                Cell celula = cabecalho.createCell(i);
                celula.setCellValue(CABECALHO.get(i));
                celula.setCellStyle(estilos.cabecalho);
            }

            int linha = LINHA_CABECALHO + 1;
            for (ResumoAcertosMes mes : meses) {
                for (AcertoFuncionario acerto : mes.acertos()) {
                    Row row = aba.createRow(linha++);
                    AcertoMensal quadro = acerto.acerto();
                    texto(row, 0, TituloPeriodo.mes(mes.competencia()), estilos.texto);
                    texto(row, 1, acerto.funcionario().getNome(), estilos.textoEsquerda);
                    valor(row, 2, quadro.salarioBase(), estilos.valor);
                    valor(row, 3, quadro.bonificacoes(), estilos.valor);
                    valor(row, 4, quadro.reembolsos(), estilos.valor);
                    valor(row, 5, quadro.vales(), estilos.valor);
                    valor(row, 6, quadro.parcelas(), estilos.valor);
                    valor(row, 7, quadro.descontos(), estilos.valor);
                    valor(row, 8, quadro.liquido(), estilos.valor);
                    valor(row, 9, acerto.getCusto(), estilos.valor);
                    texto(row, 10, acerto.isPago() ? "Pago" : "Em aberto", estilos.texto);
                    data(row, 11, acerto.isPago() ? acerto.fechamento().getDataPagamento() : null, estilos.data);
                }
            }
            int quantidade = linha - LINHA_CABECALHO - 1;
            escreverTotal(aba, estilos, linha, quantidade);

            for (int i = 0; i < LARGURAS.length; i++) {
                aba.setColumnWidth(i, LARGURAS[i] * 256);
            }
            aba.createFreezePane(0, 1);
            if (quantidade > 0) {
                criarTabela(aba, linha - 1);
            }
            configurarImpressao(planilha, aba, tituloPeriodo, linha);
            XSSFFormulaEvaluator.evaluateAllFormulaCells(planilha);

            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar a planilha de funcionários.", e);
        }
    }

    private static void escreverTotal(XSSFSheet aba, Estilos estilos, int linha, int quantidade) {
        Row row = aba.createRow(linha);
        texto(row, 0, "Total", estilos.totalRotulo);
        texto(row, 1, quantidade == 1 ? "1 acerto" : quantidade + " acertos", estilos.totalRotulo);
        for (int coluna = PRIMEIRA_COLUNA_VALOR; coluna <= ULTIMA_COLUNA_VALOR; coluna++) {
            Cell celula = row.createCell(coluna);
            celula.setCellStyle(estilos.totalValor);
            if (quantidade > 0) {
                String letra = CellReference.convertNumToColString(coluna);
                celula.setCellFormula("SUM(" + letra + "2:" + letra + linha + ")");
            } else {
                celula.setCellValue(0);
            }
        }
        for (int coluna = ULTIMA_COLUNA_VALOR + 1; coluna < CABECALHO.size(); coluna++) {
            row.createCell(coluna).setCellStyle(estilos.totalRotulo);
        }
    }

    private static void configurarImpressao(XSSFWorkbook planilha, XSSFSheet aba, String tituloPeriodo,
                                            int ultimaLinha) {
        PrintSetup impressao = aba.getPrintSetup();
        impressao.setLandscape(true);
        impressao.setPaperSize(PrintSetup.A4_PAPERSIZE);
        impressao.setFitWidth((short) 1);
        impressao.setFitHeight((short) 0);
        aba.setFitToPage(true);
        aba.setAutobreaks(true);
        aba.setHorizontallyCenter(true);
        aba.getHeader().setCenter("Funcionários — " + tituloPeriodo);
        aba.getFooter().setRight("Página &P de &N");
        aba.setRepeatingRows(CellRangeAddress.valueOf("1:1"));
        planilha.setPrintArea(0, 0, CABECALHO.size() - 1, 0, ultimaLinha);
    }

    private static void criarTabela(XSSFSheet aba, int ultimaLinha) {
        AreaReference area = new AreaReference(
                new CellReference(LINHA_CABECALHO, 0),
                new CellReference(ultimaLinha, CABECALHO.size() - 1),
                SpreadsheetVersion.EXCEL2007);
        XSSFTable tabela = aba.createTable(area);
        tabela.setName(TABELA);
        tabela.setDisplayName(TABELA);
        tabela.getCTTable().addNewAutoFilter().setRef(area.formatAsString());
        tabela.getCTTable().addNewTableStyleInfo();
        XSSFTableStyleInfo estilo = (XSSFTableStyleInfo) tabela.getStyle();
        estilo.setName("TableStyleMedium3");
        estilo.setShowRowStripes(true);
        estilo.setShowColumnStripes(false);
        tabela.updateHeaders();
    }

    private static void texto(Row row, int coluna, String valor, CellStyle estilo) {
        Cell celula = row.createCell(coluna);
        if (valor != null && !valor.isBlank()) {
            celula.setCellValue(valor);
        }
        celula.setCellStyle(estilo);
    }

    private static void data(Row row, int coluna, LocalDate valor, CellStyle estilo) {
        Cell celula = row.createCell(coluna);
        if (valor != null) {
            celula.setCellValue(valor);
        }
        celula.setCellStyle(estilo);
    }

    private static void valor(Row row, int coluna, BigDecimal valor, CellStyle estilo) {
        Cell celula = row.createCell(coluna);
        celula.setCellValue(valor == null ? 0 : valor.doubleValue());
        celula.setCellStyle(estilo);
    }

    private static final class Estilos {

        final CellStyle cabecalho;
        final CellStyle texto;
        final CellStyle textoEsquerda;
        final CellStyle data;
        final CellStyle valor;
        final CellStyle totalRotulo;
        final CellStyle totalValor;

        Estilos(XSSFWorkbook planilha) {
            XSSFFont negrito = planilha.createFont();
            negrito.setBold(true);
            negrito.setColor(IndexedColors.WHITE.getIndex());
            cabecalho = estilo(planilha, negrito, HorizontalAlignment.CENTER);
            cabecalho.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFFont normal = planilha.createFont();
            texto = estilo(planilha, normal, HorizontalAlignment.CENTER);
            textoEsquerda = estilo(planilha, normal, HorizontalAlignment.LEFT);

            data = estilo(planilha, normal, HorizontalAlignment.CENTER);
            data.setDataFormat(planilha.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));

            short formatoValor = planilha.getCreationHelper().createDataFormat().getFormat("#,##0.00");
            valor = estilo(planilha, normal, HorizontalAlignment.RIGHT);
            valor.setDataFormat(formatoValor);

            XSSFFont negritoTotal = planilha.createFont();
            negritoTotal.setBold(true);
            totalRotulo = estilo(planilha, negritoTotal, HorizontalAlignment.CENTER);
            totalRotulo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalRotulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            totalValor = planilha.createCellStyle();
            totalValor.cloneStyleFrom(totalRotulo);
            totalValor.setAlignment(HorizontalAlignment.RIGHT);
            totalValor.setDataFormat(formatoValor);
        }

        private static XSSFCellStyle estilo(XSSFWorkbook planilha, XSSFFont fonte, HorizontalAlignment alinhamento) {
            XSSFCellStyle estilo = planilha.createCellStyle();
            estilo.setFont(fonte);
            estilo.setAlignment(alinhamento);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
            return estilo;
        }
    }
}
