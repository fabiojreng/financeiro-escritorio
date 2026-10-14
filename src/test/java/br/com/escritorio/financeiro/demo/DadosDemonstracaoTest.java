package br.com.escritorio.financeiro.demo;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.repository.AcordoHonorariosRepository;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.CategoriaDespesaRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.DespesaRepository;
import br.com.escritorio.financeiro.repository.FormaPagamentoDespesaRepository;
import br.com.escritorio.financeiro.repository.ParcelaHonorarioRepository;
import br.com.escritorio.financeiro.repository.PrevisaoBeneficioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import br.com.escritorio.financeiro.repository.RepasseRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.service.AuditoriaService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O carregador é montado à mão: ativar o profile demo aqui faria os dados serem gravados no banco
 * de testes na subida do contexto, fora da transação de cada teste.
 */
@Transactional
class DadosDemonstracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ResponsavelRepository responsavelRepository;
    @Autowired
    private TipoServicoRepository tipoServicoRepository;
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
    @Autowired
    private RepasseRepository repasseRepository;
    @Autowired
    private CategoriaDespesaRepository categoriaDespesaRepository;
    @Autowired
    private FormaPagamentoDespesaRepository formaPagamentoDespesaRepository;
    @Autowired
    private DespesaRepository despesaRepository;
    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private Clock relogio;

    private DadosDemonstracao dados;

    @BeforeEach
    void setUp() {
        dados = new DadosDemonstracao(clienteRepository, responsavelRepository, tipoServicoRepository,
                atendimentoRepository, previsaoRepository, acordoRepository, parcelaRepository, recebimentoRepository,
                repasseRepository, categoriaDespesaRepository, formaPagamentoDespesaRepository, despesaRepository,
                auditoriaService, relogio);
    }

    @Test
    void carregaCadastrosELancamentosQueRespeitamAsRegrasDoBanco() {
        List<Supplier<Long>> contagens = List.of(clienteRepository::count, responsavelRepository::count,
                atendimentoRepository::count, previsaoRepository::count, acordoRepository::count,
                parcelaRepository::count, recebimentoRepository::count, repasseRepository::count,
                despesaRepository::count, categoriaDespesaRepository::count, formaPagamentoDespesaRepository::count);
        List<Long> antes = contagens.stream().map(Supplier::get).toList();

        dados.carregar();
        entityManager.flush();

        List<Long> criados = new ArrayList<>();
        for (int i = 0; i < contagens.size(); i++) {
            criados.add(contagens.get(i).get() - antes.get(i));
        }
        assertThat(criados).containsExactly(8L, 3L, 7L, 13L, 6L, 5L, 5L, 5L, 7L, 0L, 0L);
        assertThat(clienteRepository.findAll()).filteredOn(c -> c.getNome().equals("Juliana Prado Ferreira"))
                .singleElement()
                .satisfies(c -> assertThat(c.getCpf()).isNull());
    }

    @Test
    void telasPrincipaisAbremComOsDadosDeDemonstracao() throws Exception {
        dados.carregar();
        entityManager.flush();
        entityManager.clear();

        for (String tela : List.of("/", "/agenda", "/honorarios", "/repasses", "/relatorios/previsoes",
                "/relatorios/acordos", "/despesas", "/despesas?mes=" + YearMonth.now(relogio).minusMonths(1))) {
            mvc.perform(get(tela).with(user("ana").roles("OPERADOR"))).andExpect(status().isOk());
        }
    }

    @Test
    void naoCarregaDeNovoQuandoOBancoJaTemCadastros() {
        dados.carregar();
        long clientes = clienteRepository.count();

        dados.run(null);

        assertThat(clienteRepository.count()).isEqualTo(clientes);
    }
}
