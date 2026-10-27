package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.ResponsavelPagamento;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.security.UsuariosDeTeste;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.PagamentoService;
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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private AuditoriaService auditoriaService;
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
    void todoOPeriodoMostraQualquerDataEABuscaPeloCpfFiltraSoOCliente() throws Exception {
        salvarPagamento(segunda.minusMonths(3), "Cliente Antigo Todo Período", "11144477735");
        salvarPagamento(segunda.plusMonths(2), "Cliente Futuro Todo Período", "39053344705");

        mvc.perform(agendaNoPeriodo("TODOS"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Todas as datas"),
                        containsString("Cliente Antigo Todo Período"),
                        containsString("Cliente Futuro Todo Período"),
                        containsString("data-sugerir-cliente"),
                        not(containsString("Semana anterior")))));

        mvc.perform(agendaNoPeriodo("TODOS").param("busca", "111.444.777-35"))
                .andExpect(content().string(allOf(
                        containsString("Cliente Antigo Todo Período"),
                        not(containsString("Cliente Futuro Todo Período")))));

        mvc.perform(get("/agenda-pagamentos/planilha.xlsx").param("periodo", "TODOS").with(operador()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("agenda-pagamentos-todas-as-datas.xlsx")));
    }

    @Test
    void periodoMesFiltraPeloMesEscolhidoSemDiaDeReferencia() throws Exception {
        LocalDate quarta = segunda.plusDays(2);
        salvarPagamento(quarta);
        YearMonth mes = YearMonth.from(quarta);

        mvc.perform(agendaNoPeriodo("MES").param("mes", mes.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Salvo Agenda"),
                        containsString("type=\"month\" id=\"mes\" name=\"mes\" class=\"form-control\" value=\"" + mes + "\""))));
        mvc.perform(agendaNoPeriodo("MES").param("mes", mes.plusMonths(1).toString()))
                .andExpect(content().string(allOf(
                        not(containsString("Cliente Salvo Agenda")),
                        containsString("value=\"" + mes.plusMonths(1) + "\""))));

        mvc.perform(get("/agenda-pagamentos/planilha.xlsx").param("periodo", "MES").param("mes", mes.toString())
                        .with(operador()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsString("agenda-pagamentos-" + mes + ".xlsx")));
    }

    @Test
    void salvarELancarOutroVoltaParaOFormularioEmBrancoNoMesmoDia() throws Exception {
        LocalDate quarta = segunda.plusDays(2);

        mvc.perform(get("/agenda-pagamentos/novo").with(operador()))
                .andExpect(content().string(containsString("Salvar e lançar outro")));

        mvc.perform(novoPagamento("Cliente Lança Outro", CPF, quarta, "1.000,00").param("acao", "novo"))
                .andExpect(redirectedUrl("/agenda-pagamentos/novo?data=" + quarta))
                .andExpect(flash().attribute("sucesso",
                        "Pagamento de 'Cliente Lança Outro' lançado. Preencha o próximo pagamento."));
        assertThat(pagamentoDoCliente("Cliente Lança Outro").getDataRecebimento()).isEqualTo(quarta);

        mvc.perform(get("/agenda-pagamentos/novo").param("data", quarta.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("value=\"" + quarta + "\""),
                        not(containsString("value=\"Cliente Lança Outro\"")))));
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
    void sugereClientesAtivosPorNomeOuCpfEnquantoDigita() throws Exception {
        Cliente comCpf = salvarCliente("Maria Sugestão Teste", "11144477735");
        salvarCliente("Mário Sugestão Teste", null);
        Cliente inativo = salvarCliente("Marta Sugestão Inativa", "39053344705");
        inativo.setAtivo(false);
        clienteRepository.flush();

        mvc.perform(get("/clientes/sugestoes").param("termo", "sugestão").with(operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(comCpf.getId()))
                .andExpect(jsonPath("$[0].nome").value("Maria Sugestão Teste"))
                .andExpect(jsonPath("$[0].cpf").value(CPF))
                .andExpect(jsonPath("$[1].nome").value("Mário Sugestão Teste"))
                .andExpect(jsonPath("$[1].cpf").doesNotExist());

        mvc.perform(get("/clientes/sugestoes").param("termo", "111.444").with(operador()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Maria Sugestão Teste"));

        mvc.perform(get("/clientes/sugestoes").param("termo", "m").with(operador()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sugestoesDosFiltrosIncluemClientesInativos() throws Exception {
        salvarCliente("Maria Sugestão Teste", "11144477735");
        Cliente inativo = salvarCliente("Marta Sugestão Inativa", "39053344705");
        inativo.setAtivo(false);
        clienteRepository.flush();

        mvc.perform(get("/clientes/sugestoes").param("termo", "sugestão").param("todos", "true").with(operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.nome == 'Marta Sugestão Inativa')].ativo", contains(false)))
                .andExpect(jsonPath("$[?(@.nome == 'Maria Sugestão Teste')].ativo", contains(true)));
    }

    @Test
    void lancaParaOClienteEscolhidoNaBuscaSemCadastrarDeNovo() throws Exception {
        Cliente cliente = salvarCliente("Cliente Escolhido Teste", "11144477735");
        long clientesAntes = clienteRepository.count();

        mvc.perform(get("/agenda-pagamentos/novo").param("clienteId", cliente.getId().toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("value=\"Cliente Escolhido Teste\""),
                        containsString("value=\"" + CPF + "\""),
                        not(containsString("<span data-aviso-sem-cpf>")))));

        mvc.perform(novoPagamento("Cliente Escolhido Teste", CPF, segunda, "1.000,00")
                        .param("clienteId", cliente.getId().toString()))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));

        assertThat(clienteRepository.count()).isEqualTo(clientesAntes);
        assertThat(pagamentoRepository.findByClienteIdOrderByDataRecebimentoDescIdDesc(cliente.getId())).hasSize(1);
    }

    @Test
    void clienteEscolhidoSemCpfTemOCadastroCompletado() throws Exception {
        Cliente semCpf = salvarCliente("Cliente Sem CPF Teste", null);

        mvc.perform(get("/agenda-pagamentos/novo").param("clienteId", semCpf.getId().toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("value=\"Cliente Sem CPF Teste\""),
                        containsString("href=\"/clientes/" + semCpf.getId() + "\""),
                        containsString("<span data-aviso-sem-cpf>"))));

        mvc.perform(novoPagamento("Cliente Sem CPF Teste", "390.533.447-05", segunda, "800,00")
                        .param("clienteId", semCpf.getId().toString()))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));

        assertThat(semCpf.getCpf()).isEqualTo("39053344705");
        assertThat(pagamentoRepository.findByClienteIdOrderByDataRecebimentoDescIdDesc(semCpf.getId())).hasSize(1);
    }

    @Test
    void clienteEscolhidoSemCpfRecusaCpfDeOutroCliente() throws Exception {
        salvarCliente("Dono do CPF Teste", "39053344705");
        Cliente semCpf = salvarCliente("Cliente Sem CPF Teste", null);

        mvc.perform(novoPagamento("Cliente Sem CPF Teste", "390.533.447-05", segunda, "800,00")
                        .param("clienteId", semCpf.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "cpf"))
                .andExpect(content().string(containsString("Este CPF já pertence a")));

        assertThat(semCpf.getCpf()).isNull();
    }

    @Test
    void camposObrigatoriosVazios() throws Exception {
        mvc.perform(post("/agenda-pagamentos").with(operador()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nomeCliente", "cpf", "responsavelIds",
                        "tipoServicoId", "dataRecebimento"))
                .andExpect(model().errorCount(5));
    }

    @Test
    void salvaSemValorESemBancoQuandoAindaNaoSeSabe() throws Exception {
        mvc.perform(post("/agenda-pagamentos").with(operador()).with(csrf())
                        .param("nomeCliente", "Cliente Sem Valor Teste")
                        .param("cpf", CPF)
                        .param("responsavelIds", responsavel.getId().toString())
                        .param("tipoServicoId", tipo.getId().toString())
                        .param("dataRecebimento", segunda.toString())
                        .param("valor", "")
                        .param("banco", "")
                        .param("status", "AGENDADO"))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));

        PagamentoAgendado pagamento = pagamentoDoCliente("Cliente Sem Valor Teste");
        assertThat(pagamento.getValor()).isNull();
        assertThat(pagamento.getBanco()).isNull();

        mvc.perform(get("/agenda-pagamentos").param("data", segunda.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cliente Sem Valor Teste")));
        mvc.perform(get("/agenda-pagamentos/planilha.xlsx").param("data", segunda.toString()).with(operador()))
                .andExpect(status().isOk());
    }

    @Test
    void marcaQueOClienteRecebeuPelaLinhaEVoltaParaAMesmaSemana() throws Exception {
        PagamentoAgendado pagamento = salvarPagamento(segunda);
        String voltar = "/agenda-pagamentos?data=" + segunda;

        mvc.perform(post("/agenda-pagamentos/{id}/status", pagamento.getId()).with(operador()).with(csrf())
                        .param("status", "RECEBIDO")
                        .param("voltar", voltar))
                .andExpect(redirectedUrl(voltar))
                .andExpect(flash().attribute("sucesso",
                        "\"Cliente recebeu?\" do pagamento de 'Cliente Salvo Agenda' alterado para Recebeu."));
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.RECEBIDO);

        mvc.perform(post("/agenda-pagamentos/{id}/status", pagamento.getId()).with(operador()).with(csrf())
                        .param("status", "AGENDADO")
                        .param("voltar", "//exemplo.com"))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.AGENDADO);
    }

    @Test
    void situacaoPendenteNaoExisteMais() throws Exception {
        PagamentoAgendado pagamento = salvarPagamento(segunda);

        mvc.perform(post("/agenda-pagamentos/{id}/status", pagamento.getId()).with(operador()).with(csrf())
                        .param("status", "PENDENTE"))
                .andExpect(status().isBadRequest());
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.AGENDADO);

        mvc.perform(get("/agenda-pagamentos").param("data", segunda.toString()).with(operador()))
                .andExpect(content().string(allOf(
                        containsString("Cliente recebeu?"),
                        containsString("Recebeu"),
                        not(containsString("Pendente / não pago")))));
    }

    @Test
    void colunaHonorarioMostraSeOClienteJaPagouOEscritorio() throws Exception {
        PagamentoAgendado quitado = salvarPagamento(segunda, "Cliente Quitou Teste", "11144477735");
        quitado.setValorHonorario(new BigDecimal("450.00"));
        quitado.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);
        PagamentoAgendado aReceber = salvarPagamento(segunda, "Cliente Deve Teste", "39053344705");
        aReceber.setValorHonorario(new BigDecimal("300.00"));
        salvarPagamento(segunda, "Cliente Sem Honorário Teste", "52998224725");
        pagamentoRepository.flush();

        mvc.perform(get("/agenda-pagamentos").param("data", segunda.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("<th>Honorário</th>"),
                        containsString("href=\"/honorarios/" + quitado.getId() + "\""),
                        containsString("href=\"/honorarios/" + aReceber.getId() + "\""),
                        containsString(">Recebido</a>"),
                        containsString(">A receber</a>"),
                        containsString(">Sem honorário</span>"),
                        containsString("badge text-decoration-none selo-sucesso"),
                        containsString("badge text-decoration-none selo-pendente"),
                        containsString("select-situacao selo-andamento"),
                        containsString("data-classe=\"selo-sucesso\""))));
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
    void agendaNaoMostraValoresESemPermissaoDeHonorariosOFormularioNaoAlteraHonorarioNemRepasse() throws Exception {
        PagamentoAgendado pagamento = salvarPagamento(segunda);
        pagamento.setValorHonorario(new BigDecimal("321.00"));
        ResponsavelPagamento repasse = pagamento.getResponsaveis().get(0);
        repasse.setValorRepasse(new BigDecimal("87.00"));
        pagamentoRepository.flush();

        mvc.perform(get("/agenda-pagamentos").param("data", segunda.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Salvo Agenda"),
                        not(containsString("id=\"honorario-")),
                        not(containsString("id=\"repasse-")),
                        not(containsString("321,00")),
                        not(containsString("87,00")))));

        mvc.perform(get("/agenda-pagamentos/novo").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("name=\"valorHonorario\""),
                        not(containsString("name=\"valorRepasse\"")))));

        mvc.perform(get("/agenda-pagamentos/novo").with(semLancarHonorarios()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        not(containsString("Honorário do escritório")),
                        not(containsString("name=\"valorHonorario\"")),
                        not(containsString("name=\"valorRepasse\"")))));

        mvc.perform(formulario(post("/agenda-pagamentos/{id}", pagamento.getId()), semLancarHonorarios(),
                        "Cliente Salvo Agenda", "11144477735", segunda, "1.500,00")
                        .param("valorHonorario", "999,00")
                        .param("situacaoHonorario", "RECEBIDO")
                        .param("valorRepasse", ""))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));
        assertThat(pagamento.getValorHonorario()).isEqualByComparingTo("321.00");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.A_RECEBER);
        assertThat(repasse.getValorRepasse()).isEqualByComparingTo("87.00");
    }

    @Test
    void cadastroJaLancaHonorarioRecebidoQueApareceEmHonorariosENoHistorico() throws Exception {
        mvc.perform(novoPagamento("Cliente Já Pagou Escritório", CPF, segunda, "1.500,00")
                        .param("valorHonorario", "450,00")
                        .param("situacaoHonorario", "RECEBIDO"))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));

        PagamentoAgendado pagamento = pagamentoDoCliente("Cliente Já Pagou Escritório");
        assertThat(pagamento.getValorHonorario()).isEqualByComparingTo("450.00");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.RECEBIDO);

        mvc.perform(get("/honorarios").param("periodo", "MES").param("data", segunda.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Já Pagou Escritório"),
                        containsString("450,00"))));

        mvc.perform(get("/agenda-pagamentos/{id}/editar", pagamento.getId()).with(operador()))
                .andExpect(content().string(allOf(
                        containsString("honorário R$ 450,00 (Recebido)"),
                        containsString("value=\"450,00\""))));
    }

    @Test
    void edicaoPeloFormularioDaAgendaAlteraOHonorario() throws Exception {
        PagamentoAgendado pagamento = salvarPagamento(segunda);
        pagamentoRepository.flush();

        mvc.perform(formulario(post("/agenda-pagamentos/{id}", pagamento.getId()),
                        "Cliente Salvo Agenda", "11144477735", segunda, "1.500,00")
                        .param("valorHonorario", "200,00")
                        .param("situacaoHonorario", "A_RECEBER"))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));

        assertThat(pagamento.getValorHonorario()).isEqualByComparingTo("200.00");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.A_RECEBER);
        mvc.perform(get("/agenda-pagamentos/{id}/editar", pagamento.getId()).with(operador()))
                .andExpect(content().string(containsString("honorário de sem valor para R$ 200,00")));
    }

    @Test
    void honorarioRecebidoSemValorVoltaComErroSemLancar() throws Exception {
        mvc.perform(novoPagamento("Cliente Recebido Sem Valor", CPF, segunda, "1.500,00")
                        .param("valorHonorario", "")
                        .param("situacaoHonorario", "RECEBIDO"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "valorHonorario"))
                .andExpect(content().string(containsString("Informe o valor do honorário recebido.")));

        assertThat(pagamentoRepository.findAll())
                .noneMatch(p -> p.getCliente().getNome().equals("Cliente Recebido Sem Valor"));
    }

    @Test
    void comNegociacaoAtivaOHonorarioFicaSoParaLeituraNoFormulario() throws Exception {
        PagamentoAgendado pagamento = salvarPagamento(segunda);
        pagamento.setValorHonorario(new BigDecimal("900.00"));
        pagamentoRepository.flush();
        LocalDate hoje = LocalDate.now(relogio);
        mvc.perform(post("/honorarios/{id}/negociar", pagamento.getId()).with(operador()).with(csrf())
                        .param("valorNegociado", "900,00")
                        .param("quantidade", "1")
                        .param("primeiroVencimento", hoje.toString())
                        .param("parcelas[0].vencimento", hoje.toString())
                        .param("parcelas[0].valor", "900,00"))
                .andExpect(redirectedUrl("/honorarios/" + pagamento.getId()));
        SituacaoHonorarioPagamento negociada = pagamento.getSituacaoHonorario();

        mvc.perform(get("/agenda-pagamentos/{id}/editar", pagamento.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Este honorário tem uma negociação"),
                        containsString("href=\"/honorarios/" + pagamento.getId() + "\""),
                        not(containsString("name=\"valorHonorario\"")))));

        mvc.perform(formulario(post("/agenda-pagamentos/{id}", pagamento.getId()),
                        "Cliente Salvo Agenda", "11144477735", segunda, "1.500,00")
                        .param("valorHonorario", "1,00")
                        .param("situacaoHonorario", "RECEBIDO"))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));
        assertThat(pagamento.getValorHonorario()).isEqualByComparingTo("900.00");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(negociada);
    }

    @Test
    void lancaComVariosResponsaveisEEdicaoIncluiERemoveRegistrandoNoHistorico() throws Exception {
        Responsavel bruno = responsavelRepository.save(new Responsavel("Bruno Captação Teste", null));
        Responsavel carla = responsavelRepository.save(new Responsavel("Carla Captação Teste", null));

        mvc.perform(novoPagamento("Cliente Compartilhado Teste", CPF, segunda, "1.000,00")
                        .param("responsavelIds", bruno.getId().toString()))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));
        PagamentoAgendado pagamento = pagamentoDoCliente("Cliente Compartilhado Teste");
        assertThat(pagamento.getNomesResponsaveis())
                .isEqualTo("Bruno Captação Teste, Responsável Agenda Pagamento");

        mvc.perform(get("/agenda-pagamentos").param("data", segunda.toString()).with(operador()))
                .andExpect(content().string(containsString("Bruno Captação Teste, Responsável Agenda Pagamento")));

        mvc.perform(get("/agenda-pagamentos/{id}/editar", pagamento.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("id=\"responsavelIds-" + bruno.getId() + "\" checked"),
                        containsString("id=\"responsavelIds-" + responsavel.getId() + "\" checked"),
                        not(containsString("id=\"responsavelIds-" + carla.getId() + "\" checked")))));

        mvc.perform(post("/agenda-pagamentos/{id}", pagamento.getId()).with(operador()).with(csrf())
                        .param("nomeCliente", "Cliente Compartilhado Teste")
                        .param("cpf", CPF)
                        .param("responsavelIds", bruno.getId().toString(), carla.getId().toString())
                        .param("tipoServicoId", tipo.getId().toString())
                        .param("dataRecebimento", segunda.toString())
                        .param("valor", "1.000,00")
                        .param("banco", "Caixa Econômica Federal")
                        .param("status", "AGENDADO"))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + segunda));
        assertThat(pagamento.getNomesResponsaveis()).isEqualTo("Bruno Captação Teste, Carla Captação Teste");

        mvc.perform(get("/agenda-pagamentos/{id}/editar", pagamento.getId()).with(operador()))
                .andExpect(content().string(containsString(
                        "responsáveis: incluído &#39;Carla Captação Teste&#39;, removido &#39;Responsável Agenda Pagamento&#39;")));
    }

    @Test
    void filtroPorResponsavelEncontraPagamentoCompartilhado() throws Exception {
        Responsavel bruno = responsavelRepository.save(new Responsavel("Bruno Filtro Teste", null));
        Responsavel carla = responsavelRepository.save(new Responsavel("Carla Filtro Teste", null));
        PagamentoAgendado pagamento = salvarPagamento(segunda);
        pagamento.adicionarResponsavel(bruno);
        pagamentoRepository.flush();

        mvc.perform(agendaNoPeriodo("SEMANA").param("data", segunda.toString())
                        .param("responsavelId", bruno.getId().toString()))
                .andExpect(content().string(containsString("Cliente Salvo Agenda")));
        mvc.perform(agendaNoPeriodo("SEMANA").param("data", segunda.toString())
                        .param("responsavelId", responsavel.getId().toString()))
                .andExpect(content().string(containsString("Cliente Salvo Agenda")));
        mvc.perform(agendaNoPeriodo("SEMANA").param("data", segunda.toString())
                        .param("responsavelId", carla.getId().toString()))
                .andExpect(content().string(not(containsString("Cliente Salvo Agenda"))));
    }

    @Test
    void exportaExcelDaSemanaFiltrada() throws Exception {
        PagamentoAgendado pagamento = salvarPagamento(segunda.plusDays(4));
        pagamento.setValorHonorario(new BigDecimal("450.00"));
        pagamento.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);
        pagamentoRepository.flush();

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
            assertThat(aba.getRow(1).getCell(AgendaPagamentosExcel.CABECALHO.indexOf("HONORÁRIO"))
                    .getStringCellValue()).isEqualTo("Recebido");
            assertThat(aba.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Total (1 pagamento)");
            assertThat(aba.getPrintSetup().getLandscape()).isTrue();
            assertThat(aba.getPrintSetup().getFitWidth()).isEqualTo((short) 1);
        }
    }

    @Test
    void excluiPagamentoLancadoEmDuplicidadeEGuardaNaAuditoria() throws Exception {
        LocalDate quarta = segunda.plusDays(2);
        PagamentoAgendado pagamento = salvarPagamento(quarta);
        Long id = pagamento.getId();

        mvc.perform(get("/agenda-pagamentos/{id}/editar", id).with(operador()))
                .andExpect(content().string(allOf(
                        containsString("Excluir pagamento"),
                        containsString("/agenda-pagamentos/" + id + "/excluir"))));

        mvc.perform(post("/agenda-pagamentos/{id}/excluir", id).with(operador()).with(csrf())
                        .param("motivo", "Lançado em duplicidade"))
                .andExpect(redirectedUrl("/agenda-pagamentos?data=" + quarta))
                .andExpect(flash().attribute("sucesso",
                        "Pagamento de 'Cliente Salvo Agenda' excluído. O registro fica na auditoria."));

        assertThat(pagamentoRepository.findById(id)).isEmpty();
        assertThat(auditoriaService.historico(PagamentoService.ENTIDADE, id))
                .anyMatch(a -> a.getAcao() == AcaoAuditoria.EXCLUSAO
                        && a.getDescricao().contains("Motivo: Lançado em duplicidade"));
        mvc.perform(agendaNoPeriodo("DIA").param("data", quarta.toString()))
                .andExpect(content().string(not(containsString("Cliente Salvo Agenda"))));
    }

    @Test
    void pagamentoComHonorarioRecebidoNaoPodeSerExcluido() throws Exception {
        PagamentoAgendado pagamento = salvarPagamento(segunda);
        pagamento.setValorHonorario(new BigDecimal("500.00"));
        pagamento.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);
        Long id = pagamento.getId();

        mvc.perform(get("/agenda-pagamentos/{id}/editar", id).with(operador()))
                .andExpect(content().string(allOf(
                        containsString("Para excluir, volte o honorário para"),
                        not(containsString("/agenda-pagamentos/" + id + "/excluir")))));

        mvc.perform(post("/agenda-pagamentos/{id}/excluir", id).with(operador()).with(csrf())
                        .param("motivo", "Lançado em duplicidade"))
                .andExpect(redirectedUrl("/agenda-pagamentos/" + id + "/editar"))
                .andExpect(flash().attributeExists("erro"));
        assertThat(pagamentoRepository.findById(id)).isPresent();
    }

    @Test
    void quemNaoTemPermissaoNaoExcluiPagamento() throws Exception {
        PagamentoAgendado pagamento = salvarPagamento(segunda);
        Set<Permissao> permissoes = EnumSet.copyOf(UsuariosDeTeste.PERMISSOES_OPERADOR);
        permissoes.remove(Permissao.AGENDA_EXCLUIR);
        RequestPostProcessor semExcluir = UsuariosDeTeste.comPermissoes("ana", permissoes);

        mvc.perform(get("/agenda-pagamentos/{id}/editar", pagamento.getId()).with(semExcluir))
                .andExpect(content().string(not(containsString("Excluir pagamento"))));
        mvc.perform(post("/agenda-pagamentos/{id}/excluir", pagamento.getId()).with(semExcluir).with(csrf())
                        .param("motivo", "Lançado em duplicidade"))
                .andExpect(status().isForbidden());
        assertThat(pagamentoRepository.findById(pagamento.getId())).isPresent();
    }

    private MockHttpServletRequestBuilder agendaNoPeriodo(String periodo) {
        return get("/agenda-pagamentos").param("periodo", periodo).with(operador());
    }

    private MockHttpServletRequestBuilder novoPagamento(String nome, String cpf, LocalDate data, String valor) {
        return formulario(post("/agenda-pagamentos"), nome, cpf, data, valor);
    }

    private MockHttpServletRequestBuilder formulario(MockHttpServletRequestBuilder requisicao, String nome,
                                                     String cpf, LocalDate data, String valor) {
        return formulario(requisicao, operador(), nome, cpf, data, valor);
    }

    private MockHttpServletRequestBuilder formulario(MockHttpServletRequestBuilder requisicao,
                                                     RequestPostProcessor usuario, String nome,
                                                     String cpf, LocalDate data, String valor) {
        return requisicao.with(usuario).with(csrf())
                .param("nomeCliente", nome)
                .param("cpf", cpf)
                .param("responsavelIds", responsavel.getId().toString())
                .param("tipoServicoId", tipo.getId().toString())
                .param("dataRecebimento", data.toString())
                .param("valor", valor)
                .param("banco", "Caixa Econômica Federal")
                .param("status", "AGENDADO");
    }

    private PagamentoAgendado salvarPagamento(LocalDate data) {
        return salvarPagamento(data, "Cliente Salvo Agenda", "11144477735");
    }

    private Cliente salvarCliente(String nome, String cpf) {
        Cliente cliente = new Cliente(nome, segunda);
        cliente.setCpf(cpf);
        return clienteRepository.save(cliente);
    }

    private PagamentoAgendado salvarPagamento(LocalDate data, String nome, String cpf) {
        Cliente cliente = salvarCliente(nome, cpf);
        return pagamentoRepository.save(new PagamentoAgendado(cliente, List.of(responsavel), tipo, data,
                new BigDecimal("1500.00"), "Banco do Brasil"));
    }

    private PagamentoAgendado pagamentoDoCliente(String nome) {
        return pagamentoRepository.findAll().stream()
                .filter(p -> p.getCliente().getNome().equals(nome))
                .findFirst().orElseThrow();
    }

    private static RequestPostProcessor operador() {
        return UsuariosDeTeste.operador();
    }

    private static RequestPostProcessor semLancarHonorarios() {
        Set<Permissao> permissoes = EnumSet.copyOf(UsuariosDeTeste.PERMISSOES_OPERADOR);
        permissoes.remove(Permissao.HONORARIOS_EDITAR);
        return UsuariosDeTeste.comPermissoes("ana", permissoes);
    }
}
