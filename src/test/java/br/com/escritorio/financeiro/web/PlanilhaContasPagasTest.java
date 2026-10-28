package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import br.com.escritorio.financeiro.domain.Despesa;
import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanilhaContasPagasTest {

    private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

    private final CategoriaDespesa energia = new CategoriaDespesa("Energia Eletrica", "Despesa c/ Energia, ref.");
    private final FormaPagamentoDespesa debito = new FormaPagamentoDespesa("DEB CX ECO", null);

    @Test
    void segueOLeiauteDoModeloDaContabilidade() throws IOException {
        Despesa energiaSetembro = new Despesa(LocalDate.of(2026, 9, 7), energia, debito,
                "Despesa c/ Energia, ref. 08/2026", new BigDecimal("1608.08"), "Companhia de Energia");
        energiaSetembro.setNumeroDocumento("08/2026");
        Despesa semNota = new Despesa(LocalDate.of(2026, 9, 21), new CategoriaDespesa("FGTS", "FGTS, ref."),
                new FormaPagamentoDespesa("BB", null), "FGTS, ref. 08/2026", new BigDecimal("1970.06"), "Caixa");

        try (XSSFWorkbook planilha = ler(PlanilhaContasPagas.gerar(SETEMBRO, List.of(energiaSetembro, semNota)))) {
            assertThat(planilha.getNumberOfSheets()).isEqualTo(1);
            XSSFSheet aba = planilha.getSheet("Contas Pagas");
            assertThat(aba).isNotNull();

            List<String> cabecalho = new ArrayList<>();
            aba.getRow(0).forEach(c -> cabecalho.add(c.getStringCellValue()));
            assertThat(cabecalho).containsExactly("DATA ", "CATEGORIA", "FORMA PAGTO", "HISTORICO",
                    "Nº NOTA/ REFERENCIA DO PAGAMENTO", "VALOR", "FORNECEDOR", "VALOR NEGATIVO");

            XSSFRow primeira = aba.getRow(1);
            assertThat(primeira.getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 7));
            assertThat(primeira.getCell(0).getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy");
            assertThat(primeira.getCell(1).getStringCellValue()).isEqualTo("Energia Eletrica");
            assertThat(primeira.getCell(2).getStringCellValue()).isEqualTo("DEB CX ECO");
            assertThat(primeira.getCell(3).getStringCellValue()).isEqualTo("Despesa c/ Energia, ref. 08/2026");
            assertThat(primeira.getCell(4).getStringCellValue()).isEqualTo("08/2026");
            assertThat(primeira.getCell(5).getNumericCellValue()).isEqualTo(1608.08);
            assertThat(primeira.getCell(5).getCellStyle().getDataFormatString()).isEqualTo("\"R$\" #,##0.00");
            assertThat(primeira.getCell(6).getStringCellValue()).isEqualTo("Companhia de Energia");
            assertThat(primeira.getCell(7).getCellFormula()).isEqualTo("-F2");
            assertThat(primeira.getCell(7).getCachedFormulaResultType()).isEqualTo(CellType.NUMERIC);
            assertThat(primeira.getCell(7).getNumericCellValue()).isEqualTo(-1608.08);

            XSSFRow segunda = aba.getRow(2);
            assertThat(segunda.getCell(4).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(segunda.getCell(7).getCellFormula()).isEqualTo("-F3");
            assertThat(aba.getLastRowNum()).isEqualTo(2);

            assertThat(aba.isColumnHidden(7)).isTrue();
            assertThat(aba.isColumnHidden(6)).isFalse();

            assertThat(aba.getTables()).singleElement().satisfies((XSSFTable tabela) -> {
                assertThat(tabela.getName()).isEqualTo("Tabela1");
                assertThat(tabela.getArea().formatAsString()).isEqualTo("A1:H3");
                assertThat(tabela.getColumns()).extracting(c -> c.getName())
                        .containsExactly("DATA ", "CATEGORIA", "FORMA PAGTO", "HISTORICO",
                                "Nº NOTA/ REFERENCIA DO PAGAMENTO", "VALOR", "FORNECEDOR", "VALOR NEGATIVO");
            });
        }
    }

    @Test
    void impressaoEmPaisagemComTextoLongoQuebrandoDentroDaColuna() throws IOException {
        String historico = "Despesa c/ manutenção do ar-condicionado da sala de atendimento, troca de filtros "
                + "e limpeza completa das unidades internas e externas";
        String fornecedor = "Refrigeração e Climatização Nossa Senhora Aparecida Comércio e Serviços Ltda";
        Despesa longa = new Despesa(LocalDate.of(2026, 9, 10), energia, debito, historico,
                new BigDecimal("850.00"), fornecedor);

        try (XSSFWorkbook planilha = ler(PlanilhaContasPagas.gerar(SETEMBRO, List.of(longa)))) {
            XSSFSheet aba = planilha.getSheet("Contas Pagas");
            int[] larguras = PlanilhaContasPagas.larguras(List.of(longa));
            for (int i = 0; i < larguras.length; i++) {
                assertThat(aba.getColumnWidth(i)).isEqualTo(larguras[i] * 256);
            }
            assertThat(larguras[3]).isLessThan(historico.length());
            assertThat(larguras[6]).isLessThan(fornecedor.length());

            XSSFRow linha = aba.getRow(1);
            for (int coluna = 0; coluna < 7; coluna++) {
                XSSFCell celula = linha.getCell(coluna);
                assertThat(celula.getCellStyle().getWrapText()).as("coluna %d", coluna).isTrue();
                assertThat(celula.getCellStyle().getAlignment()).isEqualTo(HorizontalAlignment.CENTER);
                assertThat(celula.getCellStyle().getVerticalAlignment()).isEqualTo(VerticalAlignment.CENTER);
            }
            assertThat(linha.getHeightInPoints()).isGreaterThanOrEqualTo(2 * 21f);

            PrintSetup impressao = aba.getPrintSetup();
            assertThat(impressao.getLandscape()).isTrue();
            assertThat(impressao.getPaperSize()).isEqualTo(PrintSetup.A4_PAPERSIZE);
            assertThat(impressao.getFitWidth()).isEqualTo((short) 1);
            assertThat(impressao.getFitHeight()).isEqualTo((short) 0);
            assertThat(aba.getHorizontallyCenter()).isTrue();
            assertThat(aba.getRepeatingRows().formatAsString()).isEqualTo("1:1");
            assertThat(aba.getHeader().getCenter()).isEqualTo("Contas pagas — Setembro de 2026");
        }
    }

    @Test
    void mesSemDespesasGeraSoOCabecalho() throws IOException {
        try (XSSFWorkbook planilha = ler(PlanilhaContasPagas.gerar(SETEMBRO, List.of()))) {
            XSSFSheet aba = planilha.getSheet("Contas Pagas");
            assertThat(aba.getLastRowNum()).isZero();
            assertThat(aba.getRow(0).getCell(0).getStringCellValue()).isEqualTo("DATA ");
            assertThat(aba.getTables()).isEmpty();
            assertThat(aba.getPrintSetup().getLandscape()).isTrue();
        }
    }

    private static XSSFWorkbook ler(byte[] conteudo) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(conteudo));
    }
}
