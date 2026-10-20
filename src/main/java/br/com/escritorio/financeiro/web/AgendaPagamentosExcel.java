package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Cpf;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
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
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFTableStyleInfo;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Planilha Excel da agenda de pagamentos no mesmo leiaute de impressão do modelo Domínio:
 * tabela a partir de A1, A4 paisagem, ajuste à largura da página e cabeçalho repetido.
 */
final class AgendaPagamentosExcel {

    static final String ABA = "Pagamentos";
    static final String TABELA = "Pagamentos";
    static final List<String> CABECALHO = List.of(
            "CLIENTE", "CPF", "RESPONSÁVEL", "DIA RECEB.", "BENEFÍCIO",
            "VALOR", "BANCO", "STATUS", "OBSERVAÇÕES", "HONORÁRIO");
    static final String SEM_HONORARIO = "Sem honorário";

    private static final int LINHA_CABECALHO = 0;
    private static final int COLUNA_VALOR = 5;
    private static final int COLUNA_HONORARIO = 9;
    /** Acima destas larguras (em caracteres) o texto quebra em mais linhas em vez de ser cortado. */
    private static final int[] LARGURAS_MAXIMAS = {40, 16, 25, 12, 35, 16, 30, 22, 45, 16};
    /** Espaço do botão de filtro da tabela, que fica sobre o cabeçalho. */
    private static final int FOLGA_FILTRO = 3;
    private static final float ALTURA_LINHA = 15f;
    private static final DecimalFormat MOEDA =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));

    private AgendaPagamentosExcel() {
    }

    static byte[] gerar(String tituloPeriodo, List<PagamentoAgendado> pagamentos) {
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            XSSFSheet aba = planilha.createSheet(ABA);
            Estilos estilos = new Estilos(planilha);

            Row cabecalho = aba.createRow(LINHA_CABECALHO);
            cabecalho.setHeightInPoints(30);
            for (int i = 0; i < CABECALHO.size(); i++) {
                Cell celula = cabecalho.createCell(i);
                celula.setCellValue(CABECALHO.get(i));
                celula.setCellStyle(estilos.cabecalho);
            }

            int[] larguras = larguras(pagamentos);
            int linha = LINHA_CABECALHO + 1;
            for (PagamentoAgendado pagamento : pagamentos) {
                Row row = aba.createRow(linha++);
                int linhasTexto = linhasNecessarias(larguras, pagamento.getCliente().getNome(), null,
                        pagamento.getNomesResponsaveis(), null, pagamento.getTipoServico().getNome(), null,
                        pagamento.getBanco(), null, pagamento.getObservacoes());
                if (linhasTexto > 1) {
                    row.setHeightInPoints(ALTURA_LINHA * linhasTexto);
                }
                texto(row, 0, pagamento.getCliente().getNome(), estilos.texto);
                texto(row, 1, Cpf.formatar(pagamento.getCliente().getCpf()), estilos.centralizado);
                texto(row, 2, pagamento.getNomesResponsaveis(), estilos.texto);
                data(row, 3, pagamento.getDataRecebimento(), estilos.data);
                texto(row, 4, pagamento.getTipoServico().getNome(), estilos.texto);
                valor(row, COLUNA_VALOR, pagamento.getValor(), estilos.valor);
                texto(row, 6, pagamento.getBanco(), estilos.texto);
                texto(row, 7, pagamento.getStatus().getRotulo(), estilos.centralizado);
                texto(row, 8, pagamento.getObservacoes(), estilos.texto);
                texto(row, COLUNA_HONORARIO, situacaoHonorario(pagamento), estilos.honorario(pagamento));
            }

            escreverTotal(aba, estilos, linha, pagamentos.size());

            for (int i = 0; i < larguras.length; i++) {
                aba.setColumnWidth(i, larguras[i] * 256);
            }
            aba.createFreezePane(0, 1);
            if (!pagamentos.isEmpty()) {
                criarTabela(aba, linha - 1);
            }
            configurarImpressao(planilha, aba, tituloPeriodo, linha);
            XSSFFormulaEvaluator.evaluateAllFormulaCells(planilha);

            planilha.write(saida);
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gerar a agenda de pagamentos.", e);
        }
    }

    /**
     * Largura de cada coluna pelo maior conteúdo, limitada à máxima (o texto maior quebra linha),
     * mas nunca menor que o cabeçalho, que em negrito e maiúsculas ocupa cerca de 20% a mais por letra.
     */
    static int[] larguras(List<PagamentoAgendado> pagamentos) {
        int[] larguras = new int[CABECALHO.size()];
        BigDecimal total = BigDecimal.ZERO;
        for (PagamentoAgendado p : pagamentos) {
            ajustar(larguras, 0, p.getCliente().getNome());
            ajustar(larguras, 1, Cpf.formatar(p.getCliente().getCpf()));
            ajustar(larguras, 2, p.getNomesResponsaveis());
            ajustar(larguras, 3, "00/00/0000");
            ajustar(larguras, 4, p.getTipoServico().getNome());
            ajustar(larguras, COLUNA_VALOR, MOEDA.format(p.getValor()));
            ajustar(larguras, 6, p.getBanco());
            ajustar(larguras, 7, p.getStatus().getRotulo());
            ajustar(larguras, 8, p.getObservacoes());
            ajustar(larguras, COLUNA_HONORARIO, situacaoHonorario(p));
            total = total.add(p.getValor());
        }
        ajustar(larguras, 0, rotuloTotal(pagamentos.size()));
        ajustar(larguras, COLUNA_VALOR, MOEDA.format(total));
        for (int i = 0; i < larguras.length; i++) {
            int cabecalho = (int) Math.ceil(CABECALHO.get(i).length() * 1.2) + FOLGA_FILTRO;
            larguras[i] = Math.max(cabecalho, Math.min(larguras[i], LARGURAS_MAXIMAS[i]));
        }
        return larguras;
    }

    private static void ajustar(int[] larguras, int coluna, String texto) {
        if (texto != null) {
            larguras[coluna] = Math.max(larguras[coluna], texto.length() + 2);
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

        // Margens do modelo Domínio (polegadas).
        aba.setMargin(XSSFSheet.LeftMargin, 0.24);
        aba.setMargin(XSSFSheet.RightMargin, 0.24);
        aba.setMargin(XSSFSheet.TopMargin, 0.75);
        aba.setMargin(XSSFSheet.BottomMargin, 0.75);
        aba.setMargin(XSSFSheet.HeaderMargin, 0.31);
        aba.setMargin(XSSFSheet.FooterMargin, 0.31);

        String periodo = tituloPeriodo == null || tituloPeriodo.isBlank()
                ? "Agenda de pagamentos"
                : tituloPeriodo;
        aba.getHeader().setCenter("Agenda de pagamentos — " + periodo);
        aba.getFooter().setRight("Página &P de &N");

        aba.setRepeatingRows(CellRangeAddress.valueOf("1:1"));
        planilha.setPrintArea(0, 0, CABECALHO.size() - 1, 0, ultimaLinha);
    }

    private static void escreverTotal(XSSFSheet aba, Estilos estilos, int linha, int quantidade) {
        Row row = aba.createRow(linha);
        row.setHeightInPoints(16);
        texto(row, 0, rotuloTotal(quantidade), estilos.totalRotulo);
        for (int i = 1; i < COLUNA_VALOR; i++) {
            row.createCell(i).setCellStyle(estilos.totalRotulo);
        }
        Cell celularValor = row.createCell(COLUNA_VALOR);
        celularValor.setCellStyle(estilos.totalValor);
        if (quantidade > 0) {
            celularValor.setCellFormula("SUM(F2:F" + linha + ")");
        } else {
            celularValor.setCellValue(0);
        }
        for (int i = COLUNA_VALOR + 1; i < CABECALHO.size(); i++) {
            row.createCell(i).setCellStyle(estilos.totalRotulo);
        }
    }

    /** Se a cliente já quitou o honorário com o escritório; sem valor lançado, ainda não há o que cobrar. */
    static String situacaoHonorario(PagamentoAgendado pagamento) {
        return pagamento.getValorHonorario() == null ? SEM_HONORARIO : pagamento.getSituacaoHonorario().getRotulo();
    }

    private static String rotuloTotal(int quantidade) {
        return quantidade == 1 ? "Total (1 pagamento)" : "Total (" + quantidade + " pagamentos)";
    }

    /** Linhas que o texto mais longo da linha ocupa quando quebrado na largura da coluna. */
    private static int linhasNecessarias(int[] larguras, String... textos) {
        int linhas = 1;
        for (int i = 0; i < textos.length; i++) {
            if (textos[i] != null) {
                int caberPorLinha = Math.max(1, larguras[i] - 2);
                linhas = Math.max(linhas, (textos[i].length() + caberPorLinha - 1) / caberPorLinha);
            }
        }
        return linhas;
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
        if (valor != null) {
            celula.setCellValue(valor.doubleValue());
        }
        celula.setCellStyle(estilo);
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
        final CellStyle totalRotulo;
        final CellStyle totalValor;
        final Map<SituacaoHonorarioPagamento, CellStyle> honorarios = new EnumMap<>(SituacaoHonorarioPagamento.class);
        final CellStyle semHonorario;

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
            aplicarBordas(cabecalho);

            texto = planilha.createCellStyle();
            texto.setVerticalAlignment(VerticalAlignment.CENTER);
            texto.setWrapText(true);
            aplicarBordas(texto);

            centralizado = planilha.createCellStyle();
            centralizado.setAlignment(HorizontalAlignment.CENTER);
            centralizado.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordas(centralizado);

            short formatoData = planilha.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy");
            data = planilha.createCellStyle();
            data.setDataFormat(formatoData);
            data.setAlignment(HorizontalAlignment.CENTER);
            data.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordas(data);

            short formatoValor = planilha.getCreationHelper().createDataFormat().getFormat("#,##0.00");
            valor = planilha.createCellStyle();
            valor.setDataFormat(formatoValor);
            valor.setAlignment(HorizontalAlignment.RIGHT);
            valor.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordas(valor);

            XSSFFont fonteTotal = planilha.createFont();
            fonteTotal.setBold(true);

            totalRotulo = planilha.createCellStyle();
            totalRotulo.setFont(fonteTotal);
            totalRotulo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalRotulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalRotulo.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordas(totalRotulo);

            totalValor = planilha.createCellStyle();
            totalValor.cloneStyleFrom(totalRotulo);
            totalValor.setDataFormat(formatoValor);
            totalValor.setAlignment(HorizontalAlignment.RIGHT);

            // Cores dos estilos "Bom", "Neutro" e "Ruim" do próprio Excel.
            honorarios.put(SituacaoHonorarioPagamento.RECEBIDO, destaque(planilha, "C6EFCE", "006100"));
            honorarios.put(SituacaoHonorarioPagamento.A_RECEBER, destaque(planilha, "FFEB9C", "9C5700"));
            honorarios.put(SituacaoHonorarioPagamento.EM_ATRASO, destaque(planilha, "FFC7CE", "9C0006"));
            CellStyle emAndamento = destaque(planilha, "DDEBF7", "1F4E78");
            honorarios.put(SituacaoHonorarioPagamento.PARCIAL, emAndamento);
            honorarios.put(SituacaoHonorarioPagamento.NEGOCIADO, emAndamento);
            semHonorario = destaque(planilha, "EDEDED", "595959");
        }

        CellStyle honorario(PagamentoAgendado pagamento) {
            return pagamento.getValorHonorario() == null
                    ? semHonorario
                    : honorarios.get(pagamento.getSituacaoHonorario());
        }

        private static CellStyle destaque(XSSFWorkbook planilha, String fundo, String letra) {
            XSSFFont fonte = planilha.createFont();
            fonte.setBold(true);
            fonte.setColor(cor(letra));

            XSSFCellStyle estilo = planilha.createCellStyle();
            estilo.setFont(fonte);
            estilo.setFillForegroundColor(cor(fundo));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estilo.setAlignment(HorizontalAlignment.CENTER);
            estilo.setVerticalAlignment(VerticalAlignment.CENTER);
            aplicarBordas(estilo);
            return estilo;
        }

        private static XSSFColor cor(String hexadecimal) {
            return new XSSFColor(HexFormat.of().parseHex(hexadecimal), null);
        }

        private static void aplicarBordas(CellStyle estilo) {
            estilo.setBorderTop(BorderStyle.THIN);
            estilo.setBorderBottom(BorderStyle.THIN);
            estilo.setBorderLeft(BorderStyle.THIN);
            estilo.setBorderRight(BorderStyle.THIN);
        }
    }
}
