package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgendaPagamentosExcelTest {

    @Test
    void geraTabelaNoPadraoDominioComImpressaoEmPaisagem() throws IOException {
        PagamentoAgendado pagamento = pagamento("Maria Silva", "11144477735", LocalDate.of(2026, 10, 16),
                "1500.00", StatusPagamento.AGENDADO);
        PagamentoAgendado outro = pagamento("José Santos", "39053344705", LocalDate.of(2026, 10, 17),
                "800.50", StatusPagamento.PAGO);

        try (XSSFWorkbook planilha = ler(AgendaPagamentosExcel.gerar(
                "Semana de 12/10/2026 a 18/10/2026", List.of(pagamento, outro)))) {
            assertThat(planilha.getNumberOfSheets()).isEqualTo(1);
            XSSFSheet aba = planilha.getSheet(AgendaPagamentosExcel.ABA);
            assertThat(aba).isNotNull();
            assertThat(aba.getMergedRegions()).isEmpty();

            List<String> cabecalho = new ArrayList<>();
            aba.getRow(0).forEach(c -> cabecalho.add(c.getStringCellValue()));
            assertThat(cabecalho).containsExactlyElementsOf(AgendaPagamentosExcel.CABECALHO);

            XSSFRow primeira = aba.getRow(1);
            assertThat(primeira.getCell(0).getStringCellValue()).isEqualTo("Maria Silva");
            assertThat(primeira.getCell(1).getStringCellValue()).isEqualTo("111.444.777-35");
            assertThat(primeira.getCell(3).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 10, 16));
            assertThat(primeira.getCell(3).getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy");
            assertThat(primeira.getCell(5).getNumericCellValue()).isEqualTo(1500.00);
            assertThat(primeira.getCell(5).getCellStyle().getDataFormatString()).isEqualTo("#,##0.00");
            assertThat(primeira.getCell(7).getStringCellValue()).isEqualTo("Agendado");

            XSSFRow total = aba.getRow(3);
            assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Total (2 pagamentos)");
            assertThat(total.getCell(5).getCellFormula()).isEqualTo("SUM(F2:F3)");
            assertThat(total.getCell(5).getCachedFormulaResultType()).isEqualTo(CellType.NUMERIC);
            assertThat(total.getCell(5).getNumericCellValue()).isEqualTo(2300.50);

            assertThat(aba.getTables()).singleElement().satisfies((XSSFTable tabela) -> {
                assertThat(tabela.getName()).isEqualTo(AgendaPagamentosExcel.TABELA);
                assertThat(tabela.getArea().formatAsString()).isEqualTo("A1:J3");
            });

            PrintSetup impressao = aba.getPrintSetup();
            assertThat(impressao.getLandscape()).isTrue();
            assertThat(impressao.getPaperSize()).isEqualTo(PrintSetup.A4_PAPERSIZE);
            assertThat(impressao.getFitWidth()).isEqualTo((short) 1);
            assertThat(impressao.getFitHeight()).isEqualTo((short) 0);
            assertThat(aba.getHeader().getCenter())
                    .isEqualTo("Agenda de pagamentos — Semana de 12/10/2026 a 18/10/2026");
            assertThat(aba.getRepeatingRows().formatAsString()).isEqualTo("1:1");
        }
    }

    @Test
    void colunasAcompanhamOConteudoETextoMuitoLongoQuebraLinha() throws IOException {
        PagamentoAgendado pagamento = pagamento("Erislene de Sousa Silva Nascimento", "11144477735",
                LocalDate.of(2026, 10, 27), "3400.00", StatusPagamento.PENDENTE);
        pagamento.setObservacoes("Cliente pediu para avisar por telefone antes de ir ao banco, "
                + "pois mora longe da agência e depende de transporte da prefeitura.");

        try (XSSFWorkbook planilha = ler(AgendaPagamentosExcel.gerar("Outubro de 2026", List.of(pagamento)))) {
            XSSFSheet aba = planilha.getSheet(AgendaPagamentosExcel.ABA);
            int[] larguras = AgendaPagamentosExcel.larguras(List.of(pagamento));

            assertThat(larguras[0]).isGreaterThanOrEqualTo("Erislene de Sousa Silva Nascimento".length());
            assertThat(larguras[4]).isGreaterThanOrEqualTo("Salário-maternidade".length());
            assertThat(larguras[6]).isGreaterThanOrEqualTo("Caixa Econômica Federal".length());
            assertThat(larguras[7]).isGreaterThanOrEqualTo("Pendente / não pago".length());
            for (int i = 0; i < larguras.length; i++) {
                assertThat(aba.getColumnWidth(i)).isEqualTo(larguras[i] * 256);
                String titulo = AgendaPagamentosExcel.CABECALHO.get(i);
                assertThat(larguras[i]).as(titulo).isGreaterThan(titulo.length());
            }

            assertThat(larguras[8]).isEqualTo(45);
            XSSFRow linha = aba.getRow(1);
            assertThat(linha.getCell(8).getCellStyle().getWrapText()).isTrue();
            assertThat(linha.getHeightInPoints()).isGreaterThanOrEqualTo(30f);
        }
    }

    @Test
    void colunaHonorarioDepoisDasObservacoesMostraSeAClienteJaQuitouComCores() throws IOException {
        PagamentoAgendado quitado = pagamento("Maria Silva", "11144477735", LocalDate.of(2026, 10, 16),
                "1500.00", StatusPagamento.PAGO);
        quitado.setValorHonorario(new BigDecimal("450.00"));
        quitado.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);
        PagamentoAgendado aReceber = pagamento("José Santos", "39053344705", LocalDate.of(2026, 10, 17),
                "800.00", StatusPagamento.AGENDADO);
        aReceber.setValorHonorario(new BigDecimal("300.00"));
        PagamentoAgendado atrasado = pagamento("Ana Souza", "52998224725", LocalDate.of(2026, 10, 18),
                "900.00", StatusPagamento.PAGO);
        atrasado.setValorHonorario(new BigDecimal("300.00"));
        atrasado.setSituacaoHonorario(SituacaoHonorarioPagamento.EM_ATRASO);
        PagamentoAgendado semHonorario = pagamento("Luzia Costa", null, LocalDate.of(2026, 10, 19),
                "700.00", StatusPagamento.AGENDADO);
        semHonorario.adicionarResponsavel(new Responsavel("Bruno Lima", null));

        try (XSSFWorkbook planilha = ler(AgendaPagamentosExcel.gerar("Outubro de 2026",
                List.of(quitado, aReceber, atrasado, semHonorario)))) {
            XSSFSheet aba = planilha.getSheet(AgendaPagamentosExcel.ABA);
            int coluna = AgendaPagamentosExcel.CABECALHO.indexOf("HONORÁRIO");
            assertThat(coluna).isEqualTo(AgendaPagamentosExcel.CABECALHO.indexOf("OBSERVAÇÕES") + 1);

            assertThat(textoEFundo(aba.getRow(1), coluna)).containsExactly("Recebido", "C6EFCE");
            assertThat(textoEFundo(aba.getRow(2), coluna)).containsExactly("A receber", "FFEB9C");
            assertThat(textoEFundo(aba.getRow(3), coluna)).containsExactly("Em atraso", "FFC7CE");
            assertThat(textoEFundo(aba.getRow(4), coluna))
                    .containsExactly(AgendaPagamentosExcel.SEM_HONORARIO, "EDEDED");

            assertThat(aba.getRow(4).getCell(2).getStringCellValue()).isEqualTo("Ana Responsável, Bruno Lima");
        }
    }

    @Test
    void semPagamentosGeraCabecalhoETotalZerado() throws IOException {
        try (XSSFWorkbook planilha = ler(AgendaPagamentosExcel.gerar("Outubro de 2026", List.of()))) {
            XSSFSheet aba = planilha.getSheet(AgendaPagamentosExcel.ABA);
            assertThat(aba.getLastRowNum()).isEqualTo(1);
            assertThat(aba.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Total (0 pagamentos)");
            assertThat(aba.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(0);
            assertThat(aba.getTables()).isEmpty();
            assertThat(aba.getPrintSetup().getLandscape()).isTrue();
        }
    }

    private static PagamentoAgendado pagamento(String nome, String cpf, LocalDate data, String valor,
                                               StatusPagamento status) {
        Cliente cliente = new Cliente(nome, data);
        cliente.setCpf(cpf);
        Responsavel responsavel = new Responsavel("Ana Responsável", null);
        TipoServico tipo = new TipoServico("Salário-maternidade", null);
        PagamentoAgendado pagamento = new PagamentoAgendado(cliente, List.of(responsavel), tipo, data,
                new BigDecimal(valor), "Caixa Econômica Federal");
        pagamento.setStatus(status);
        return pagamento;
    }

    private static List<String> textoEFundo(XSSFRow linha, int coluna) {
        XSSFCell celula = linha.getCell(coluna);
        XSSFColor fundo = celula.getCellStyle().getFillForegroundColorColor();
        return List.of(celula.getStringCellValue(), HexFormat.of().withUpperCase().formatHex(fundo.getRGB()));
    }

    private static XSSFWorkbook ler(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }
}
