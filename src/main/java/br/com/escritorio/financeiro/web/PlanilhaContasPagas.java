package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Despesa;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableStyleInfo;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Planilha "Contas Pagas" no mesmo leiaute do modelo enviado pela contabilidade para importar no
 * Sistema Domínio: mesma aba, mesmos cabeçalhos (inclusive espaços), mesma ordem de colunas e a
 * coluna VALOR NEGATIVO oculta com a fórmula {@code =-VALOR}.
 */
final class PlanilhaContasPagas {

    static final String ABA = "Contas Pagas";
    static final String TABELA = "Tabela1";
    static final List<String> CABECALHO = List.of("DATA ", "CATEGORIA", "FORMA PAGTO", "HISTORICO",
            "Nº NOTA/ REFERENCIA DO PAGAMENTO", "VALOR", "FORNECEDOR", "VALOR NEGATIVO");
    static final int COLUNA_VALOR_NEGATIVO = 7;

    private static final String FORMATO_VALOR = "\"R$\" #,##0.00";

    private static final int[] LARGURAS = {11, 24, 17, 44, 20, 16, 47, 21};

    private PlanilhaContasPagas() {
    }

    static byte[] gerar(List<Despesa> despesas) {
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            XSSFSheet aba = planilha.createSheet(ABA);
            Estilos estilos = new Estilos(planilha);

            Row cabecalho = aba.createRow(0);
            cabecalho.setHeightInPoints(30);
            for (int i = 0; i < CABECALHO.size(); i++) {
                Cell celula = cabecalho.createCell(i);
                celula.setCellValue(CABECALHO.get(i));
                celula.setCellStyle(estilos.cabecalho);
            }

            int linha = 1;
            for (Despesa despesa : despesas) {
                Row row = aba.createRow(linha);
                celula(row, 0, null, estilos.data).setCellValue(despesa.getDataPagamento());
                celula(row, 1, despesa.getCategoria().getNome(), estilos.texto);
                celula(row, 2, despesa.getFormaPagamento().getNome(), estilos.centralizado);
                celula(row, 3, despesa.getHistorico(), estilos.texto);
                celula(row, 4, despesa.getNumeroDocumento(), estilos.texto);
                celula(row, 5, null, estilos.valor).setCellValue(despesa.getValor().doubleValue());
                celula(row, 6, despesa.getFornecedor(), estilos.texto);
                Cell negativo = row.createCell(COLUNA_VALOR_NEGATIVO);
                negativo.setCellFormula("-F" + (linha + 1));
                negativo.setCellStyle(estilos.valor);
                linha++;
            }

            for (int i = 0; i < LARGURAS.length; i++) {
                aba.setColumnWidth(i, LARGURAS[i] * 256);
            }
            aba.setColumnHidden(COLUNA_VALOR_NEGATIVO, true);
            aba.createFreezePane(0, 1);

            if (!despesas.isEmpty()) {
                criarTabela(planilha, aba, linha - 1);
            }
            XSSFFormulaEvaluator.evaluateAllFormulaCells(planilha);

            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar a planilha de despesas.", e);
        }
    }

    private static Cell celula(Row row, int coluna, String valor, CellStyle estilo) {
        Cell celula = row.createCell(coluna);
        if (valor != null) {
            celula.setCellValue(valor);
        }
        celula.setCellStyle(estilo);
        return celula;
    }

    private static void criarTabela(XSSFWorkbook planilha, XSSFSheet aba, int ultimaLinha) {
        AreaReference area = new AreaReference(new CellReference(0, 0),
                new CellReference(ultimaLinha, CABECALHO.size() - 1), SpreadsheetVersion.EXCEL2007);
        XSSFTable tabela = aba.createTable(area);
        tabela.setName(TABELA);
        tabela.setDisplayName(TABELA);
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

    private static final class Estilos {

        final CellStyle cabecalho;
        final CellStyle texto;
        final CellStyle centralizado;
        final CellStyle data;
        final CellStyle valor;

        Estilos(XSSFWorkbook planilha) {
            XSSFFont negrito = planilha.createFont();
            negrito.setBold(true);
            negrito.setColor(IndexedColors.WHITE.getIndex());

            cabecalho = planilha.createCellStyle();
            cabecalho.setFont(negrito);
            cabecalho.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cabecalho.setAlignment(HorizontalAlignment.CENTER);
            cabecalho.setVerticalAlignment(VerticalAlignment.CENTER);
            cabecalho.setWrapText(true);
            cabecalho.setBorderBottom(BorderStyle.THIN);

            texto = planilha.createCellStyle();

            centralizado = planilha.createCellStyle();
            centralizado.setAlignment(HorizontalAlignment.CENTER);

            short formatoData = planilha.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy");
            data = planilha.createCellStyle();
            data.setDataFormat(formatoData);
            data.setAlignment(HorizontalAlignment.CENTER);

            short formatoValor = planilha.getCreationHelper().createDataFormat().getFormat(FORMATO_VALOR);
            valor = planilha.createCellStyle();
            valor.setDataFormat(formatoValor);
        }
    }
}
