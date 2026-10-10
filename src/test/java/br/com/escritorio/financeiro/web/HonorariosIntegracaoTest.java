package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.FormaCobranca;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoParcela;
import br.com.escritorio.financeiro.domain.TipoPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.FiltroSaldo;
import br.com.escritorio.financeiro.dto.ResumoHonorarios;
import br.com.escritorio.financeiro.repository.AcordoHonorariosRepository;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.ParcelaHonorarioRepository;
import br.com.escritorio.financeiro.repository.PrevisaoBeneficioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.service.HonorariosService;
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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class HonorariosIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Clock relogio;
    @Autowired
    private HonorariosService honorariosService;
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
    private ParcelaHonorarioRepository parcelaRepository;
    @Autowired
    private RecebimentoHonorarioRepository recebimentoRepository;

    private LocalDate hoje;
    private Atendimento atendimento;
    private int atendimentosCriados;

    @BeforeEach
    void setUp() {
        hoje = LocalDate.now(relogio);
        atendimento = novoAtendimento("Cliente Honorários Teste");
    }

    @Test
    void fluxoDeAcordoParcelasERecebimentoPelaTela() throws Exception {
        mvc.perform(post("/atendimentos/{id}/honorarios", atendimento.getId()).with(operador()).with(csrf())
                        .param("formaCobranca", "VALOR_FIXO")
                        .param("valorContratado", "3.000,00")
                        .param("dataAcordo", hoje.toString()))
                .andExpect(redirectedUrlPattern("/honorarios/*"));
        AcordoHonorarios acordo = acordoRepository.findByAtendimentoId(atendimento.getId()).orElseThrow();

        mvc.perform(post("/honorarios/{id}/parcelas/gerar", acordo.getId()).with(operador()).with(csrf())
                        .param("quantidade", "3")
                        .param("valorTotal", "3.000,00")
                        .param("primeiroVencimento", hoje.plusMonths(1).toString()))
                .andExpect(redirectedUrl("/honorarios/" + acordo.getId() + "#parcelas"));
        ParcelaHonorario primeira = parcelaRepository.findByAcordoIdOrderByNumeroAsc(acordo.getId()).getFirst();

        mvc.perform(post("/honorarios/{id}/recebimentos", acordo.getId()).with(operador()).with(csrf())
                        .param("dataRecebimento", hoje.toString())
                        .param("valor", "1.000,00")
                        .param("formaPagamento", "PIX")
                        .param("parcelaId", primeira.getId().toString()))
                .andExpect(redirectedUrl("/honorarios/" + acordo.getId() + "#recebimentos"));

        assertThat(primeira.getSituacao()).isEqualTo(SituacaoParcela.PAGA);
        ResumoHonorarios resumo = honorariosService.resumo(acordo.getId());
        assertThat(resumo.totalRecebido()).isEqualByComparingTo("1000.00");
        assertThat(resumo.saldo()).isEqualByComparingTo("2000.00");

        mvc.perform(get("/honorarios/{id}", acordo.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("3.000,00"),
                        containsString("2.000,00"),
                        containsString("Paga"),
                        containsString("Recebimento de R$"),
                        not(containsString("recebimentos/" + resumo.recebimentos().getFirst().getId() + "/cancelar")))));

        mvc.perform(get("/atendimentos/{id}", atendimento.getId()).with(operador()))
                .andExpect(content().string(allOf(containsString("Abrir honorários"), containsString("2.000,00"))));
    }

    @Test
    void beneficioDoInssNaoEntraNoSaldo() {
        PrevisaoBeneficio previsao = previsaoRepository.save(new PrevisaoBeneficio(atendimento, hoje,
                new BigDecimal("5000.00"), TipoPagamento.RETROATIVO));
        previsao.confirmarLiberacao(hoje, new BigDecimal("5200.00"));
        AcordoHonorarios acordo = acordoValorFixo(atendimento, "3000.00");
        receber(acordo, "1000.00");

        ResumoHonorarios resumo = honorariosService.resumo(acordo.getId());

        assertThat(resumo.totalRecebido()).isEqualByComparingTo("1000.00");
        assertThat(resumo.saldo()).isEqualByComparingTo("2000.00");
    }

    @Test
    void acordoPercentualFicaComValorADefinirMesmoComBeneficioLiberado() throws Exception {
        PrevisaoBeneficio previsao = previsaoRepository.save(new PrevisaoBeneficio(atendimento, hoje,
                new BigDecimal("10000.00"), TipoPagamento.RETROATIVO));
        previsao.confirmarLiberacao(hoje, new BigDecimal("10000.00"));

        mvc.perform(post("/atendimentos/{id}/honorarios", atendimento.getId()).with(operador()).with(csrf())
                        .param("formaCobranca", "PERCENTUAL")
                        .param("percentual", "30")
                        .param("baseCalculo", "Valores retroativos do benefício")
                        .param("dataAcordo", hoje.toString()))
                .andExpect(redirectedUrlPattern("/honorarios/*"));

        AcordoHonorarios acordo = acordoRepository.findByAtendimentoId(atendimento.getId()).orElseThrow();
        assertThat(acordo.getValorContratado()).isNull();
        mvc.perform(get("/honorarios/{id}", acordo.getId()).with(operador()))
                .andExpect(content().string(allOf(containsString("A definir"), containsString("30,00%"))));
    }

    @Test
    void percentualSemBaseDeCalculoVoltaComErro() throws Exception {
        mvc.perform(post("/atendimentos/{id}/honorarios", atendimento.getId()).with(operador()).with(csrf())
                        .param("formaCobranca", "PERCENTUAL")
                        .param("percentual", "30")
                        .param("dataAcordo", hoje.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "baseCalculo"));
    }

    @Test
    void segundoAcordoNoMesmoAtendimentoLevaAoExistente() throws Exception {
        AcordoHonorarios acordo = acordoValorFixo(atendimento, "1000.00");

        mvc.perform(get("/atendimentos/{id}/honorarios/novo", atendimento.getId()).with(operador()))
                .andExpect(redirectedUrl("/honorarios/" + acordo.getId()));
    }

    @Test
    void somenteAdministradorCancelaRecebimentoEComMotivo() throws Exception {
        AcordoHonorarios acordo = acordoValorFixo(atendimento, "3000.00");
        RecebimentoHonorario recebimento = receber(acordo, "1000.00");

        mvc.perform(post("/recebimentos/{id}/cancelar", recebimento.getId()).with(operador()).with(csrf())
                        .param("motivo", "Lançado em duplicidade"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/recebimentos/{id}/cancelar", recebimento.getId()).with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/honorarios/" + acordo.getId() + "#recebimentos"))
                .andExpect(flash().attributeExists("erro"));
        assertThat(recebimento.isCancelado()).isFalse();

        mvc.perform(post("/recebimentos/{id}/cancelar", recebimento.getId()).with(admin()).with(csrf())
                        .param("motivo", "Lançado em duplicidade"))
                .andExpect(flash().attributeExists("sucesso"));

        assertThat(recebimento.isCancelado()).isTrue();
        assertThat(recebimentoRepository.findById(recebimento.getId())).isPresent();
        assertThat(honorariosService.resumo(acordo.getId()).saldo()).isEqualByComparingTo("3000.00");
    }

    @Test
    void listaFiltraPorSaldo() throws Exception {
        AcordoHonorarios pendente = acordoValorFixo(atendimento, "3000.00");
        receber(pendente, "1000.00");
        AcordoHonorarios quitado = acordoValorFixo(novoAtendimento("Cliente Quitado Honorários"), "500.00");
        receber(quitado, "500.00");

        assertThat(honorariosService.listar("honorários teste", null, FiltroSaldo.PENDENTES)).hasSize(1);

        mvc.perform(get("/honorarios").param("saldo", "QUITADOS").param("busca", "Honorários")
                        .with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Quitado Honorários"),
                        not(containsString("Cliente Honorários Teste")))));
    }

    private Atendimento novoAtendimento(String nomeCliente) {
        Cliente cliente = clienteRepository.save(new Cliente(nomeCliente, hoje));
        int sequencia = ++atendimentosCriados;
        TipoServico tipo = tipoServicoRepository.save(new TipoServico("Serviço Honorários " + sequencia, null));
        Responsavel responsavel = responsavelRepository.save(new Responsavel("Responsável Honorários " + sequencia, null));
        return atendimentoRepository.save(new Atendimento(cliente, tipo, responsavel, hoje));
    }

    private AcordoHonorarios acordoValorFixo(Atendimento atendimento, String valor) {
        AcordoHonorarios acordo = new AcordoHonorarios(atendimento, FormaCobranca.VALOR_FIXO, hoje);
        acordo.setValorContratado(new BigDecimal(valor));
        return acordoRepository.save(acordo);
    }

    private RecebimentoHonorario receber(AcordoHonorarios acordo, String valor) {
        return recebimentoRepository.save(new RecebimentoHonorario(acordo, null, hoje, new BigDecimal(valor),
                FormaPagamento.PIX));
    }

    private static RequestPostProcessor operador() {
        return user("ana").roles("OPERADOR");
    }

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }
}
