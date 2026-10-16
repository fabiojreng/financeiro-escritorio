package br.com.escritorio.financeiro.demo;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Dados fictícios para apresentar o sistema. Só existe no profile {@code demo}, que aponta para um
 * banco próprio ({@code DB_DEMO_NAME}), e só carrega quando o banco ainda não tem clientes nem
 * responsáveis. As datas são relativas ao dia em que a aplicação sobe. Nenhum cliente fictício tem CPF.
 */
@Component
@Profile("demo")
public class DadosDemonstracao implements ApplicationRunner {

    static final String ENTIDADE = "Demonstração";

    private static final Logger log = LoggerFactory.getLogger(DadosDemonstracao.class);
    private static final String OBSERVACAO = "Dado fictício para demonstração.";

    private final ClienteRepository clienteRepository;
    private final ResponsavelRepository responsavelRepository;
    private final TipoServicoRepository tipoServicoRepository;
    private final PagamentoAgendadoRepository pagamentoRepository;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    private LocalDate hoje;

    public DadosDemonstracao(ClienteRepository clienteRepository, ResponsavelRepository responsavelRepository,
                             TipoServicoRepository tipoServicoRepository,
                             PagamentoAgendadoRepository pagamentoRepository, AuditoriaService auditoriaService,
                             Clock relogio) {
        this.clienteRepository = clienteRepository;
        this.responsavelRepository = responsavelRepository;
        this.tipoServicoRepository = tipoServicoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (clienteRepository.count() > 0 || responsavelRepository.count() > 0) {
            log.info("O banco já tem cadastros; os dados de demonstração não foram carregados.");
            return;
        }
        carregar();
    }

    void carregar() {
        hoje = LocalDate.now(relogio);

        Responsavel helena = responsavel("Helena Duarte");
        Responsavel rafael = responsavel("Rafael Moreira");
        Responsavel camila = responsavel("Camila Torres");

        TipoServico maternidade = tipo("Salário-maternidade");
        TipoServico idade = tipo("Aposentadoria por idade");
        TipoServico incapacidade = tipo("Aposentadoria por incapacidade");
        TipoServico outros = tipo("Outros serviços");

        Cliente juliana = cliente("Juliana Prado Ferreira", "(11) 90000-0001", 3);
        pagamento(juliana, helena, maternidade, hoje.minusMonths(1), "1518.00", StatusPagamento.PAGO,
                "450.00", SituacaoHonorarioPagamento.RECEBIDO, "120.00", SituacaoRepassePagamento.PAGO);
        pagamento(juliana, helena, maternidade, hoje, "1518.00", StatusPagamento.AGENDADO,
                "450.00", SituacaoHonorarioPagamento.A_RECEBER, "120.00", SituacaoRepassePagamento.PENDENTE);
        pagamento(juliana, helena, maternidade, hoje.plusMonths(1), "1518.00", StatusPagamento.AGENDADO,
                "450.00", SituacaoHonorarioPagamento.A_RECEBER, null, null);

        Cliente antonio = cliente("Antônio Carlos Ribeiro", "(11) 90000-0002", 8);
        pagamento(antonio, rafael, idade, hoje.minusDays(10), "18450.00", StatusPagamento.PENDENTE,
                "5535.00", SituacaoHonorarioPagamento.A_RECEBER, "200.00", SituacaoRepassePagamento.PENDENTE);
        pagamento(antonio, rafael, idade, hoje.plusDays(3), "2350.00", StatusPagamento.AGENDADO,
                "700.00", SituacaoHonorarioPagamento.A_RECEBER, null, null);

        Cliente maria = cliente("Maria das Graças Lima", "(11) 90000-0003", 6);
        pagamento(maria, camila, incapacidade, hoje.minusDays(6), "3275.40", StatusPagamento.PAGO,
                "2500.00", SituacaoHonorarioPagamento.RECEBIDO, "250.00", SituacaoRepassePagamento.PAGO);

        Cliente jose = cliente("José Aparecido Nunes", "(11) 90000-0004", 4);
        pagamento(jose, helena, idade, hoje, "2100.00", StatusPagamento.AGENDADO,
                null, null, "180.00", SituacaoRepassePagamento.PENDENTE);

        Cliente francisca = cliente("Francisca Souza Martins", "(11) 90000-0005", 2);
        pagamento(francisca, camila, maternidade, hoje.plusDays(20), "6072.00", StatusPagamento.AGENDADO,
                "1500.00", SituacaoHonorarioPagamento.A_RECEBER, "150.00", SituacaoRepassePagamento.PENDENTE);

        Cliente paulo = cliente("Paulo Henrique Alves", "(11) 90000-0006", 10);
        paulo.setAtivo(false);
        pagamento(paulo, rafael, outros, hoje.minusMonths(2), "1412.00", StatusPagamento.PENDENTE,
                null, null, null, null);

        Cliente luzia = cliente("Luzia Fernandes Costa", "(11) 90000-0007", 5);
        pagamento(luzia, helena, idade, hoje.minusMonths(1).minusDays(5), "9800.00", StatusPagamento.PAGO,
                "1500.00", SituacaoHonorarioPagamento.RECEBIDO, "500.00", SituacaoRepassePagamento.PAGO);
        pagamento(luzia, helena, idade, hoje.plusMonths(1), "1518.00", StatusPagamento.AGENDADO,
                "1440.00", SituacaoHonorarioPagamento.A_RECEBER, null, null);

        cliente("Roberto Dias Silva", "(11) 90000-0008", 0);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, null,
                "Dados fictícios de demonstração carregados pelo profile demo.");
        log.info("Dados de demonstração carregados: {} clientes e {} pagamentos fictícios.",
                clienteRepository.count(), pagamentoRepository.count());
    }

    private Responsavel responsavel(String nome) {
        return responsavelRepository.save(new Responsavel(nome, OBSERVACAO));
    }

    private TipoServico tipo(String nome) {
        return tipoServicoRepository.findAllByOrderByAtivoDescNomeAsc().stream()
                .filter(t -> t.getNome().equalsIgnoreCase(nome))
                .findFirst()
                .orElseGet(() -> tipoServicoRepository.save(new TipoServico(nome, OBSERVACAO)));
    }

    private Cliente cliente(String nome, String telefone, int mesesDeCadastro) {
        Cliente cliente = new Cliente(nome, hoje.minusMonths(mesesDeCadastro));
        cliente.setTelefone(telefone);
        cliente.setObservacoes(OBSERVACAO);
        return clienteRepository.save(cliente);
    }

    private PagamentoAgendado pagamento(Cliente cliente, Responsavel responsavel, TipoServico tipo, LocalDate data,
                                        String valor, StatusPagamento status, String honorario,
                                        SituacaoHonorarioPagamento situacaoHonorario, String repasse,
                                        SituacaoRepassePagamento situacaoRepasse) {
        PagamentoAgendado pagamento = new PagamentoAgendado(cliente, responsavel, tipo, data, new BigDecimal(valor),
                "Caixa Econômica Federal");
        pagamento.setStatus(status);
        pagamento.setObservacoes(OBSERVACAO);
        if (honorario != null) {
            pagamento.setValorHonorario(new BigDecimal(honorario));
            pagamento.setSituacaoHonorario(situacaoHonorario);
        }
        if (repasse != null) {
            pagamento.setValorRepasse(new BigDecimal(repasse));
            pagamento.setSituacaoRepasse(situacaoRepasse);
        }
        return pagamentoRepository.save(pagamento);
    }
}
