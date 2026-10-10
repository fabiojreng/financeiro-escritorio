package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.FormaCobranca;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.TipoPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.AcordoHonorariosRepository;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.AuditoriaRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PrevisaoBeneficioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import br.com.escritorio.financeiro.repository.RepasseRepository;
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
import java.time.LocalDateTime;
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
    private AtendimentoRepository atendimentoRepository;
    @Autowired
    private PrevisaoBeneficioRepository previsaoRepository;
    @Autowired
    private AcordoHonorariosRepository acordoRepository;
    @Autowired
    private RecebimentoHonorarioRepository recebimentoRepository;
    @Autowired
    private RepasseRepository repasseRepository;
    @Autowired
    private AuditoriaRepository auditoriaRepository;

    private TipoServico tipo;
    private Responsavel ana;
    private Responsavel bruno;
    private Atendimento atendimentoAna;
    private Atendimento atendimentoBruno;

    @BeforeEach
    void setUp() {
        tipo = tipoServicoRepository.save(new TipoServico("Serviço Relatório Teste", null));
        ana = responsavelRepository.save(new Responsavel("Ana Relatório Teste", null));
        bruno = responsavelRepository.save(new Responsavel("Bruno Relatório Teste", null));
        Cliente maria = new Cliente("Maria Relatório", LocalDate.of(2019, 1, 1));
        maria.setCpf(CPF);
        clienteRepository.save(maria);
        Cliente joao = clienteRepository.save(new Cliente("João Relatório", LocalDate.of(2019, 1, 1)));
        atendimentoAna = atendimentoRepository.save(new Atendimento(maria, tipo, ana, LocalDate.of(2019, 1, 1)));
        atendimentoBruno = atendimentoRepository.save(new Atendimento(joao, tipo, bruno, LocalDate.of(2019, 1, 1)));
    }

    @Test
    void relatorioDePrevisoesFiltraPorPeriodoEResponsavel() throws Exception {
        previsao(atendimentoAna, LocalDate.of(2019, 7, 10), "1234.56");
        previsao(atendimentoBruno, LocalDate.of(2019, 7, 12), "2222.22");
        previsao(atendimentoAna, LocalDate.of(2019, 8, 1), "3333.33");

        mvc.perform(get("/relatorios/previsoes").param("mes", "2019-07").param("responsavelId", ana.getId().toString())
                        .with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Julho de 2019"),
                        containsString("Valores do cliente"),
                        containsString("Maria Relatório"),
                        containsString("1.234,56"),
                        not(containsString("2.222,22")),
                        not(containsString("3.333,33")))));
    }

    @Test
    void csvDePrevisoesUsaOsMesmosFiltrosNaoExpoeCpfEFicaNoHistorico() throws Exception {
        previsao(atendimentoAna, LocalDate.of(2019, 7, 10), "1234.56");
        previsao(atendimentoBruno, LocalDate.of(2019, 7, 12), "2222.22");

        MvcResult resultado = mvc.perform(get("/relatorios/previsoes.csv").param("mes", "2019-07")
                        .param("responsavelId", ana.getId().toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/csv")))
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment; filename=\"previsoes-beneficio-2019-07.csv\"")))
                .andReturn();

        String csv = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFData prevista;Cliente;");
        assertThat(csv).contains("10/07/2019;Maria Relatório;Serviço Relatório Teste;Ana Relatório Teste;")
                .contains(";1234,56;")
                .doesNotContain("João Relatório")
                .doesNotContain(CPF);

        List<Auditoria> registros = auditoriaRepository
                .findByEntidadeOrderByDataHoraDescIdDesc(RelatorioService.ENTIDADE, PageRequest.of(0, 5)).getContent();
        assertThat(registros).isNotEmpty();
        assertThat(registros.getFirst().getAcao()).isEqualTo(AcaoAuditoria.EXPORTACAO);
        assertThat(registros.getFirst().getDescricao()).contains("previsões", "Julho de 2019", "1 linha");
    }

    @Test
    void relatorioDeRecebimentosSomaSoOsConfirmadosDoPeriodo() throws Exception {
        AcordoHonorarios acordo = acordo(atendimentoAna, LocalDate.of(2019, 6, 1), "5000.00");
        receber(acordo, LocalDate.of(2019, 7, 5), "1000.00");
        receber(acordo, LocalDate.of(2019, 7, 6), "777.77")
                .cancelar("Lançado em duplicidade", "admin", LocalDateTime.of(2019, 7, 7, 9, 0));
        receber(acordo, LocalDate.of(2019, 8, 5), "888.88");
        previsao(atendimentoAna, LocalDate.of(2019, 7, 10), "9999.99");

        mvc.perform(get("/relatorios/recebimentos").param("mes", "2019-07").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Valores do escritório"),
                        containsString("1.000,00"),
                        containsString("777,77"),
                        containsString("Lançado em duplicidade"),
                        not(containsString("888,88")),
                        not(containsString("9.999,99")))));

        MvcResult resultado = mvc.perform(get("/relatorios/recebimentos.csv").param("mes", "2019-07")
                        .param("situacao", "CONFIRMADOS").with(operador()))
                .andExpect(status().isOk())
                .andReturn();
        String csv = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).contains(";1000,00;Confirmado;").doesNotContain("777,77");
    }

    @Test
    void relatorioDeAcordosFiltraPorSaldo() throws Exception {
        AcordoHonorarios pendente = acordo(atendimentoAna, LocalDate.of(2019, 7, 1), "3000.00");
        receber(pendente, LocalDate.of(2019, 7, 2), "1000.00");
        AcordoHonorarios quitado = acordo(atendimentoBruno, LocalDate.of(2019, 7, 3), "500.00");
        receber(quitado, LocalDate.of(2019, 7, 4), "500.00");

        mvc.perform(get("/relatorios/acordos").param("mes", "2019-07").param("situacao", "PENDENTES")
                        .with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Maria Relatório"),
                        containsString("2.000,00"),
                        not(containsString("João Relatório")))));

        MvcResult resultado = mvc.perform(get("/relatorios/acordos.csv").param("mes", "2019-07").with(operador()))
                .andReturn();
        String csv = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).contains(";3000,00;1000,00;2000,00").contains(";500,00;500,00;0,00");
    }

    @Test
    void relatorioDeRepassesFiltraPorDestinatario() throws Exception {
        repasse(atendimentoAna, ana, LocalDate.of(2019, 7, 10), "300.00");
        repasse(atendimentoBruno, bruno, LocalDate.of(2019, 7, 11), "444.44");

        MvcResult resultado = mvc.perform(get("/relatorios/repasses.csv").param("mes", "2019-07")
                        .param("responsavelId", ana.getId().toString()).with(operador()))
                .andExpect(status().isOk())
                .andReturn();
        String csv = new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(csv).contains("Ana Relatório Teste;Comissão;300,00").doesNotContain("444,44");

        mvc.perform(get("/relatorios/repasses").param("mes", "2019-07").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Destinatário"), containsString("744,44"))));
    }

    @Test
    void historicoDoClienteSeparaValoresDoClienteEDoEscritorio() throws Exception {
        PrevisaoBeneficio previsao = previsao(atendimentoAna, LocalDate.of(2019, 7, 10), "5000.00");
        previsao.confirmarLiberacao(LocalDate.of(2019, 7, 11), new BigDecimal("5100.00"));
        AcordoHonorarios acordo = acordo(atendimentoAna, LocalDate.of(2019, 7, 1), "3000.00");
        receber(acordo, LocalDate.of(2019, 7, 20), "1000.00");
        repasse(atendimentoAna, ana, LocalDate.of(2019, 7, 25), "250.00");

        mvc.perform(get("/relatorios/historico").param("clienteId", atendimentoAna.getCliente().getId().toString())
                        .with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Histórico financeiro de"),
                        containsString("Maria Relatório"),
                        containsString("Valores do cliente"),
                        containsString("5.100,00"),
                        containsString("Valores do escritório"),
                        containsString("2.000,00"),
                        containsString("250,00"),
                        not(containsString("João Relatório")))));

        mvc.perform(get("/relatorios/historico").param("busca", "relatório").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Maria Relatório"), containsString("João Relatório"))));
    }

    @Test
    void exportacaoExigeUsuarioAutenticado() throws Exception {
        mvc.perform(get("/relatorios/previsoes.csv"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void parametrosInvalidosNaoQuebramORelatorio() throws Exception {
        mvc.perform(get("/relatorios/previsoes").param("mes", "xyz").param("situacao", "INEXISTENTE")
                        .with(operador()))
                .andExpect(status().isOk());
    }

    private PrevisaoBeneficio previsao(Atendimento atendimento, LocalDate data, String valor) {
        return previsaoRepository.save(new PrevisaoBeneficio(atendimento, data, new BigDecimal(valor),
                TipoPagamento.PRIMEIRO_PAGAMENTO));
    }

    private AcordoHonorarios acordo(Atendimento atendimento, LocalDate data, String valor) {
        AcordoHonorarios acordo = new AcordoHonorarios(atendimento, FormaCobranca.VALOR_FIXO, data);
        acordo.setValorContratado(new BigDecimal(valor));
        return acordoRepository.save(acordo);
    }

    private RecebimentoHonorario receber(AcordoHonorarios acordo, LocalDate data, String valor) {
        return recebimentoRepository.save(new RecebimentoHonorario(acordo, null, data, new BigDecimal(valor),
                FormaPagamento.PIX));
    }

    private Repasse repasse(Atendimento atendimento, Responsavel destinatario, LocalDate data, String valor) {
        return repasseRepository.save(new Repasse(atendimento, destinatario, "Comissão", new BigDecimal(valor), data));
    }

    private static RequestPostProcessor operador() {
        return user("ana").roles("OPERADOR");
    }
}
