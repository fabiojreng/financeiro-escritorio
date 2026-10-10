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
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.domain.TipoPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.PainelIndicadores;
import br.com.escritorio.financeiro.repository.AcordoHonorariosRepository;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PrevisaoBeneficioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import br.com.escritorio.financeiro.repository.RepasseRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.service.PainelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Massa pequena em março de 2020 (no passado em relação ao relógio dos testes) para fixar o que
 * cada indicador do painel significa.
 */
@Transactional
class PainelIntegracaoTest extends IntegracaoTest {

    private static final LocalDate INICIO = LocalDate.of(2020, 3, 1);
    private static final LocalDate FIM = LocalDate.of(2020, 3, 31);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private PainelService painelService;
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

    private TipoServico tipo;
    private Responsavel responsavel;
    private int atendimentosCriados;

    @BeforeEach
    void setUp() {
        tipo = tipoServicoRepository.save(new TipoServico("Serviço Painel Teste", null));
        responsavel = responsavelRepository.save(new Responsavel("Responsável Painel Teste", null));
    }

    @Test
    void valoresDoClienteContamSomenteOBeneficioDoPeriodo() {
        Atendimento atendimento = novoAtendimento();
        previsao(atendimento, LocalDate.of(2020, 3, 5), "1000.00");
        PrevisaoBeneficio liberada = previsao(atendimento, LocalDate.of(2020, 3, 10), "2000.00");
        liberada.confirmarLiberacao(LocalDate.of(2020, 3, 12), new BigDecimal("2100.00"));
        previsao(atendimento, LocalDate.of(2020, 3, 15), "3000.00").alterarSituacao(SituacaoPrevisao.CANCELADO);
        previsao(atendimento, LocalDate.of(2020, 4, 2), "4000.00");
        PrevisaoBeneficio liberadaNoMes = previsao(atendimento, LocalDate.of(2020, 2, 20), "500.00");
        liberadaNoMes.confirmarLiberacao(LocalDate.of(2020, 3, 1), new BigDecimal("550.00"));
        previsaoRepository.flush();

        PainelIndicadores.ValoresCliente cliente = painelService.calcular(INICIO, FIM).cliente();

        assertThat(cliente.previsoes()).as("previsões de março, sem a cancelada").isEqualTo(2);
        assertThat(cliente.valorEstimado()).as("estimado das previsões de março").isEqualByComparingTo("3000.00");
        assertThat(cliente.valorConfirmado()).as("liberações com data efetiva em março")
                .isEqualByComparingTo("2650.00");
        assertThat(cliente.pendentesConfirmacao()).isEqualTo(1);
        assertThat(cliente.atrasadasNoPeriodo()).isEqualTo(1);
        assertThat(cliente.atrasadasTotal()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void valoresDoEscritorioSeparamHonorariosERepasses() {
        Atendimento comValor = novoAtendimento();
        previsao(comValor, LocalDate.of(2020, 3, 5), "9000.00").confirmarLiberacao(LocalDate.of(2020, 3, 6),
                new BigDecimal("9000.00"));
        AcordoHonorarios acordoMarco = acordo(comValor, LocalDate.of(2020, 3, 3), "3000.00");
        receber(acordoMarco, LocalDate.of(2020, 3, 20), "1000.00");
        receber(acordoMarco, LocalDate.of(2020, 4, 5), "500.00");
        receber(acordoMarco, LocalDate.of(2020, 3, 21), "200.00")
                .cancelar("Lançado em duplicidade", "admin", LocalDateTime.of(2020, 3, 22, 10, 0));

        AcordoHonorarios percentual = acordo(novoAtendimento(), LocalDate.of(2020, 3, 8), null);
        receber(percentual, LocalDate.of(2020, 3, 25), "300.00");

        AcordoHonorarios acordoJaneiro = acordo(novoAtendimento(), LocalDate.of(2020, 1, 10), "1000.00");
        receber(acordoJaneiro, LocalDate.of(2020, 2, 1), "400.00");

        repasse(comValor, LocalDate.of(2020, 3, 10), "200.00");
        repasse(comValor, LocalDate.of(2020, 2, 10), "100.00");
        repasse(comValor, LocalDate.of(2020, 4, 10), "999.00");
        repasse(comValor, LocalDate.of(2020, 3, 12), "300.00")
                .registrarPagamento(LocalDate.of(2020, 3, 15), new BigDecimal("280.00"));
        repasse(comValor, LocalDate.of(2020, 3, 10), "50.00")
                .cancelar("Não combinado", "admin", LocalDateTime.of(2020, 3, 11, 10, 0));
        repasseRepository.flush();
        recebimentoRepository.flush();

        PainelIndicadores.ValoresEscritorio escritorio = painelService.calcular(INICIO, FIM).escritorio();

        assertThat(escritorio.honorariosContratados()).as("acordos firmados em março com valor")
                .isEqualByComparingTo("3000.00");
        assertThat(escritorio.acordosSemValor()).isEqualTo(1);
        assertThat(escritorio.honorariosRecebidos()).as("caixa de março, sem o cancelado, inclusive do percentual")
                .isEqualByComparingTo("1300.00");
        assertThat(escritorio.honorariosPendentes()).as("(3000 + 1000) contratados até 31/03 - (1000 + 400) recebidos até 31/03")
                .isEqualByComparingTo("2600.00");
        assertThat(escritorio.repassesPendentes()).as("pendentes até 31/03, inclusive o atrasado de fevereiro")
                .isEqualByComparingTo("300.00");
        assertThat(escritorio.quantidadeRepassesPendentes()).isEqualTo(2);
        assertThat(escritorio.repassesPagos()).isEqualByComparingTo("280.00");
    }

    @Test
    void painelMostraOsDoisBlocosEOPeriodo() throws Exception {
        Atendimento atendimento = novoAtendimento();
        previsao(atendimento, LocalDate.of(2020, 3, 5), "1234.56");
        acordo(atendimento, LocalDate.of(2020, 3, 3), "7654.32");
        previsaoRepository.flush();

        mvc.perform(get("/").param("mes", "2020-03").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Março de 2020"),
                        containsString("Valores do cliente"),
                        containsString("Valores do escritório"),
                        containsString("1.234,56"),
                        containsString("7.654,32"))));
    }

    @Test
    void periodoPersonalizadoEParametroInvalidoNaoQuebramOPainel() throws Exception {
        mvc.perform(get("/").param("inicio", "2020-03-31").param("fim", "2020-03-01")
                        .with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("De 01/03/2020 a 31/03/2020")));

        mvc.perform(get("/").param("mes", "abc").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk());
    }

    private Atendimento novoAtendimento() {
        Cliente cliente = clienteRepository.save(new Cliente("Cliente Painel " + (++atendimentosCriados),
                LocalDate.of(2020, 1, 1)));
        return atendimentoRepository.save(new Atendimento(cliente, tipo, responsavel, LocalDate.of(2020, 1, 1)));
    }

    private PrevisaoBeneficio previsao(Atendimento atendimento, LocalDate data, String valor) {
        return previsaoRepository.save(new PrevisaoBeneficio(atendimento, data, new BigDecimal(valor),
                TipoPagamento.PRIMEIRO_PAGAMENTO));
    }

    private AcordoHonorarios acordo(Atendimento atendimento, LocalDate data, String valor) {
        AcordoHonorarios acordo;
        if (valor == null) {
            acordo = new AcordoHonorarios(atendimento, FormaCobranca.PERCENTUAL, data);
            acordo.definirCobranca(FormaCobranca.PERCENTUAL, new BigDecimal("30"), "Retroativos");
        } else {
            acordo = new AcordoHonorarios(atendimento, FormaCobranca.VALOR_FIXO, data);
            acordo.setValorContratado(new BigDecimal(valor));
        }
        return acordoRepository.save(acordo);
    }

    private RecebimentoHonorario receber(AcordoHonorarios acordo, LocalDate data, String valor) {
        return recebimentoRepository.save(new RecebimentoHonorario(acordo, null, data, new BigDecimal(valor),
                FormaPagamento.PIX));
    }

    private Repasse repasse(Atendimento atendimento, LocalDate data, String valor) {
        return repasseRepository.save(new Repasse(atendimento, responsavel, "Comissão", new BigDecimal(valor), data));
    }
}
