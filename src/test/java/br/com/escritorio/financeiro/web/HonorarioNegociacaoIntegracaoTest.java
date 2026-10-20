package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoParcela;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ParcelaHonorarioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class HonorarioNegociacaoIntegracaoTest extends IntegracaoTest {

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
    private ParcelaHonorarioRepository parcelaRepository;
    @Autowired
    private RecebimentoHonorarioRepository recebimentoRepository;

    private LocalDate hoje;
    private Responsavel ana;
    private TipoServico tipo;

    @BeforeEach
    void setUp() {
        hoje = LocalDate.now(relogio);
        ana = responsavelRepository.save(new Responsavel("Ana Negociação Teste", null));
        tipo = tipoServicoRepository.save(new TipoServico("Benefício Negociação Teste", null));
    }

    @Test
    void negociarParcelarReceberParcialEEstornar() throws Exception {
        PagamentoAgendado pagamento = pagamento("Cliente Negociação", "11144477735", "900.00");
        pagamentoRepository.flush();

        mvc.perform(post("/honorarios/{id}/negociar", pagamento.getId()).with(operador()).with(csrf())
                        .param("valorNegociado", "600,00")
                        .param("motivo", "Desconto combinado com o cliente")
                        .param("quantidade", "3")
                        .param("primeiroVencimento", hoje.toString())
                        .param("parcelas[0].vencimento", hoje.toString())
                        .param("parcelas[0].valor", "200,00")
                        .param("parcelas[1].vencimento", hoje.plusMonths(1).toString())
                        .param("parcelas[1].valor", "200,00")
                        .param("parcelas[2].vencimento", hoje.plusMonths(2).toString())
                        .param("parcelas[2].valor", "200,00"))
                .andExpect(redirectedUrl("/honorarios/" + pagamento.getId()))
                .andExpect(flash().attribute("sucesso", "Negociação de honorário registrada."));

        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.NEGOCIADO);
        List<ParcelaHonorario> parcelas = parcelaRepository.findAll().stream()
                .filter(p -> p.getNegociacao().getPagamento().getId().equals(pagamento.getId()))
                .toList();
        assertThat(parcelas).hasSize(3);

        ParcelaHonorario primeira = parcelas.stream().filter(p -> p.getNumero() == 1).findFirst().orElseThrow();
        mvc.perform(post("/honorarios/{id}/parcelas/{parcelaId}/receber", pagamento.getId(), primeira.getId())
                        .with(operador()).with(csrf())
                        .param("dataRecebimento", hoje.toString())
                        .param("valor", "80,00")
                        .param("formaPagamento", "PIX"))
                .andExpect(redirectedUrl("/honorarios/" + pagamento.getId()));
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.PARCIAL);
        assertThat(primeira.getSituacao()).isEqualTo(SituacaoParcela.ABERTA);

        var recebimento = recebimentoRepository.findByParcelaIdOrderByDataRecebimentoDescIdDesc(primeira.getId()).getFirst();
        mvc.perform(post("/honorarios/{id}/recebimentos/{recebimentoId}/estornar", pagamento.getId(), recebimento.getId())
                        .with(operador()).with(csrf())
                        .param("motivo", "Lançado em duplicidade"))
                .andExpect(redirectedUrl("/honorarios/" + pagamento.getId()));
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.NEGOCIADO);

        mvc.perform(get("/honorarios/{id}", pagamento.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Negociação"),
                        containsString("600,00"),
                        containsString("Desconto combinado com o cliente"),
                        containsString("Estornado"))));

        mvc.perform(get("/agenda-pagamentos/{id}/editar", pagamento.getId()).with(operador()))
                .andExpect(content().string(containsString("Negociação de honorário")));
    }

    @Test
    void parcelaVencidaApareceComoEmAtrasoNaListaENaAgenda() throws Exception {
        PagamentoAgendado pagamento = pagamento("Cliente Em Atraso", "39053344705", "300.00");
        pagamentoRepository.flush();

        mvc.perform(post("/honorarios/{id}/negociar", pagamento.getId()).with(operador()).with(csrf())
                        .param("valorNegociado", "180,00")
                        .param("motivo", "Parcelamento combinado após atraso")
                        .param("quantidade", "1")
                        .param("primeiroVencimento", hoje.minusDays(5).toString())
                        .param("parcelas[0].vencimento", hoje.minusDays(5).toString())
                        .param("parcelas[0].valor", "180,00"))
                .andExpect(redirectedUrl("/honorarios/" + pagamento.getId()));
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.EM_ATRASO);

        mvc.perform(get("/honorarios").param("periodo", "MES").param("data", hoje.toString())
                        .param("situacaoHonorario", "EM_ATRASO").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Em Atraso"),
                        containsString("Em atraso"))));

        mvc.perform(get("/agenda-pagamentos").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cliente Em Atraso")));

        mvc.perform(get("/relatorios/honorarios").param("mes", hoje.toString().substring(0, 7))
                        .param("situacao", "EM_ATRASO").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Em Atraso"),
                        containsString("180,00"),
                        not(containsString("Atendimentos")))));
    }

    @Test
    void negociaSemMotivoComParcelasDeValoresDiferentes() throws Exception {
        PagamentoAgendado pagamento = pagamento("Cliente Sem Motivo", "12345678909", "5000.00");
        pagamentoRepository.flush();

        mvc.perform(get("/honorarios/{id}/negociar", pagamento.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("(opcional)"),
                        containsString("data-calcular-parcelas"),
                        containsString("data-ja-recebido=\"0"))));

        mvc.perform(post("/honorarios/{id}/negociar", pagamento.getId()).with(operador()).with(csrf())
                        .param("valorNegociado", "4800,00")
                        .param("motivo", "   ")
                        .param("quantidade", "4")
                        .param("primeiroVencimento", hoje.toString())
                        .param("parcelas[0].vencimento", hoje.toString())
                        .param("parcelas[0].valor", "1.000,00")
                        .param("parcelas[1].vencimento", hoje.plusMonths(1).toString())
                        .param("parcelas[1].valor", "1.800,00")
                        .param("parcelas[2].vencimento", hoje.plusMonths(2).toString())
                        .param("parcelas[2].valor", "1.000,00")
                        .param("parcelas[3].vencimento", hoje.plusMonths(3).toString())
                        .param("parcelas[3].valor", "1.000,00"))
                .andExpect(redirectedUrl("/honorarios/" + pagamento.getId()));

        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.NEGOCIADO);
        mvc.perform(get("/honorarios/{id}", pagamento.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Não informado"),
                        containsString("1.800,00"))));
    }

    @Test
    void lancamentoSimplesContinuaNaLinha() throws Exception {
        PagamentoAgendado pagamento = pagamento("Cliente Simples", "52998224725", null);
        pagamentoRepository.flush();

        mvc.perform(post("/honorarios/{id}", pagamento.getId()).with(operador()).with(csrf())
                        .param("valor", "250,00")
                        .param("situacao", "RECEBIDO"))
                .andExpect(redirectedUrl("/honorarios"));
        assertThat(pagamento.getValorHonorario()).isEqualByComparingTo("250.00");
        assertThat(pagamento.getSituacaoHonorario()).isEqualTo(SituacaoHonorarioPagamento.RECEBIDO);
    }

    private PagamentoAgendado pagamento(String nome, String cpf, String honorario) {
        Cliente cliente = new Cliente(nome, hoje);
        cliente.setCpf(cpf);
        clienteRepository.save(cliente);
        PagamentoAgendado pagamento = new PagamentoAgendado(cliente, List.of(ana), tipo, hoje, new BigDecimal("1500.00"),
                "Banco do Brasil");
        if (honorario != null) {
            pagamento.setValorHonorario(new BigDecimal(honorario));
        }
        return pagamentoRepository.save(pagamento);
    }

    private static RequestPostProcessor operador() {
        return user("ana").roles("OPERADOR");
    }
}
