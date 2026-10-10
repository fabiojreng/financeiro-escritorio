package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.FormaCobranca;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoRepasse;
import br.com.escritorio.financeiro.domain.TipoPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.ResumoHonorarios;
import br.com.escritorio.financeiro.repository.AcordoHonorariosRepository;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PrevisaoBeneficioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import br.com.escritorio.financeiro.repository.RepasseRepository;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
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
class RepasseIntegracaoTest extends IntegracaoTest {

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
    private RecebimentoHonorarioRepository recebimentoRepository;
    @Autowired
    private RepasseRepository repasseRepository;

    private LocalDate hoje;
    private Atendimento atendimento;
    private Responsavel destinatario;

    @BeforeEach
    void setUp() {
        hoje = LocalDate.now(relogio);
        Cliente cliente = clienteRepository.save(new Cliente("Cliente Repasse Teste", hoje));
        TipoServico tipo = tipoServicoRepository.save(new TipoServico("Serviço Repasse Teste", null));
        destinatario = responsavelRepository.save(new Responsavel("Destinatário Repasse Teste", null));
        atendimento = atendimentoRepository.save(new Atendimento(cliente, tipo, destinatario, hoje));
    }

    @Test
    void fluxoDeLancamentoEPagamentoPelaTela() throws Exception {
        mvc.perform(get("/atendimentos/{id}/repasses/novo", atendimento.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("form", hasProperty("destinatarioId", equalTo(destinatario.getId()))));

        mvc.perform(post("/atendimentos/{id}/repasses", atendimento.getId()).with(operador()).with(csrf())
                        .param("destinatarioId", destinatario.getId().toString())
                        .param("descricao", "Comissão pela indicação")
                        .param("valorPrevisto", "1.250,50")
                        .param("dataPrevista", hoje.plusDays(5).toString()))
                .andExpect(redirectedUrlPattern("/repasses/*"));
        Repasse repasse = repasseRepository.findByAtendimentoIdOrderByDataPrevistaAscIdAsc(atendimento.getId()).getFirst();
        assertThat(repasse.getValorPrevisto()).isEqualByComparingTo("1250.50");

        mvc.perform(post("/repasses/{id}/pagamento", repasse.getId()).with(operador()).with(csrf())
                        .param("dataPagamento", hoje.toString())
                        .param("valorPago", "1.200,00"))
                .andExpect(redirectedUrl("/repasses/" + repasse.getId()));

        assertThat(repasse.getSituacao()).isEqualTo(SituacaoRepasse.PAGO);
        assertThat(repasse.getValorPago()).isEqualByComparingTo("1200.00");

        mvc.perform(get("/repasses/{id}", repasse.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("1.250,50"),
                        containsString("1.200,00"),
                        containsString("Pagamento de R$"),
                        not(containsString("/repasses/" + repasse.getId() + "/cancelar")))));

        mvc.perform(get("/atendimentos/{id}", atendimento.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Repasses internos"),
                        containsString("Comissão pela indicação"))));

        mvc.perform(get("/repasses").param("situacao", "PAGO").param("busca", "repasse teste").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Cliente Repasse Teste"), containsString("1.200,00"))));
    }

    @Test
    void repasseNaoEntraNoSaldoDeHonorariosNemMexeNoBeneficio() {
        PrevisaoBeneficio previsao = previsaoRepository.save(new PrevisaoBeneficio(atendimento, hoje,
                new BigDecimal("5000.00"), TipoPagamento.RETROATIVO));
        previsao.confirmarLiberacao(hoje, new BigDecimal("5000.00"));
        AcordoHonorarios acordo = new AcordoHonorarios(atendimento, FormaCobranca.VALOR_FIXO, hoje);
        acordo.setValorContratado(new BigDecimal("3000.00"));
        acordoRepository.save(acordo);
        recebimentoRepository.save(new RecebimentoHonorario(acordo, null, hoje, new BigDecimal("1000.00"),
                FormaPagamento.PIX));
        Repasse repasse = repasseRepository.save(new Repasse(atendimento, destinatario, "Comissão",
                new BigDecimal("400.00"), hoje));
        repasse.registrarPagamento(hoje, new BigDecimal("400.00"));
        repasseRepository.flush();

        ResumoHonorarios resumo = honorariosService.resumo(acordo.getId());

        assertThat(resumo.totalRecebido()).isEqualByComparingTo("1000.00");
        assertThat(resumo.saldo()).isEqualByComparingTo("2000.00");
        assertThat(previsao.getValorConfirmado()).isEqualByComparingTo("5000.00");
    }

    @Test
    void destinatarioEValorSaoObrigatorios() throws Exception {
        mvc.perform(post("/atendimentos/{id}/repasses", atendimento.getId()).with(operador()).with(csrf())
                        .param("descricao", "Comissão")
                        .param("dataPrevista", hoje.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "destinatarioId", "valorPrevisto"));
        assertThat(repasseRepository.findByAtendimentoIdOrderByDataPrevistaAscIdAsc(atendimento.getId())).isEmpty();
    }

    @Test
    void operadorCancelaPendenteMasSoAdministradorCancelaPago() throws Exception {
        Repasse pendente = repasseRepository.save(new Repasse(atendimento, destinatario, "Comissão",
                new BigDecimal("300.00"), hoje));
        Repasse pago = repasseRepository.save(new Repasse(atendimento, destinatario, "Bônus",
                new BigDecimal("200.00"), hoje));
        pago.registrarPagamento(hoje, new BigDecimal("200.00"));

        mvc.perform(post("/repasses/{id}/cancelar", pendente.getId()).with(operador()).with(csrf())
                        .param("motivo", "Comissão não foi combinada"))
                .andExpect(redirectedUrl("/repasses/" + pendente.getId()))
                .andExpect(flash().attributeExists("sucesso"));
        assertThat(pendente.isCancelado()).isTrue();

        mvc.perform(post("/repasses/{id}/cancelar", pago.getId()).with(operador()).with(csrf())
                        .param("motivo", "Pago em duplicidade"))
                .andExpect(flash().attributeExists("erro"));
        assertThat(pago.isPago()).isTrue();

        mvc.perform(get("/repasses/{id}", pago.getId()).with(admin()))
                .andExpect(content().string(containsString("/repasses/" + pago.getId() + "/cancelar")));

        mvc.perform(post("/repasses/{id}/cancelar", pago.getId()).with(admin()).with(csrf())
                        .param("motivo", "Pago em duplicidade"))
                .andExpect(flash().attributeExists("sucesso"));
        assertThat(pago.isCancelado()).isTrue();
        assertThat(repasseRepository.findById(pago.getId())).isPresent();

        mvc.perform(get("/repasses/{id}", pago.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Pago em duplicidade"), containsString("Cancelado"))));
    }

    private static RequestPostProcessor operador() {
        return user("ana").roles("OPERADOR");
    }

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }
}
