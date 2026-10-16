package br.com.escritorio.financeiro.demo;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
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
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
    private AuditoriaService auditoriaService;
    @Autowired
    private Clock relogio;

    private DadosDemonstracao dados;

    @BeforeEach
    void setUp() {
        dados = new DadosDemonstracao(clienteRepository, responsavelRepository, tipoServicoRepository,
                pagamentoRepository, auditoriaService, relogio);
    }

    @Test
    void carregaCadastrosELancamentosQueRespeitamAsRegrasDoBanco() {
        List<Supplier<Long>> contagens = List.of(clienteRepository::count, responsavelRepository::count,
                pagamentoRepository::count);
        long clientesAntes = contagens.get(0).get();
        long responsaveisAntes = contagens.get(1).get();
        long pagamentosAntes = contagens.get(2).get();

        dados.carregar();
        entityManager.flush();

        assertThat(clienteRepository.count() - clientesAntes).isEqualTo(8L);
        assertThat(responsavelRepository.count() - responsaveisAntes).isEqualTo(3L);
        assertThat(pagamentoRepository.count() - pagamentosAntes).isEqualTo(11L);
        assertThat(clienteRepository.findAll()).filteredOn(c -> c.getNome().equals("Juliana Prado Ferreira"))
                .singleElement()
                .satisfies(c -> assertThat(c.getCpf()).isNull());
    }

    @Test
    void telasPrincipaisAbremComOsDadosDeDemonstracao() throws Exception {
        dados.carregar();
        entityManager.flush();
        entityManager.clear();

        for (String tela : List.of("/agenda-pagamentos", "/honorarios", "/repasses", "/relatorios/pagamentos",
                "/relatorios/honorarios", "/relatorios/repasses")) {
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
