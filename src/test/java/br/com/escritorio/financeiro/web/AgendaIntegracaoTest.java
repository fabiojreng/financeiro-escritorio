package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.Responsavel;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AgendaIntegracaoTest extends IntegracaoTest {

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
    private TipoServico tipo;
    private Responsavel ana;
    private Responsavel bruno;

    @BeforeEach
    void setUp() {
        hoje = LocalDate.now(relogio);
        tipo = tipoServicoRepository.save(new TipoServico("Serviço Agenda Teste", null));
        ana = responsavelRepository.save(new Responsavel("Ana Agenda Teste", null));
        bruno = responsavelRepository.save(new Responsavel("Bruno Agenda Teste", null));
    }

    @Test
    void mostraSomenteAsPrevisoesDoMesEscolhido() throws Exception {
        Atendimento atendimento = atendimento("Cliente Agenda Mensal", ana);
        previsao(atendimento, LocalDate.of(2030, 3, 15), "1111.11");
        previsao(atendimento, LocalDate.of(2030, 4, 10), "2222.22");

        mvc.perform(get("/agenda").param("mes", "2030-03").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Março de 2030"),
                        containsString("Cliente Agenda Mensal"),
                        containsString("1.111,11"),
                        not(containsString("2.222,22")))));
    }

    @Test
    void filtraPorResponsavel() throws Exception {
        previsao(atendimento("Cliente Da Ana Agenda", ana), LocalDate.of(2030, 5, 5), "100.00");
        previsao(atendimento("Cliente Do Bruno Agenda", bruno), LocalDate.of(2030, 5, 6), "200.00");

        mvc.perform(get("/agenda").param("mes", "2030-05").param("responsavelId", ana.getId().toString())
                        .with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Da Ana Agenda"),
                        not(containsString("Cliente Do Bruno Agenda")))));
    }

    @Test
    void periodoPersonalizadoSubstituiOMes() throws Exception {
        Atendimento atendimento = atendimento("Cliente Periodo Agenda", ana);
        previsao(atendimento, LocalDate.of(2030, 6, 28), "300.00");
        previsao(atendimento, LocalDate.of(2030, 7, 3), "400.00");
        previsao(atendimento, LocalDate.of(2030, 7, 20), "500.00");

        mvc.perform(get("/agenda").param("mes", "2030-01").param("inicio", "2030-06-25").param("fim", "2030-07-05")
                        .with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("De 25/06/2030 a 05/07/2030"),
                        containsString("300,00"),
                        containsString("400,00"),
                        not(containsString("500,00")))));
    }

    @Test
    void previsaoVencidaSemConfirmacaoGanhaRotuloDeAtrasada() throws Exception {
        LocalDate vencida = hoje.minusDays(3);
        previsao(atendimento("Cliente Atrasado Agenda", ana), vencida, "700.00");

        mvc.perform(get("/agenda").param("mes", YearMonth.from(vencida).toString())
                        .with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Atrasado Agenda"),
                        containsString("Atrasada"),
                        containsString("Ver atrasadas"))));
    }

    @Test
    void somenteAtrasadasTrazPendenciasDeMesesAnterioresSemAsLiberadas() throws Exception {
        LocalDate antiga = hoje.minusMonths(2);
        previsao(atendimento("Cliente Pendente Antigo", ana), antiga, "800.00");
        PrevisaoBeneficio liberada = previsao(atendimento("Cliente Liberado Antigo", ana), antiga, "900.00");
        liberada.confirmarLiberacao(antiga, new BigDecimal("900.00"));

        mvc.perform(get("/agenda").param("atrasadas", "true").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Pendente Antigo"),
                        not(containsString("Cliente Liberado Antigo")))));
    }

    @Test
    void parametroDeMesInvalidoUsaOMesAtual() throws Exception {
        mvc.perform(get("/agenda").param("mes", "abc").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk());
    }

    private Atendimento atendimento(String nomeCliente, Responsavel responsavel) {
        Cliente cliente = clienteRepository.save(new Cliente(nomeCliente, hoje));
        return atendimentoRepository.save(new Atendimento(cliente, tipo, responsavel, hoje.minusMonths(6)));
    }

    private PrevisaoBeneficio previsao(Atendimento atendimento, LocalDate data, String valor) {
        return previsaoRepository.save(new PrevisaoBeneficio(atendimento, data, new BigDecimal(valor),
                TipoPagamento.PRIMEIRO_PAGAMENTO));
    }
}
