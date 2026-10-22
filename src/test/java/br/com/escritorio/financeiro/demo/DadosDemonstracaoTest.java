package br.com.escritorio.financeiro.demo;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.repository.CategoriaDespesaRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.DespesaRepository;
import br.com.escritorio.financeiro.repository.FechamentoFuncionarioRepository;
import br.com.escritorio.financeiro.repository.FormaPagamentoDespesaRepository;
import br.com.escritorio.financeiro.repository.FuncionarioRepository;
import br.com.escritorio.financeiro.repository.LancamentoFuncionarioRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.FechamentoFuncionarioService;
import br.com.escritorio.financeiro.service.FuncionarioService;
import br.com.escritorio.financeiro.service.HonorarioService;
import br.com.escritorio.financeiro.service.LancamentoFuncionarioService;
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
import static br.com.escritorio.financeiro.security.UsuariosDeTeste.administrador;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private PagamentoAgendadoRepository pagamentoRepository;
    @Autowired
    private CategoriaDespesaRepository categoriaDespesaRepository;
    @Autowired
    private FormaPagamentoDespesaRepository formaPagamentoDespesaRepository;
    @Autowired
    private DespesaRepository despesaRepository;
    @Autowired
    private HonorarioService honorarioService;
    @Autowired
    private FuncionarioService funcionarioService;
    @Autowired
    private LancamentoFuncionarioService lancamentoFuncionarioService;
    @Autowired
    private FechamentoFuncionarioService fechamentoFuncionarioService;
    @Autowired
    private FuncionarioRepository funcionarioRepository;
    @Autowired
    private LancamentoFuncionarioRepository lancamentoFuncionarioRepository;
    @Autowired
    private FechamentoFuncionarioRepository fechamentoFuncionarioRepository;
    @Autowired
    private AuditoriaService auditoriaService;
    @Autowired
    private Clock relogio;

    private DadosDemonstracao dados;

    @BeforeEach
    void setUp() {
        dados = new DadosDemonstracao(clienteRepository, responsavelRepository, tipoServicoRepository,
                pagamentoRepository, categoriaDespesaRepository, formaPagamentoDespesaRepository, despesaRepository,
                honorarioService, funcionarioService, lancamentoFuncionarioService,
                fechamentoFuncionarioService, auditoriaService, relogio);
    }

    @Test
    void carregaCadastrosELancamentosQueRespeitamAsRegrasDoBanco() {
        List<Supplier<Long>> contagens = List.of(clienteRepository::count, responsavelRepository::count,
                pagamentoRepository::count, despesaRepository::count, categoriaDespesaRepository::count,
                formaPagamentoDespesaRepository::count, funcionarioRepository::count,
                lancamentoFuncionarioRepository::count, fechamentoFuncionarioRepository::count);
        List<Long> antes = contagens.stream().map(Supplier::get).toList();

        dados.carregar();
        entityManager.flush();

        List<Long> criados = new ArrayList<>();
        for (int i = 0; i < contagens.size(); i++) {
            criados.add(contagens.get(i).get() - antes.get(i));
        }
        assertThat(criados).containsExactly(8L, 3L, 11L, 7L, 0L, 0L, 2L, 8L, 1L);
        assertThat(fechamentoFuncionarioRepository.pagos()).singleElement()
                .satisfies(f -> assertThat(f.getLiquido()).isEqualByComparingTo("1830.00"));
        assertThat(clienteRepository.findAll()).filteredOn(c -> c.getNome().equals("Juliana Prado Ferreira"))
                .singleElement()
                .satisfies(c -> assertThat(c.getCpf()).isNull());
        assertThat(pagamentoRepository.findAll())
                .filteredOn(p -> p.getCliente().getNome().equals("Maria das Graças Lima"))
                .singleElement()
                .satisfies(p -> {
                    assertThat(p.getNomesResponsaveis()).isEqualTo("Camila Torres, Rafael Moreira");
                    assertThat(p.getTotalRepasse()).isEqualByComparingTo("350.00");
                });
    }

    @Test
    void telasPrincipaisAbremComOsDadosDeDemonstracao() throws Exception {
        dados.carregar();
        entityManager.flush();
        entityManager.clear();

        for (String tela : List.of("/agenda-pagamentos", "/honorarios", "/repasses", "/relatorios/pagamentos",
                "/relatorios/honorarios", "/relatorios/repasses", "/despesas",
                "/despesas?mes=" + YearMonth.now(relogio).minusMonths(1), "/funcionarios", "/funcionarios/acertos",
                "/funcionarios/relatorios/totais")) {
            mvc.perform(get(tela).with(administrador("ana"))).andExpect(status().isOk());
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
