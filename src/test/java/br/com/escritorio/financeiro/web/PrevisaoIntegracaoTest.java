package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.domain.TipoPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PrevisaoBeneficioRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

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
class PrevisaoIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Clock relogio;
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

    private LocalDate hoje;
    private Atendimento atendimento;

    @BeforeEach
    void setUp() {
        hoje = LocalDate.now(relogio);
        Cliente cliente = clienteRepository.save(new Cliente("Cliente Previsão Teste", hoje));
        TipoServico tipo = tipoServicoRepository.save(new TipoServico("Serviço Previsão Teste", null));
        Responsavel responsavel = responsavelRepository.save(new Responsavel("Responsável Previsão Teste", null));
        atendimento = atendimentoRepository.save(new Atendimento(cliente, tipo, responsavel, hoje));
    }

    @Test
    void cadastraVariasPrevisoesNoMesmoAtendimentoComValorNoPadraoBrasileiro() throws Exception {
        mvc.perform(novaPrevisao("1.234,56", "PRIMEIRO_PAGAMENTO"))
                .andExpect(redirectedUrl("/atendimentos/" + atendimento.getId() + "#previsoes"));
        mvc.perform(novaPrevisao("800,00", "COMPETENCIA_MENSAL"))
                .andExpect(redirectedUrl("/atendimentos/" + atendimento.getId() + "#previsoes"));

        assertThat(previsaoRepository.findByAtendimentoIdOrderByDataPrevistaAscIdAsc(atendimento.getId()))
                .hasSize(2)
                .allSatisfy(p -> {
                    assertThat(p.getSituacao()).isEqualTo(SituacaoPrevisao.PREVISTO);
                    assertThat(p.getValorConfirmado()).isNull();
                })
                .extracting(PrevisaoBeneficio::getValorEstimado)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("1234.56"), new BigDecimal("800.00"));

        mvc.perform(get("/atendimentos/{id}", atendimento.getId()).with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("1.234,56"),
                        containsString("Competência mensal"),
                        containsString("Confirmar liberação"))));
    }

    @Test
    void valorNegativoOuInvalidoVoltaAoFormularioComMensagem() throws Exception {
        mvc.perform(novaPrevisao("-10,00", "OUTRO"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "valorEstimado"))
                .andExpect(content().string(containsString("O valor estimado não pode ser negativo.")));

        mvc.perform(novaPrevisao("abc", "OUTRO"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "valorEstimado"))
                .andExpect(content().string(containsString("Informe um valor válido")));

        assertThat(previsaoRepository.findByAtendimentoIdOrderByDataPrevistaAscIdAsc(atendimento.getId())).isEmpty();
    }

    @Test
    void confirmarLiberacaoGuardaValorConfirmadoSeparadoDoEstimado() throws Exception {
        PrevisaoBeneficio previsao = salvarPrevisao(hoje.minusDays(1));

        mvc.perform(post("/previsoes/{id}/confirmar", previsao.getId()).with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("dataEfetiva", hoje.toString())
                        .param("valorConfirmado", "1.480,00"))
                .andExpect(redirectedUrl("/atendimentos/" + atendimento.getId()));

        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.LIBERADO);
        assertThat(previsao.getDataEfetiva()).isEqualTo(hoje);
        assertThat(previsao.getValorConfirmado()).isEqualByComparingTo("1480.00");
        assertThat(previsao.getValorEstimado()).isEqualByComparingTo("1500.00");
    }

    @Test
    void confirmacaoComDataFuturaERejeitada() throws Exception {
        PrevisaoBeneficio previsao = salvarPrevisao(hoje);

        mvc.perform(post("/previsoes/{id}/confirmar", previsao.getId()).with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("dataEfetiva", hoje.plusDays(3).toString())
                        .param("valorConfirmado", "1.480,00"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "dataEfetiva"));

        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.PREVISTO);
    }

    @Test
    void somenteAdministradorDesfazLiberacao() throws Exception {
        PrevisaoBeneficio previsao = salvarPrevisao(hoje);
        previsao.confirmarLiberacao(hoje, new BigDecimal("1500.00"));

        mvc.perform(post("/previsoes/{id}/desfazer-liberacao", previsao.getId())
                        .with(user("ana").roles("OPERADOR")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.LIBERADO);

        mvc.perform(post("/previsoes/{id}/desfazer-liberacao", previsao.getId())
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/atendimentos/" + atendimento.getId()));
        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.AGUARDANDO_CONFIRMACAO);
        assertThat(previsao.getValorConfirmado()).isNull();
    }

    @Test
    void telasMostramPrevisaoLiberadaComAcoesDeCorrecao() throws Exception {
        PrevisaoBeneficio previsao = salvarPrevisao(hoje);
        previsao.confirmarLiberacao(hoje, new BigDecimal("1475.30"));

        mvc.perform(get("/atendimentos/{id}", atendimento.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("1.475,30"),
                        containsString("Corrigir liberação"),
                        containsString("Desfazer"))));
        mvc.perform(get("/atendimentos/{id}", atendimento.getId()).with(user("ana").roles("OPERADOR")))
                .andExpect(content().string(not(containsString("desfazer-liberacao"))));
        mvc.perform(get("/previsoes/{id}/editar", previsao.getId()).with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editar o valor estimado")));
        mvc.perform(get("/previsoes/{id}/confirmar", previsao.getId()).with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Salvar correção"), containsString("1.475,30"))));
        mvc.perform(get("/agenda").param("mes", YearMonth.from(hoje).toString()).with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cliente Previsão Teste")));
    }

    @Test
    void retornoAposAlterarSituacaoSoAceitaEnderecoInterno() throws Exception {
        PrevisaoBeneficio previsao = salvarPrevisao(hoje);

        mvc.perform(post("/previsoes/{id}/situacao", previsao.getId()).with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("situacao", "ADIADO")
                        .param("voltar", "/agenda?mes=2026-10"))
                .andExpect(redirectedUrl("/agenda?mes=2026-10"));
        assertThat(previsao.getSituacao()).isEqualTo(SituacaoPrevisao.ADIADO);

        mvc.perform(post("/previsoes/{id}/situacao", previsao.getId()).with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("situacao", "PREVISTO")
                        .param("voltar", "//exemplo.com/agenda"))
                .andExpect(redirectedUrl("/atendimentos/" + atendimento.getId()));
    }

    @Test
    void edicaoMostraHistoricoDaPrevisao() throws Exception {
        mvc.perform(novaPrevisao("1.000,00", "RETROATIVO"));
        PrevisaoBeneficio previsao = previsaoRepository
                .findByAtendimentoIdOrderByDataPrevistaAscIdAsc(atendimento.getId()).getFirst();

        mvc.perform(get("/previsoes/{id}/editar", previsao.getId()).with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Histórico desta previsão"),
                        containsString("valor estimado R$"))));
    }

    private MockHttpServletRequestBuilder novaPrevisao(String valor, String tipoPagamento) {
        return post("/atendimentos/{id}/previsoes", atendimento.getId())
                .with(user("ana").roles("OPERADOR")).with(csrf())
                .param("dataPrevista", hoje.plusDays(20).toString())
                .param("valorEstimado", valor)
                .param("tipoPagamento", tipoPagamento)
                .param("situacao", "PREVISTO");
    }

    private PrevisaoBeneficio salvarPrevisao(LocalDate data) {
        return previsaoRepository.save(new PrevisaoBeneficio(atendimento, data, new BigDecimal("1500.00"),
                TipoPagamento.PRIMEIRO_PAGAMENTO));
    }
}
