package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.LocalPericia;
import br.com.escritorio.financeiro.domain.Pericia;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.TipoPericia;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgendaPericiasExcelTest {

    private final TipoPericia tipo = new TipoPericia("Perícia Médica Inicial", null);
    private final Responsavel escritorio = new Responsavel("Escritório", null);
    private final LocalPericia grajau = new LocalPericia("APS Grajaú", null);

    @Test
    void geraTabelaComImpressaoEmPaisagemNoMesmoLeiauteDaAgendaDePagamentos() throws IOException {
        Pericia manha = pericia("Maria Silva", "11144477735", LocalDate.of(2026, 10, 30), LocalTime.of(9, 0));
        manha.getCliente().setNomeMae("Raimunda Silva");
        manha.setLocal(grajau);
        Pericia semLocal = pericia("José Santos", null, LocalDate.of(2026, 10, 30), null);

        try (XSSFWorkbook planilha = ler(AgendaPericiasExcel.gerar("Sexta-feira, 30/10/2026",
                List.of(manha, semLocal)))) {
            XSSFSheet aba = planilha.getSheet(AgendaPericiasExcel.ABA);
            assertThat(aba).isNotNull();

            List<String> cabecalho = new ArrayList<>();
            aba.getRow(0).forEach(c -> cabecalho.add(c.getStringCellValue()));
            assertThat(cabecalho).containsExactlyElementsOf(AgendaPericiasExcel.CABECALHO);

            XSSFRow primeira = aba.getRow(1);
            assertThat(primeira.getCell(0).getLocalDateTimeCellValue().toLocalDate())
                    .isEqualTo(LocalDate.of(2026, 10, 30));
            assertThat(primeira.getCell(0).getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy");
            assertThat(primeira.getCell(1).getStringCellValue()).isEqualTo("09:00");
            assertThat(primeira.getCell(2).getStringCellValue()).isEqualTo("Maria Silva");
            assertThat(primeira.getCell(3).getStringCellValue()).isEqualTo("111.444.777-35");
            assertThat(primeira.getCell(4).getStringCellValue()).isEqualTo("Raimunda Silva");
            assertThat(primeira.getCell(5).getStringCellValue()).isEqualTo("Perícia Médica Inicial");
            assertThat(primeira.getCell(6).getStringCellValue()).isEqualTo("Escritório");
            assertThat(primeira.getCell(7).getStringCellValue()).isEqualTo("APS Grajaú");
            assertThat(textoEFundo(primeira.getCell(8))).containsExactly("Agendada", "DDEBF7");

            XSSFRow segunda = aba.getRow(2);
            assertThat(segunda.getCell(1).getStringCellValue()).isEqualTo("—");
            assertThat(segunda.getCell(7).getStringCellValue()).isEqualTo(AgendaPericiasExcel.LOCAL_A_DEFINIR);

            assertThat(aba.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Total (2 perícias)");

            assertThat(aba.getTables()).singleElement().satisfies((XSSFTable tabela) ->
                    assertThat(tabela.getArea().formatAsString()).isEqualTo("A1:J3"));

            PrintSetup impressao = aba.getPrintSetup();
            assertThat(impressao.getLandscape()).isTrue();
            assertThat(impressao.getPaperSize()).isEqualTo(PrintSetup.A4_PAPERSIZE);
            assertThat(impressao.getFitWidth()).isEqualTo((short) 1);
            assertThat(impressao.getFitHeight()).isEqualTo((short) 0);
            assertThat(aba.getHeader().getCenter()).isEqualTo("Agenda de perícias — Sexta-feira, 30/10/2026");
            assertThat(aba.getRepeatingRows().formatAsString()).isEqualTo("1:1");
        }
    }

    @Test
    void situacoesColoridasEObservacaoLongaQuebraLinha() throws IOException {
        Pericia realizada = pericia("Ana Souza", null, LocalDate.of(2026, 10, 26), LocalTime.of(8, 0));
        realizada.marcarRealizada("ana", LocalDateTime.of(2026, 10, 26, 12, 0));
        Pericia faltou = pericia("Luzia Costa", null, LocalDate.of(2026, 10, 27), LocalTime.of(10, 0));
        faltou.marcarFalta("Não compareceu", "ana", LocalDateTime.of(2026, 10, 27, 12, 0));
        faltou.setObservacoes("Cliente avisou que estava internada e vai trazer o atestado médico na semana "
                + "seguinte para pedir a remarcação da perícia junto ao INSS.");

        try (XSSFWorkbook planilha = ler(AgendaPericiasExcel.gerar("Outubro de 2026", List.of(realizada, faltou)))) {
            XSSFSheet aba = planilha.getSheet(AgendaPericiasExcel.ABA);
            assertThat(textoEFundo(aba.getRow(1).getCell(8))).containsExactly("Realizada", "C6EFCE");
            assertThat(textoEFundo(aba.getRow(2).getCell(8))).containsExactly("Faltou", "FFC7CE");

            XSSFCell observacao = aba.getRow(2).getCell(9);
            assertThat(observacao.getCellStyle().getWrapText()).isTrue();
            assertThat(aba.getRow(2).getHeightInPoints()).isGreaterThanOrEqualTo(2 * 21f);
        }
    }

    @Test
    void semPericiasGeraCabecalhoETotalZerado() throws IOException {
        try (XSSFWorkbook planilha = ler(AgendaPericiasExcel.gerar("Outubro de 2026", List.of()))) {
            XSSFSheet aba = planilha.getSheet(AgendaPericiasExcel.ABA);
            assertThat(aba.getLastRowNum()).isEqualTo(1);
            assertThat(aba.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Total (0 perícias)");
            assertThat(aba.getTables()).isEmpty();
        }
    }

    private Pericia pericia(String nome, String cpf, LocalDate data, LocalTime horario) {
        Cliente cliente = new Cliente(nome, data);
        cliente.setCpf(cpf);
        Pericia pericia = new Pericia(cliente, tipo, escritorio);
        pericia.setData(data);
        pericia.setHorario(horario);
        return pericia;
    }

    private static List<String> textoEFundo(XSSFCell celula) {
        return List.of(celula.getStringCellValue(),
                HexFormat.of().withUpperCase().formatHex(celula.getCellStyle().getFillForegroundColorColor().getRGB()));
    }

    private static XSSFWorkbook ler(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }
}
