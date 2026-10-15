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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(redirectedUrl("/agenda-pagamentos?semana=" + quarta));

        Cliente cliente = clienteRepository.findByCpf("11144477735").orElseThrow();
        assertThat(cliente.getNome()).isEqualTo("Cliente Agenda Teste");

        mvc.perform(get("/agenda-pagamentos").param("semana", segunda.toString()).with(operador()))
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

        mvc.perform(get("/agenda-pagamentos").param("semana", segunda.plusWeeks(1).toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Nenhum pagamento nesta semana."),
                        not(containsString("Cliente Agenda Teste")))));
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
        String voltar = "/agenda-pagamentos?semana=" + segunda;

        mvc.perform(post("/agenda-pagamentos/{id}/status", pagamento.getId()).with(operador()).with(csrf())
                        .param("status", "PAGO")
                        .param("voltar", voltar))
                .andExpect(redirectedUrl(voltar));
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.PAGO);

        mvc.perform(post("/agenda-pagamentos/{id}/status", pagamento.getId()).with(operador()).with(csrf())
                        .param("status", "PENDENTE")
                        .param("voltar", "//exemplo.com"))
                .andExpect(redirectedUrl("/agenda-pagamentos?semana=" + segunda));
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
    void exportaCsvDaSemanaFiltrada() throws Exception {
        salvarPagamento(segunda.plusDays(4));

        MvcResult resultado = mvc.perform(get("/agenda-pagamentos/planilha.csv")
                        .param("semana", segunda.toString()).with(operador()))
                .andExpect(status().isOk())
                .andReturn();
        String csv = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv)
                .contains("Cliente;CPF;Responsável;Dia de recebimento;Benefício;Valor;Banco;Status")
                .contains("Cliente Salvo Agenda;111.444.777-35;Responsável Agenda Pagamento;"
                        + DATA.format(segunda.plusDays(4)) + ";Benefício Agenda Pagamento;1500,00;Banco do Brasil;Agendado");
        assertThat(resultado.getResponse().getHeader("Content-Disposition"))
                .contains("agenda-pagamentos-" + segunda + ".csv");
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
