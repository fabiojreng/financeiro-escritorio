package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.AuditoriaRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.service.RelatorioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class RelatorioIntegracaoTest extends IntegracaoTest {

    private static final String CPF = "52998224725";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private TipoServicoRepository tipoServicoRepository;
    @Autowired
    private ResponsavelRepository responsavelRepository;
    @Autowired
    private PagamentoAgendadoRepository pagamentoRepository;
    @Autowired
    private AuditoriaRepository auditoriaRepository;

    private TipoServico tipo;
    private Responsavel ana;
    private Responsavel bruno;
    private Cliente maria;
    private Cliente joao;

    @BeforeEach
    void setUp() {
        tipo = tipoServicoRepository.save(new TipoServico("Serviço Relatório Teste", null));
        ana = responsavelRepository.save(new Responsavel("Ana Relatório Teste", null));
        bruno = responsavelRepository.save(new Responsavel("Bruno Relatório Teste", null));
        maria = new Cliente("Maria Relatório", LocalDate.of(2019, 1, 1));
        maria.setCpf(CPF);
        clienteRepository.save(maria);
        joao = clienteRepository.save(new Cliente("João Relatório", LocalDate.of(2019, 1, 1)));
    }

    @Test
    void relatorioDePagamentosFiltraPorPeriodoEResponsavel() throws Exception {
        pagamento(maria, ana, LocalDate.of(2019, 7, 10), "1234.56", "100.00", null);
        pagamento(joao, bruno, LocalDate.of(2019, 7, 12), "2222.22", null, null);
        pagamento(maria, ana, LocalDate.of(2019, 8, 1), "3333.33", null, null);

        mvc.perform(get("/relatorios/pagamentos").param("mes", "2019-07").param("responsavelId", ana.getId().toString())
                        .with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Julho de 2019"),
                        containsString("Maria Relatório"),
                        containsString("1.234,56"),
                        not(containsString("2.222,22")),
                        not(containsString("3.333,33")))));
    }

    @Test
    void csvDePagamentosUsaOsMesmosFiltrosNaoExpoeCpfEFicaNoHistorico() throws Exception {
        pagamento(maria, ana, LocalDate.of(2019, 7, 10), "1234.56", "100.00", "40.00");
        pagamento(joao, bruno, LocalDate.of(2019, 7, 12), "2222.22", null, null);

        MvcResult resultado = mvc.perform(get("/relatorios/pagamentos.csv").param("mes", "2019-07")
                        .param("responsavelId", ana.getId().toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/csv")))
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment; filename=\"pagamentos-2019-07.csv\"")))
                .andReturn();

        String csv = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFDia de recebimento;Cliente;");
        assertThat(csv).contains("10/07/2019;Maria Relatório;Serviço Relatório Teste;Ana Relatório Teste;")
                .contains(";1234,56;")
                .doesNotContain("João Relatório")
                .doesNotContain(CPF);

        List<Auditoria> registros = auditoriaRepository
                .findByEntidadeOrderByDataHoraDescIdDesc(RelatorioService.ENTIDADE, PageRequest.of(0, 5)).getContent();
        assertThat(registros).isNotEmpty();
        assertThat(registros.getFirst().getAcao()).isEqualTo(AcaoAuditoria.EXPORTACAO);
        assertThat(registros.getFirst().getDescricao()).contains("pagamentos", "Julho de 2019", "1 linha");
    }

    @Test
    void relatorioDeHonorariosFiltraPorSituacao() throws Exception {
        PagamentoAgendado recebido = pagamento(maria, ana, LocalDate.of(2019, 7, 5), "1500.00", "500.00", null);
        recebido.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);
        pagamento(joao, bruno, LocalDate.of(2019, 7, 6), "1500.00", "300.00", null);
        pagamento(maria, ana, LocalDate.of(2019, 7, 7), "1500.00", null, "80.00");
        pagamentoRepository.flush();

        mvc.perform(get("/relatorios/honorarios").param("mes", "2019-07").param("situacao", "RECEBIDO")
                        .with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Maria Relatório"),
                        containsString("500,00"),
                        not(containsString("300,00")),
                        not(containsString("80,00")))));
    }

    @Test
    void relatorioDeRepassesFiltraPorResponsavel() throws Exception {
        pagamento(maria, ana, LocalDate.of(2019, 7, 10), "1500.00", null, "300.00");
        pagamento(joao, bruno, LocalDate.of(2019, 7, 11), "1500.00", null, "444.44");

        MvcResult resultado = mvc.perform(get("/relatorios/repasses.csv").param("mes", "2019-07")
                        .param("responsavelId", ana.getId().toString()).with(operador()))
                .andExpect(status().isOk())
                .andReturn();
        String csv = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).contains("Ana Relatório Teste;300,00").doesNotContain("444,44");
    }

    @Test
    void historicoDoClienteSeparaBeneficioHonorarioERepasse() throws Exception {
        PagamentoAgendado pagamento = pagamento(maria, ana, LocalDate.of(2019, 7, 10), "5000.00", "1500.00", "250.00");
        pagamento.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);
        pagamento.setSituacaoRepasse(SituacaoRepassePagamento.PENDENTE);
        pagamentoRepository.flush();

        mvc.perform(get("/relatorios/historico").param("clienteId", maria.getId().toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Histórico financeiro de"),
                        containsString("Maria Relatório"),
                        containsString("5.000,00"),
                        containsString("1.500,00"),
                        containsString("250,00"),
                        not(containsString("João Relatório")))));

        mvc.perform(get("/relatorios/historico").param("busca", "relatório").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Maria Relatório"), containsString("João Relatório"))));
    }

    @Test
    void exportacaoExigeUsuarioAutenticado() throws Exception {
        mvc.perform(get("/relatorios/pagamentos.csv"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void parametrosInvalidosNaoQuebramORelatorio() throws Exception {
        mvc.perform(get("/relatorios/pagamentos").param("mes", "xyz").param("situacao", "INEXISTENTE")
                        .with(operador()))
                .andExpect(status().isOk());
    }

    @Test
    void inicioDosRelatoriosVaiParaPagamentos() throws Exception {
        mvc.perform(get("/relatorios").with(operador()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/relatorios/pagamentos"));
    }

    private PagamentoAgendado pagamento(Cliente cliente, Responsavel responsavel, LocalDate data, String valor,
                                        String honorario, String repasse) {
        PagamentoAgendado pagamento = new PagamentoAgendado(cliente, responsavel, tipo, data, new BigDecimal(valor),
                "Banco do Brasil");
        if (honorario != null) {
            pagamento.setValorHonorario(new BigDecimal(honorario));
        }
        if (repasse != null) {
            pagamento.setValorRepasse(new BigDecimal(repasse));
        }
        return pagamentoRepository.save(pagamento);
    }

    private static RequestPostProcessor operador() {
        return user("ana").roles("OPERADOR");
    }
}
