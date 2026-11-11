package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.BeneficioConcedido;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.TipoServico;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BeneficiosConcedidosExcelTest {

    private final TipoServico maternidade = new TipoServico("Salário-maternidade", null);
    private final TipoServico bpc = new TipoServico("Benefício de Prestação Continuada", null);
    private final Responsavel ana = new Responsavel("Ana Souza", null);
    private final YearMonth mes = YearMonth.of(2026, 11);

    @Test
    void geraTabelaComImpressaoEmPaisagemNoMesmoLeiauteDasOutrasPlanilhas() throws IOException {
        BeneficioConcedido comCpf = beneficio("Maria Silva", "11144477735", maternidade, ana);
        comCpf.setComPagamento(true);
        BeneficioConcedido semCpf = beneficio("José Santos", null, bpc, new Responsavel("Bruno Lima", null));

        try (XSSFWorkbook planilha = ler(BeneficiosConcedidosExcel.gerar("Novembro de 2026",
                List.of(comCpf, semCpf)))) {
            XSSFSheet aba = planilha.getSheet(BeneficiosConcedidosExcel.ABA);
            assertThat(aba).isNotNull();

            List<String> cabecalho = new ArrayList<>();
            aba.getRow(0).forEach(c -> cabecalho.add(c.getStringCellValue()));
            assertThat(cabecalho).containsExactlyElementsOf(BeneficiosConcedidosExcel.CABECALHO);

            XSSFRow primeira = aba.getRow(1);
            assertThat(primeira.getCell(0).getStringCellValue()).isEqualTo("MARIA SILVA");
            assertThat(primeira.getCell(1).getStringCellValue()).isEqualTo("111.444.777-35");
            assertThat(primeira.getCell(2).getStringCellValue()).isEqualTo("Salário-maternidade");
            assertThat(primeira.getCell(3).getStringCellValue()).isEqualTo("Ana Souza");
            assertThat(primeira.getCell(4).getStringCellValue()).isEqualTo(BeneficiosConcedidosExcel.COM_PAGAMENTO);

            XSSFRow segunda = aba.getRow(2);
            assertThat(segunda.getCell(0).getStringCellValue()).isEqualTo("JOSÉ SANTOS");
            assertThat(segunda.getCell(1).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(segunda.getCell(2).getStringCellValue()).isEqualTo("Benefício de Prestação Continuada");
            assertThat(segunda.getCell(3).getStringCellValue()).isEqualTo("Bruno Lima");
            assertThat(segunda.getCell(4).getStringCellValue()).isEqualTo(BeneficiosConcedidosExcel.SEM_PAGAMENTO);

            assertThat(aba.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Total (2 benefícios)");

            assertThat(aba.getTables()).singleElement().satisfies((XSSFTable tabela) ->
                    assertThat(tabela.getArea().formatAsString()).isEqualTo("A1:E3"));

            PrintSetup impressao = aba.getPrintSetup();
            assertThat(impressao.getLandscape()).isTrue();
            assertThat(impressao.getPaperSize()).isEqualTo(PrintSetup.A4_PAPERSIZE);
            assertThat(impressao.getFitWidth()).isEqualTo((short) 1);
            assertThat(impressao.getFitHeight()).isEqualTo((short) 0);
            assertThat(aba.getHeader().getCenter()).isEqualTo("Benefícios concedidos — Novembro de 2026");
            assertThat(aba.getRepeatingRows().formatAsString()).isEqualTo("1:1");
            assertThat(aba.getFitToPage()).isTrue();
        }
    }

    @Test
    void nomeLongoQuebraLinha() throws IOException {
        String nome = "Cliente com nome longo demais para caber em uma única linha da coluna da planilha impressa";
        BeneficioConcedido beneficio = beneficio(nome, null, maternidade, ana);

        try (XSSFWorkbook planilha = ler(BeneficiosConcedidosExcel.gerar("Novembro de 2026", List.of(beneficio)))) {
            XSSFSheet aba = planilha.getSheet(BeneficiosConcedidosExcel.ABA);
            assertThat(aba.getRow(1).getCell(0).getCellStyle().getWrapText()).isTrue();
            assertThat(aba.getRow(1).getHeightInPoints()).isGreaterThan(21f);
            assertThat(aba.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Total (1 benefício)");
        }
    }

    @Test
    void semResponsavelMostraTraco() throws IOException {
        try (XSSFWorkbook planilha = ler(BeneficiosConcedidosExcel.gerar("Novembro de 2026",
                List.of(beneficio("Maria Silva", null, maternidade, null))))) {
            XSSFSheet aba = planilha.getSheet(BeneficiosConcedidosExcel.ABA);
            assertThat(aba.getRow(1).getCell(3).getStringCellValue()).isEqualTo("—");
        }
    }

    @Test
    void semBeneficiosGeraCabecalhoETotalZerado() throws IOException {
        try (XSSFWorkbook planilha = ler(BeneficiosConcedidosExcel.gerar("Novembro de 2026", List.of()))) {
            XSSFSheet aba = planilha.getSheet(BeneficiosConcedidosExcel.ABA);
            assertThat(aba.getLastRowNum()).isEqualTo(1);
            assertThat(aba.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Total (0 benefícios)");
            assertThat(aba.getTables()).isEmpty();
        }
    }

    private BeneficioConcedido beneficio(String nome, String cpf, TipoServico tipo, Responsavel responsavel) {
        Cliente cliente = new Cliente(nome, LocalDate.of(2026, 11, 1));
        cliente.setCpf(cpf);
        return new BeneficioConcedido(cliente, tipo, responsavel, mes);
    }

    private static XSSFWorkbook ler(byte[] arquivo) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(arquivo));
    }
}
