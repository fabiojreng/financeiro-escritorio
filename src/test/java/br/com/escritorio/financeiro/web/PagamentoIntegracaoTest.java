package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PagamentoIntegracaoTest extends IntegracaoTest {

    private static final String CPF = "111.444.777-35";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Clock relogio;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ResponsavelRepository responsavelRepository;
    @Autowired
    private TipoServicoRepository tipoServicoRepository;
    @Autowired
    private PagamentoAgendadoRepository pagamentoRepository;

    private LocalDate segunda;
    private Responsavel responsavel;
    private TipoServico tipo;

    @BeforeEach
    void setUp() {
        segunda = LocalDate.now(relogio).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        responsavel = responsavelRepository.save(new Responsavel("Responsável Agenda Pagamento", null));
        tipo = tipoServicoRepository.save(new TipoServico("Benefício Agenda Pagamento", null));
    }

    @Test
    void lancaPagamentoCadastrandoClienteEMostraAsSeteColunasNaSemanaCerta() throws Exception {
        LocalDate quarta = segunda.plusDays(2);

        mvc.perform(novoPagamento("Cliente Agenda Teste", CPF, quarta, "1.234,56"))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + quarta));

        Cliente cliente = clienteRepository.findByCpf("11144477735").orElseThrow();
        assertThat(cliente.getNome()).isEqualTo("Cliente Agenda Teste");

        mvc.perform(get("/agenda-pagamentos").param("data", segunda.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Agenda Teste"),
                        containsString(CPF),
                        containsString("Responsável Agenda Pagamento"),
                        containsString(DATA.format(quarta)),
                        containsString("Benefício Agenda Pagamento"),
                        containsString("1.234,56"),
                        containsString("Caixa Econômica Federal"),
                        containsString("Agendado"))));

        mvc.perform(get("/agenda-pagamentos").param("data", segunda.plusWeeks(1).toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Nenhum pagamento neste período."),
                        not(containsString("Cliente Agenda Teste")))));
    }

    @Test
    void filtraPorDiaMesEPeriodoPersonalizado() throws Exception {
        LocalDate quarta = segunda.plusDays(2);
        salvarPagamento(quarta);

        mvc.perform(agendaNoPeriodo("DIA").param("data", quarta.toString()))
                .andExpect(content().string(containsString("Cliente Salvo Agenda")));
        mvc.perform(agendaNoPeriodo("DIA").param("data", quarta.plusDays(1).toString()))
                .andExpect(content().string(allOf(
                        containsString("Nenhum pagamento neste período."),
                        containsString("Próximo dia"))));

        mvc.perform(agendaNoPeriodo("MES").param("data", quarta.withDayOfMonth(1).toString()))
                .andExpect(content().string(allOf(
                        containsString("Cliente Salvo Agenda"),
                        containsString("Mês anterior"))));
        mvc.perform(agendaNoPeriodo("MES").param("data", quarta.plusMonths(1).toString()))
                .andExpect(content().string(not(containsString("Cliente Salvo Agenda"))));

        mvc.perform(agendaNoPeriodo("PERSONALIZADO").param("de", quarta.toString()))
                .andExpect(content().string(allOf(
                        containsString("A partir de " + DATA.format(quarta)),
                        containsString("Cliente Salvo Agenda"))));
        mvc.perform(agendaNoPeriodo("PERSONALIZADO")
                        .param("de", quarta.plusDays(10).toString())
                        .param("ate", quarta.plusDays(1).toString()))
                .andExpect(content().string(allOf(
                        containsString("De " + DATA.format(quarta.plusDays(1)) + " a " + DATA.format(quarta.plusDays(10))),
                        not(containsString("Cliente Salvo Agenda")))));
        mvc.perform(agendaNoPeriodo("PERSONALIZADO"))
                .andExpect(content().string(allOf(
                        containsString("Todas as datas"),
                        containsString("Cliente Salvo Agenda"))));
    }

    @Test
    void segundoPagamentoDoMesmoCpfUsaOClienteJaCadastrado() throws Exception {
        mvc.perform(novoPagamento("Cliente Agenda Teste", CPF, segunda, "1.000,00"));
        mvc.perform(novoPagamento("cliente agenda teste", "11144477735", segunda.plusDays(1), "500,00"));

        Cliente cliente = clienteRepository.findByCpf("11144477735").orElseThrow();
        assertThat(pagamentoRepository.findAll())
                .filteredOn(p -> p.getCliente().getId().equals(cliente.getId()))
                .hasSize(2);
    }

    @Test
    void cpfDeOutroClienteVoltaAoFormularioComONomeCadastrado() throws Exception {
        Cliente outro = new Cliente("Pessoa Já Cadastrada", segunda);
        outro.setCpf("11144477735");
        clienteRepository.save(outro);

        mvc.perform(novoPagamento("Cliente Agenda Teste", CPF, segunda, "1.000,00"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "cpf"))
                .andExpect(content().string(containsString("Este CPF já pertence a")));

        assertThat(pagamentoRepository.findAll()).noneMatch(p -> p.getCliente().getId().equals(outro.getId()));
    }

    @Test
    void camposObrigatoriosVazios() throws Exception {
        mvc.perform(post("/agenda-pagamentos").with(operador()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nomeCliente", "cpf", "responsavelId",
                        "tipoServicoId", "dataRecebimento", "valor", "banco"));
    }

    @Test
    void mudaStatusPelaLinhaEVoltaParaAMesmaSemana() throws Exception {
        PagamentoAgendado pagamento = salvarPagamento(segunda);
        String voltar = "/agenda-pagamentos?data=" + segunda;

        mvc.perform(post("/agenda-pagamentos/{id}/status", pagamento.getId()).with(operador()).with(csrf())
                        .param("status", "PAGO")
                        .param("voltar", voltar))
                .andExpect(redirectedUrl(voltar));
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PAGO);

        mvc.perform(post("/agenda-pagamentos/{id}/status", pagamento.getId()).with(operador()).with(csrf())
                        .param("status", "PENDENTE")
                        .param("voltar", "//exemplo.com"))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
    }

    @Test
    void edicaoMostraHistoricoDoPagamento() throws Exception {
        mvc.perform(novoPagamento("Cliente Agenda Teste", CPF, segunda, "1.000,00"));
        PagamentoAgendado pagamento = pagamentoRepository.findAll().stream()
                .filter(p -> p.getCliente().getNome().equals("Cliente Agenda Teste"))
                .findFirst().orElseThrow();

        mvc.perform(get("/agenda-pagamentos/{id}/editar", pagamento.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Histórico deste pagamento"),
                        containsString("valor R$ 1.000,00"),
                        containsString(CPF))));
    }

    @Test
    void exportaExcelDaSemanaFiltrada() throws Exception {
        salvarPagamento(segunda.plusDays(4));

        MvcResult resultado = mvc.perform(get("/agenda-pagamentos/planilha.xlsx")
                        .param("data", segunda.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition",
                        containsString("agenda-pagamentos-" + segunda + ".xlsx")))
                .andReturn();

        try (XSSFWorkbook planilha = new XSSFWorkbook(
                new ByteArrayInputStream(resultado.getResponse().getContentAsByteArray()))) {
            XSSFSheet aba = planilha.getSheet(AgendaPagamentosExcel.ABA);
            List<String> cabecalho = new ArrayList<>();
            aba.getRow(0).forEach(c -> cabecalho.add(c.getStringCellValue()));
            assertThat(cabecalho).containsExactlyElementsOf(AgendaPagamentosExcel.CABECALHO);
            assertThat(aba.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Cliente Salvo Agenda");
            assertThat(aba.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(1500.00);
            assertThat(aba.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Total (1 pagamento)");
            assertThat(aba.getPrintSetup().getLandscape()).isTrue();
            assertThat(aba.getPrintSetup().getFitWidth()).isEqualTo((short) 1);
        }
    }

    private MockHttpServletRequestBuilder agendaNoPeriodo(String periodo) {
        return get("/agenda-pagamentos").param("periodo", periodo).with(operador());
    }

    private MockHttpServletRequestBuilder novoPagamento(String nome, String cpf, LocalDate data, String valor) {
        return post("/agenda-pagamentos").with(operador()).with(csrf())
                .param("nomeCliente", nome)
                .param("cpf", cpf)
                .param("responsavelId", responsavel.getId().toString())
                .param("tipoServicoId", tipo.getId().toString())
                .param("dataRecebimento", data.toString())
                .param("valor", valor)
                .param("banco", "Caixa Econômica Federal")
                .param("status", "AGENDADO");
    }

    private PagamentoAgendado salvarPagamento(LocalDate data) {
        Cliente cliente = new Cliente("Cliente Salvo Agenda", segunda);
        cliente.setCpf("11144477735");
        clienteRepository.save(cliente);
        return pagamentoRepository.save(new PagamentoAgendado(cliente, responsavel, tipo, data,
                new BigDecimal("1500.00"), "Banco do Brasil"));
    }

    private static RequestPostProcessor operador() {
        return user("ana").roles("OPERADOR");
    }
}
