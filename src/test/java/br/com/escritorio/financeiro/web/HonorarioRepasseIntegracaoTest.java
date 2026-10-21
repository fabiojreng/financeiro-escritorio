package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.ResponsavelPagamento;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.security.UsuariosDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class HonorarioRepasseIntegracaoTest extends IntegracaoTest {

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

    private LocalDate hoje;
    private Responsavel ana;
    private Responsavel bruno;
    private TipoServico tipo;

    @BeforeEach
    void setUp() {
        hoje = LocalDate.now(relogio);
        ana = responsavelRepository.save(new Responsavel("Ana Honorário Teste", null));
        bruno = responsavelRepository.save(new Responsavel("Bruno Honorário Teste", null));
        tipo = tipoServicoRepository.save(new TipoServico("Benefício Honorário Teste", null));
    }

    @Test
    void todoPagamentoDaAgendaApareceEmHonorariosEFiltraPorResponsavelESituacao() throws Exception {
        PagamentoAgendado daAna = pagamento("Cliente Honorário Ana", "11144477735", hoje, ana, "500.00", null);
        daAna.setSituacaoHonorario(SituacaoHonorarioPagamento.RECEBIDO);
        pagamento("Cliente Honorário Bruno", "39053344705", hoje, bruno, "300.00", null);
        PagamentoAgendado semHonorario = pagamento("Cliente Sem Honorário", "52998224725", hoje, ana, null, null);
        pagamentoRepository.flush();

        mvc.perform(get("/honorarios").param("periodo", "MES").param("data", hoje.toString()).with(usuarioComRepasses()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Honorário Ana"),
                        containsString("Cliente Honorário Bruno"),
                        containsString("Cliente Sem Honorário"),
                        containsString("id=\"valor-honorario-" + semHonorario.getId() + "\""),
                        containsString("value=\"500,00\""),
                        containsString("800,00"),
                        containsString("2 de 3 pagamento(s) com honorário"))));

        mvc.perform(get("/honorarios").param("periodo", "MES").param("data", hoje.toString())
                        .param("responsavelId", ana.getId().toString()).with(usuarioComRepasses()))
                .andExpect(content().string(allOf(
                        containsString("Cliente Honorário Ana"),
                        containsString("Cliente Sem Honorário"),
                        not(containsString("Cliente Honorário Bruno")))));

        mvc.perform(get("/honorarios").param("periodo", "MES").param("data", hoje.toString())
                        .param("situacaoHonorario", "A_RECEBER").with(usuarioComRepasses()))
                .andExpect(content().string(allOf(
                        containsString("Cliente Honorário Bruno"),
                        not(containsString("Cliente Honorário Ana")),
                        not(containsString("Cliente Sem Honorário")))));
    }

    @Test
    void lancaHonorarioNaLinhaEAudita() throws Exception {
        PagamentoAgendado pagamento = pagamento("Cliente Lança Honorário", "11144477735", hoje, ana, null, null);
        pagamentoRepository.flush();
        String voltar = "/honorarios?periodo=MES&data=" + hoje;

        mvc.perform(post("/honorarios/{id}", pagamento.getId()).with(usuarioComRepasses()).with(csrf())
                        .param("valor", "1.234,56")
                        .param("situacao", "RECEBIDO")
                        .param("voltar", voltar))
                .andExpect(redirectedUrl(voltar))
                .andExpect(flash().attribute("sucesso", "Honorário de 'Cliente Lança Honorário' salvo."));
        assertThat(pagamento.getValorHonorario()).isEqualByComparingTo("1234.56");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.RECEBIDO);

        mvc.perform(post("/honorarios/{id}", pagamento.getId()).with(usuarioComRepasses()).with(csrf())
                        .param("valor", "")
                        .param("situacao", "RECEBIDO")
                        .param("voltar", "//exemplo.com"))
                .andExpect(redirectedUrl("/honorarios"));
        assertThat(pagamento.getValorHonorario()).isNull();

        mvc.perform(get("/agenda-pagamentos/{id}/editar", pagamento.getId()).with(usuarioComRepasses()))
                .andExpect(content().string(allOf(
                        containsString("honorário de sem valor para R$ 1.234,56"),
                        containsString("situação do honorário de A receber para Recebido"),
                        containsString("honorário de R$ 1.234,56 para sem valor"))));
    }

    @Test
    void valorDeHonorarioOuRepasseInvalidoVoltaComErroSemAlterar() throws Exception {
        PagamentoAgendado pagamento = pagamento("Cliente Valor Inválido", "11144477735", hoje, ana, "100.00", "50.00");
        pagamentoRepository.flush();

        mvc.perform(post("/honorarios/{id}", pagamento.getId()).with(usuarioComRepasses()).with(csrf())
                        .param("valor", "0")
                        .param("situacao", "RECEBIDO"))
                .andExpect(redirectedUrl("/honorarios"))
                .andExpect(flash().attributeExists("erro"));
        ResponsavelPagamento repasse = repasse(pagamento);
        mvc.perform(post("/repasses/{id}", repasse.getId()).with(usuarioComRepasses()).with(csrf())
                        .param("valor", "abc")
                        .param("situacao", "PAGO"))
                .andExpect(redirectedUrl("/repasses"))
                .andExpect(flash().attribute("erro", "O repasse não é um valor válido. Use, por exemplo, 1.234,56."));

        assertThat(pagamento.getValorHonorario()).isEqualByComparingTo("100.00");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.A_RECEBER);
        assertThat(repasse.getValorRepasse()).isEqualByComparingTo("50.00");
        assertThat(repasse.getSituacaoRepasse()).isEqualTo(SituacaoRepassePagamento.PENDENTE);
    }

    @Test
    void todoPagamentoApareceEmRepassesELancaNaLinha() throws Exception {
        PagamentoAgendado comRepasse = pagamento("Cliente Com Repasse", "11144477735", hoje, ana, null, "150.00");
        PagamentoAgendado semRepasse = pagamento("Cliente Sem Repasse", "39053344705", hoje, ana, "400.00", null);
        pagamentoRepository.flush();
        String voltar = "/repasses?periodo=MES&data=" + hoje;

        ResponsavelPagamento repasseLancado = repasse(comRepasse);
        ResponsavelPagamento repasseVazio = repasse(semRepasse);

        mvc.perform(get("/repasses").param("periodo", "MES").param("data", hoje.toString()).with(usuarioComRepasses()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Com Repasse"),
                        containsString("Cliente Sem Repasse"),
                        containsString("id=\"repasse-" + repasseLancado.getId() + "\""),
                        containsString("id=\"valor-repasse-" + repasseVazio.getId() + "\""),
                        containsString("1 de 2 responsável(is) com repasse"))));

        mvc.perform(post("/repasses/{id}", repasseVazio.getId()).with(usuarioComRepasses()).with(csrf())
                        .param("valor", "90,00")
                        .param("situacao", "PAGO")
                        .param("voltar", voltar))
                .andExpect(redirectedUrl(voltar))
                .andExpect(flash().attribute("sucesso",
                        "Repasse de 'Ana Honorário Teste' no pagamento de 'Cliente Sem Repasse' salvo."));
        assertThat(repasseVazio.getValorRepasse()).isEqualByComparingTo("90.00");
        assertThat(repasseVazio.getSituacaoRepasse()).isEqualTo(SituacaoRepassePagamento.PAGO);

        mvc.perform(get("/repasses").param("periodo", "MES").param("data", hoje.toString())
                        .param("situacaoRepasse", "PENDENTE").with(usuarioComRepasses()))
                .andExpect(content().string(allOf(
                        containsString("Cliente Com Repasse"),
                        not(containsString("Cliente Sem Repasse")))));
    }

    @Test
    void pagamentoCompartilhadoTemUmaLinhaDeRepassePorResponsavel() throws Exception {
        PagamentoAgendado pagamento = pagamento("Cliente Compartilhado Repasse", "11144477735", hoje, ana,
                "600.00", "100.00");
        ResponsavelPagamento doBruno = pagamento.adicionarResponsavel(bruno);
        pagamentoRepository.flush();
        ResponsavelPagamento daAna = repasse(pagamento);

        mvc.perform(get("/repasses").param("periodo", "MES").param("data", hoje.toString()).with(usuarioComRepasses()))
                .andExpect(content().string(allOf(
                        containsString("id=\"repasse-" + daAna.getId() + "\""),
                        containsString("id=\"repasse-" + doBruno.getId() + "\""),
                        containsString("1 de 2 responsável(is) com repasse"))));

        mvc.perform(post("/repasses/{id}", doBruno.getId()).with(usuarioComRepasses()).with(csrf())
                        .param("valor", "70,00")
                        .param("situacao", "PAGO"))
                .andExpect(redirectedUrl("/repasses"));
        assertThat(doBruno.getValorRepasse()).isEqualByComparingTo("70.00");
        assertThat(doBruno.getSituacaoRepasse()).isEqualTo(SituacaoRepassePagamento.PAGO);
        assertThat(daAna.getValorRepasse()).isEqualByComparingTo("100.00");
        assertThat(daAna.getSituacaoRepasse()).isEqualTo(SituacaoRepassePagamento.PENDENTE);
        assertThat(pagamento.getTotalRepasse()).isEqualByComparingTo("170.00");

        mvc.perform(get("/repasses").param("periodo", "MES").param("data", hoje.toString())
                        .param("responsavelId", bruno.getId().toString()).with(usuarioComRepasses()))
                .andExpect(content().string(allOf(
                        containsString("id=\"repasse-" + doBruno.getId() + "\""),
                        not(containsString("id=\"repasse-" + daAna.getId() + "\"")))));

        mvc.perform(get("/agenda-pagamentos/{id}/editar", pagamento.getId()).with(usuarioComRepasses()))
                .andExpect(content().string(containsString(
                        "situação do repasse de &#39;Bruno Honorário Teste&#39; de Pendente para Pago")));
    }

    @Test
    void fichaDoClienteMostraOsPagamentos() throws Exception {
        PagamentoAgendado pagamento = pagamento("Cliente Ficha Pagamento", "11144477735", hoje, ana, "500.00", "80.00");
        pagamentoRepository.flush();

        mvc.perform(get("/clientes/{id}", pagamento.getCliente().getId()).with(usuarioComRepasses()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Pagamentos"),
                        containsString("Benefício Honorário Teste"),
                        containsString("500,00"),
                        containsString("80,00"),
                        not(containsString("Atendimentos")))));
    }

    @Test
    void honorariosERepassesMostramTodoOPeriodo() throws Exception {
        pagamento("Cliente Antigo Todo Período", "11144477735", hoje.minusMonths(4), ana, "500.00", "50.00");
        pagamento("Cliente Futuro Todo Período", "39053344705", hoje.plusMonths(3), ana, "300.00", "30.00");
        pagamentoRepository.flush();

        for (String tela : List.of("/honorarios", "/repasses")) {
            mvc.perform(get(tela).param("periodo", "TODOS").with(usuarioComRepasses()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(allOf(
                            containsString("Todas as datas"),
                            containsString("Todo o período"),
                            containsString("Cliente Antigo Todo Período"),
                            containsString("Cliente Futuro Todo Período"),
                            containsString("data-sugerir-cliente"),
                            not(containsString("Mês anterior")))));
        }
    }

    private PagamentoAgendado pagamento(String nome, String cpf, LocalDate data, Responsavel responsavel,
                                        String honorario, String repasse) {
        Cliente cliente = new Cliente(nome, hoje);
        cliente.setCpf(cpf);
        clienteRepository.save(cliente);
        PagamentoAgendado pagamento = new PagamentoAgendado(cliente, List.of(responsavel), tipo, data,
                new BigDecimal("1500.00"), "Banco do Brasil");
        if (honorario != null) {
            pagamento.setValorHonorario(new BigDecimal(honorario));
        }
        if (repasse != null) {
            repasse(pagamento).setValorRepasse(new BigDecimal(repasse));
        }
        return pagamentoRepository.save(pagamento);
    }

    private static ResponsavelPagamento repasse(PagamentoAgendado pagamento) {
        return pagamento.getResponsaveis().get(0);
    }

    private static RequestPostProcessor usuarioComRepasses() {
        return UsuariosDeTeste.operadorCom(Permissao.REPASSES_VISUALIZAR, Permissao.REPASSES_EDITAR);
    }
}
