package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.AgendaHonorarios;
import br.com.escritorio.financeiro.dto.HistoricoCliente;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.security.UsuariosDeTeste;
import br.com.escritorio.financeiro.service.HonorarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class HonorarioContinuidadeIntegracaoTest extends IntegracaoTest {

    private static final String CPF = "390.533.447-05";

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
    @Autowired
    private HonorarioService honorarioService;

    private LocalDate hoje;
    private Responsavel ana;
    private TipoServico tipo;

    @BeforeEach
    void setUp() {
        hoje = LocalDate.now(relogio);
        ana = responsavelRepository.save(new Responsavel("Ana Continuidade Teste", null));
        tipo = tipoServicoRepository.save(new TipoServico("Salário-maternidade Continuidade", null));
    }

    @Test
    void salarioMaternidadeParcialContinuaNosProximosCreditosAteQuitar() throws Exception {
        LocalDate outubro = hoje.withDayOfMonth(Math.min(2, hoje.lengthOfMonth()));
        mvc.perform(novoPagamento("Maria Continuidade", CPF, outubro, "2000,00")
                        .param("valorHonorario", "1.800,00")
                        .param("situacaoHonorario", "PARCIAL")
                        .param("valorHonorarioPago", "450,00"))
                .andExpect(status().is3xxRedirection());
        PagamentoAgendado origem = pagamentoDoCliente("Maria Continuidade");
        assertThat(origem.getValorHonorario()).isEqualByComparingTo("1800.00");
        assertThat(origem.getValorHonorarioPago()).isEqualByComparingTo("450.00");
        assertThat(origem.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.PARCIAL);

        mvc.perform(get("/agenda-pagamentos/honorario-aberto")
                        .param("clienteId", origem.getCliente().getId().toString())
                        .param("tipoServicoId", tipo.getId().toString())
                        .with(operador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origens[0].id").value(origem.getId().intValue()))
                .andExpect(jsonPath("$.origens[0].saldo").value(1350.00));

        LocalDate novembro = outubro.plusMonths(1);
        mvc.perform(novoPagamento("Maria Continuidade", CPF, novembro, "2000,00"))
                .andExpect(status().is3xxRedirection());
        PagamentoAgendado novembroPagamento = pagamentoNoDia("Maria Continuidade", novembro);
        assertThat(novembroPagamento.getHonorarioOrigem().getId()).isEqualTo(origem.getId());
        assertThat(novembroPagamento.getValorHonorario()).isNull();
        assertThat(novembroPagamento.getValorHonorarioPago()).isNull();
        assertThat(novembroPagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.PARCIAL);

        mvc.perform(post("/honorarios/{id}/pago-continuacao", novembroPagamento.getId())
                        .with(operador()).with(csrf())
                        .param("valorPago", "500,00"))
                .andExpect(status().is3xxRedirection());
        pagamentoRepository.flush();
        assertThat(pagamentoRepository.findById(novembroPagamento.getId()).orElseThrow().getValorHonorarioPago())
                .isEqualByComparingTo("500.00");
        PagamentoAgendado origemAtual = pagamentoRepository.findById(origem.getId()).orElseThrow();
        assertThat(origemAtual.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.PARCIAL);
        assertThat(honorarioService.totalRecebido(origem.getId())).isEqualByComparingTo("950.00");

        mvc.perform(get("/agenda-pagamentos").param("periodo", "DIA").param("data", novembro.toString())
                        .with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Continua")))
                .andExpect(content().string(containsString("Pago neste crédito")));

        LocalDate dezembro = novembro.plusMonths(1);
        mvc.perform(novoPagamento("Maria Continuidade", CPF, dezembro, "2000,00")
                        .param("valorPagoContinuacao", "850,00"))
                .andExpect(status().is3xxRedirection());
        pagamentoRepository.flush();
        assertThat(pagamentoRepository.findById(origem.getId()).orElseThrow().getSituacaoHonorario())
                .isEqualTo(SituacaoHonorarioPagamento.RECEBIDO);
        assertThat(pagamentoRepository.findByHonorarioOrigemIdOrderByDataRecebimentoAscIdAsc(origem.getId()))
                .hasSize(2)
                .allMatch(p -> p.getSituacaoHonorario() == SituacaoHonorarioPagamento.RECEBIDO);
        assertThat(honorarioService.totalRecebido(origem.getId())).isEqualByComparingTo("1800.00");

        LocalDate janeiro = dezembro.plusMonths(1);
        mvc.perform(novoPagamento("Maria Continuidade", CPF, janeiro, "2000,00")
                        .param("valorHonorario", "300,00")
                        .param("situacaoHonorario", "A_RECEBER"))
                .andExpect(status().is3xxRedirection());
        PagamentoAgendado janeiroPagamento = pagamentoNoDia("Maria Continuidade", janeiro);
        assertThat(janeiroPagamento.getHonorarioOrigem()).isNull();
        assertThat(janeiroPagamento.getValorHonorario()).isEqualByComparingTo("300.00");

        AgendaHonorarios doCliente = honorarioService.doCliente(origem.getCliente().getId());
        HistoricoCliente historico = HistoricoCliente.de(origem.getCliente(), doCliente);
        assertThat(historico.totalHonorarios()).isEqualByComparingTo("2100.00");
        assertThat(historico.honorariosRecebidos()).isEqualByComparingTo("1800.00");
    }

    @Test
    void honorarioNovoCriaDividaIndependente() throws Exception {
        LocalDate outubro = hoje.withDayOfMonth(Math.min(2, hoje.lengthOfMonth()));
        mvc.perform(novoPagamento("Joana Continuidade", "123.456.789-09", outubro, "2000,00")
                        .param("valorHonorario", "1.800,00")
                        .param("situacaoHonorario", "PARCIAL")
                        .param("valorHonorarioPago", "450,00"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(novoPagamento("Joana Continuidade", "123.456.789-09", outubro.plusMonths(1), "2000,00")
                        .param("honorarioNovo", "true")
                        .param("valorHonorario", "900,00")
                        .param("situacaoHonorario", "A_RECEBER"))
                .andExpect(status().is3xxRedirection());

        assertThat(pagamentosDoCliente("Joana Continuidade"))
                .allMatch(p -> p.getHonorarioOrigem() == null);
        AgendaHonorarios doCliente = honorarioService.doCliente(
                clienteRepository.findByCpf("12345678909").orElseThrow().getId());
        assertThat(HistoricoCliente.de(doCliente.linhas().getFirst().pagamento().getCliente(), doCliente)
                .totalHonorarios()).isEqualByComparingTo("2700.00");
    }

    @Test
    void valorPagoNaContinuacaoNaoPassaDoSaldo() throws Exception {
        LocalDate outubro = hoje.withDayOfMonth(Math.min(2, hoje.lengthOfMonth()));
        mvc.perform(novoPagamento("Bia Continuidade", "071.168.360-39", outubro, "2000,00")
                        .param("valorHonorario", "1.800,00")
                        .param("situacaoHonorario", "PARCIAL")
                        .param("valorHonorarioPago", "450,00"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(novoPagamento("Bia Continuidade", "071.168.360-39", outubro.plusMonths(1), "2000,00")
                        .param("valorPagoContinuacao", "1.400,00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("passa do saldo em aberto")));
        assertThat(pagamentosDoCliente("Bia Continuidade")).hasSize(1);
    }

    @Test
    void origemComNegociacaoLigaOFilhoSemGravarFatia() throws Exception {
        LocalDate outubro = hoje.withDayOfMonth(Math.min(2, hoje.lengthOfMonth()));
        mvc.perform(novoPagamento("Carla Continuidade", "529.982.247-25", outubro, "2000,00")
                        .param("valorHonorario", "1.800,00")
                        .param("situacaoHonorario", "PARCIAL")
                        .param("valorHonorarioPago", "450,00"))
                .andExpect(status().is3xxRedirection());
        PagamentoAgendado origem = pagamentoDoCliente("Carla Continuidade");
        mvc.perform(post("/honorarios/{id}/negociar", origem.getId()).with(operador()).with(csrf())
                        .param("valorNegociado", "1.800,00")
                        .param("quantidade", "1")
                        .param("primeiroVencimento", outubro.plusMonths(1).toString())
                        .param("parcelas[0].vencimento", outubro.plusMonths(1).toString())
                        .param("parcelas[0].valor", "1.350,00"))
                .andExpect(redirectedUrl("/honorarios/" + origem.getId()));

        mvc.perform(novoPagamento("Carla Continuidade", "529.982.247-25", outubro.plusMonths(1), "2000,00")
                        .param("valorPagoContinuacao", "500,00"))
                .andExpect(status().is3xxRedirection());
        PagamentoAgendado filho = pagamentosDoCliente("Carla Continuidade").stream()
                .filter(PagamentoAgendado::isContinuacao)
                .findFirst().orElseThrow();
        assertThat(filho.getHonorarioOrigem().getId()).isEqualTo(origem.getId());
        assertThat(filho.getValorHonorario()).isNull();
        assertThat(filho.getValorHonorarioPago()).isNull();
        assertThat(filho.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.PARCIAL);
        assertThat(honorarioService.totalRecebido(origem.getId())).isEqualByComparingTo("450.00");
    }

    @Test
    void detalheDoFilhoRedirecionaParaAOrigem() throws Exception {
        LocalDate outubro = hoje.withDayOfMonth(Math.min(2, hoje.lengthOfMonth()));
        mvc.perform(novoPagamento("Rita Continuidade", "111.444.777-35", outubro, "2000,00")
                        .param("valorHonorario", "1.800,00")
                        .param("situacaoHonorario", "PARCIAL")
                        .param("valorHonorarioPago", "450,00"))
                .andExpect(status().is3xxRedirection());
        PagamentoAgendado origem = pagamentoDoCliente("Rita Continuidade");
        mvc.perform(novoPagamento("Rita Continuidade", "111.444.777-35", outubro.plusMonths(1), "2000,00")
                        .param("valorPagoContinuacao", "500,00"))
                .andExpect(status().is3xxRedirection());
        PagamentoAgendado filho = pagamentosDoCliente("Rita Continuidade").stream()
                .filter(PagamentoAgendado::isContinuacao)
                .findFirst().orElseThrow();

        mvc.perform(get("/honorarios/{id}", filho.getId()).with(operador()))
                .andExpect(redirectedUrl("/honorarios/" + origem.getId()));
        mvc.perform(get("/honorarios/{id}", origem.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Créditos deste honorário")));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder novoPagamento(
            String nome, String cpf, LocalDate data, String valor) {
        return post("/agenda-pagamentos").with(operador()).with(csrf())
                .param("nomeCliente", nome)
                .param("cpf", cpf)
                .param("responsavelIds", ana.getId().toString())
                .param("tipoServicoId", tipo.getId().toString())
                .param("dataRecebimento", data.toString())
                .param("valor", valor)
                .param("banco", "Caixa Econômica Federal")
                .param("status", "AGENDADO");
    }

    private PagamentoAgendado pagamentoDoCliente(String nome) {
        return pagamentosDoCliente(nome).getFirst();
    }

    private PagamentoAgendado pagamentoNoDia(String nome, LocalDate data) {
        return pagamentosDoCliente(nome).stream()
                .filter(p -> p.getDataRecebimento().equals(data))
                .findFirst().orElseThrow();
    }

    private java.util.List<PagamentoAgendado> pagamentosDoCliente(String nome) {
        Cliente cliente = clienteRepository.findAll().stream()
                .filter(c -> c.getNome().equals(nome))
                .findFirst().orElseThrow();
        return pagamentoRepository.findByClienteIdOrderByDataRecebimentoDescIdDesc(cliente.getId());
    }

    private static RequestPostProcessor operador() {
        return UsuariosDeTeste.operador();
    }
}
