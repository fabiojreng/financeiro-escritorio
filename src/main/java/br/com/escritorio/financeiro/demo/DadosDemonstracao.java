package br.com.escritorio.financeiro.demo;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Despesa;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.ResponsavelPagamento;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.domain.TipoLancamentoFuncionario;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.FuncionarioForm;
import br.com.escritorio.financeiro.dto.LancamentoFuncionarioForm;
import br.com.escritorio.financeiro.dto.NegociacaoForm;
import br.com.escritorio.financeiro.repository.CategoriaDespesaRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.DespesaRepository;
import br.com.escritorio.financeiro.repository.FormaPagamentoDespesaRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.FechamentoFuncionarioService;
import br.com.escritorio.financeiro.service.FuncionarioService;
import br.com.escritorio.financeiro.service.HonorarioService;
import br.com.escritorio.financeiro.service.LancamentoFuncionarioService;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

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
    private final CategoriaDespesaRepository categoriaDespesaRepository;
    private final FormaPagamentoDespesaRepository formaPagamentoDespesaRepository;
    private final DespesaRepository despesaRepository;
    private final HonorarioService honorarioService;
    private final FuncionarioService funcionarioService;
    private final LancamentoFuncionarioService lancamentoFuncionarioService;
    private final FechamentoFuncionarioService fechamentoFuncionarioService;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    private LocalDate hoje;
    private LocalDateTime agora;

    public DadosDemonstracao(ClienteRepository clienteRepository, ResponsavelRepository responsavelRepository,
                             TipoServicoRepository tipoServicoRepository,
                             PagamentoAgendadoRepository pagamentoRepository,
                             CategoriaDespesaRepository categoriaDespesaRepository,
                             FormaPagamentoDespesaRepository formaPagamentoDespesaRepository,
                             DespesaRepository despesaRepository, HonorarioService honorarioService,
                             FuncionarioService funcionarioService,
                             LancamentoFuncionarioService lancamentoFuncionarioService,
                             FechamentoFuncionarioService fechamentoFuncionarioService,
                             AuditoriaService auditoriaService, Clock relogio) {
        this.funcionarioService = funcionarioService;
        this.lancamentoFuncionarioService = lancamentoFuncionarioService;
        this.fechamentoFuncionarioService = fechamentoFuncionarioService;
        this.clienteRepository = clienteRepository;
        this.responsavelRepository = responsavelRepository;
        this.tipoServicoRepository = tipoServicoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.categoriaDespesaRepository = categoriaDespesaRepository;
        this.formaPagamentoDespesaRepository = formaPagamentoDespesaRepository;
        this.despesaRepository = despesaRepository;
        this.honorarioService = honorarioService;
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
        agora = LocalDateTime.now(relogio);

        Responsavel helena = responsavel("Helena Duarte");
        Responsavel rafael = responsavel("Rafael Moreira");
        Responsavel camila = responsavel("Camila Torres");

        TipoServico maternidade = tipo("Salário-maternidade");
        TipoServico idade = tipo("Aposentadoria por idade");
        TipoServico incapacidade = tipo("Aposentadoria por incapacidade");
        TipoServico outros = tipo("Outros serviços");

        Cliente juliana = cliente("Juliana Prado Ferreira", "(11) 90000-0001", 3);
        pagamento(juliana, helena, maternidade, hoje.minusMonths(1), "1518.00", StatusPagamento.RECEBIDO,
                "450.00", SituacaoHonorarioPagamento.RECEBIDO, "120.00", SituacaoRepassePagamento.PAGO);
        pagamento(juliana, helena, maternidade, hoje, "1518.00", StatusPagamento.AGENDADO,
                "450.00", SituacaoHonorarioPagamento.A_RECEBER, "120.00", SituacaoRepassePagamento.PENDENTE);
        pagamento(juliana, helena, maternidade, hoje.plusMonths(1), "1518.00", StatusPagamento.AGENDADO,
                "450.00", SituacaoHonorarioPagamento.A_RECEBER, null, null);

        Cliente antonio = cliente("Antônio Carlos Ribeiro", "(11) 90000-0002", 8);
        PagamentoAgendado honorarioNegociado = pagamento(antonio, rafael, idade, hoje.minusDays(10), "18450.00",
                StatusPagamento.AGENDADO, "5535.00", SituacaoHonorarioPagamento.A_RECEBER, "200.00",
                SituacaoRepassePagamento.PENDENTE);
        negociar(honorarioNegociado, "4000.00", 2, hoje.minusDays(10),
                "Cliente combinou desconto e parcelamento na demonstração.");
        PagamentoAgendado honorarioParcial = pagamento(antonio, rafael, idade, hoje.plusDays(3), "2350.00",
                StatusPagamento.AGENDADO, "700.00", SituacaoHonorarioPagamento.PARCIAL, null, null);
        honorarioParcial.setValorHonorarioPago(new BigDecimal("300.00"));
        pagamentoRepository.save(honorarioParcial);

        Cliente maria = cliente("Maria das Graças Lima", "(11) 90000-0003", 6);
        PagamentoAgendado compartilhado = pagamento(maria, camila, incapacidade, hoje.minusDays(6), "3275.40",
                StatusPagamento.RECEBIDO, "2500.00", SituacaoHonorarioPagamento.RECEBIDO, "250.00",
                SituacaoRepassePagamento.PAGO);
        compartilhar(compartilhado, rafael, "100.00", SituacaoRepassePagamento.PENDENTE);

        Cliente jose = cliente("José Aparecido Nunes", "(11) 90000-0004", 4);
        pagamento(jose, helena, idade, hoje, "2100.00", StatusPagamento.AGENDADO,
                null, null, "180.00", SituacaoRepassePagamento.PENDENTE);

        Cliente francisca = cliente("Francisca Souza Martins", "(11) 90000-0005", 2);
        pagamento(francisca, camila, maternidade, hoje.plusDays(20), "6072.00", StatusPagamento.AGENDADO,
                "1500.00", SituacaoHonorarioPagamento.A_RECEBER, "150.00", SituacaoRepassePagamento.PENDENTE);

        Cliente paulo = cliente("Paulo Henrique Alves", "(11) 90000-0006", 10);
        paulo.setAtivo(false);
        pagamento(paulo, rafael, outros, hoje.minusMonths(2), "1412.00", StatusPagamento.AGENDADO,
                null, null, null, null);

        Cliente luzia = cliente("Luzia Fernandes Costa", "(11) 90000-0007", 5);
        pagamento(luzia, helena, idade, hoje.minusMonths(1).minusDays(5), "9800.00", StatusPagamento.RECEBIDO,
                "1500.00", SituacaoHonorarioPagamento.RECEBIDO, "500.00", SituacaoRepassePagamento.PAGO);
        pagamento(luzia, helena, idade, hoje.plusMonths(1), "1518.00", StatusPagamento.AGENDADO,
                "1440.00", SituacaoHonorarioPagamento.A_RECEBER, null, null);

        cliente("Roberto Dias Silva", "(11) 90000-0008", 0);

        // Despesas do mês passado (planilha a enviar à contabilidade), uma cancelada, e uma do mês atual.
        LocalDate inicioMesPassado = hoje.minusMonths(1).withDayOfMonth(1);
        String referencia = String.format("%02d/%d", inicioMesPassado.getMonthValue(), inicioMesPassado.getYear());
        despesa(inicioMesPassado.plusDays(4), "Energia Eletrica", "DEB CX ECO", "Despesa c/ Energia, ref. " + referencia,
                referencia, "412.37", "Companhia de Energia Fictícia");
        despesa(inicioMesPassado.plusDays(6), "Telefonia Fixa", "BB", "Despesa c/ Telefone, ref. " + referencia,
                referencia, "129.90", "Operadora de Telefonia Fictícia");
        despesa(inicioMesPassado.plusDays(9), "Fornecedor", "TRF BB", "Duplicata, ref. NF N. 1520",
                "1520", "386.00", "Papelaria Fictícia Ltda.");
        despesa(inicioMesPassado.plusDays(9), "Fornecedor", "TRF BB", "Duplicata, ref. NF N. 1520",
                "1520", "386.00", "Papelaria Fictícia Ltda.")
                .cancelar("Lançada em duplicidade.", "sistema", agora);
        despesa(inicioMesPassado.plusDays(14), "Honorários Contábil", "TRF BB", "Honorários Contábil, ref. " + referencia,
                referencia, "950.00", "Contabilidade Fictícia");
        despesa(inicioMesPassado.plusDays(19), "Imposto Simples Federal", "BB", "Imposto Simples Federal, ref. " + referencia,
                referencia, "1874.52", "Receita Federal");
        despesa(hoje.withDayOfMonth(1), "Fretes", "CAIXA", "Frete", null, "45.00", "Transportadora Fictícia");

        // Funcionários: o acerto do mês passado da Beatriz é o exemplo da demanda (líquido de R$ 1.830,00), já pago.
        YearMonth mesPassado = YearMonth.from(hoje).minusMonths(1);
        Funcionario beatriz = funcionario("Beatriz Rocha Lima", "Secretária", hoje.minusMonths(14), "2500.00", 5);
        lancamento(beatriz, TipoLancamentoFuncionario.VALE, mesPassado.atDay(10), "300.00", null);
        lancamento(beatriz, TipoLancamentoFuncionario.VALE, mesPassado.atDay(20), "600.00", null);
        lancamento(beatriz, TipoLancamentoFuncionario.BONIFICACAO, mesPassado.atDay(25), "200.00",
                "Comissão por indicação de cliente");
        lancamento(beatriz, TipoLancamentoFuncionario.REEMBOLSO, mesPassado.atDay(12), "80.00",
                "Táxi para audiência");
        lancamento(beatriz, TipoLancamentoFuncionario.DESCONTO, mesPassado.atDay(15), "50.00",
                "Falta não justificada");
        fechamentoFuncionarioService.marcarPago(beatriz.getId(), mesPassado, hoje, FormaPagamento.PIX);
        lancamento(beatriz, TipoLancamentoFuncionario.VALE, hoje.withDayOfMonth(1), "400.00", null);

        Funcionario diego = funcionario("Diego Santos Almeida", "Auxiliar administrativo", hoje.minusMonths(5),
                "1800.00", 10);
        lancamento(diego, TipoLancamentoFuncionario.HORA_EXTRA, hoje.withDayOfMonth(1), "150.00",
                "Plantão de atendimento");
        lancamento(diego, TipoLancamentoFuncionario.VALE, hoje.withDayOfMonth(1), "200.00", null);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, null,
                "Dados fictícios de demonstração carregados pelo profile demo.");
        log.info("Dados de demonstração carregados: {} clientes, {} pagamentos e {} despesas fictícios.",
                clienteRepository.count(), pagamentoRepository.count(), despesaRepository.count());
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
        PagamentoAgendado pagamento = new PagamentoAgendado(cliente, List.of(responsavel), tipo, data,
                new BigDecimal(valor), "Caixa Econômica Federal");
        pagamento.setStatus(status);
        pagamento.setObservacoes(OBSERVACAO);
        if (honorario != null) {
            pagamento.setValorHonorario(new BigDecimal(honorario));
            pagamento.setSituacaoHonorario(situacaoHonorario);
        }
        if (repasse != null) {
            ResponsavelPagamento vinculo = pagamento.getResponsaveis().get(0);
            vinculo.setValorRepasse(new BigDecimal(repasse));
            vinculo.setSituacaoRepasse(situacaoRepasse);
        }
        return pagamentoRepository.save(pagamento);
    }

    /** Cliente captado por mais de um responsável: cada um com o próprio repasse. */
    private void compartilhar(PagamentoAgendado pagamento, Responsavel responsavel, String repasse,
                              SituacaoRepassePagamento situacaoRepasse) {
        ResponsavelPagamento vinculo = pagamento.adicionarResponsavel(responsavel);
        vinculo.setValorRepasse(new BigDecimal(repasse));
        vinculo.setSituacaoRepasse(situacaoRepasse);
        pagamentoRepository.save(pagamento);
    }

    private void negociar(PagamentoAgendado pagamento, String valorNegociado, int parcelas, LocalDate primeiroVencimento,
                          String motivo) {
        NegociacaoForm form = new NegociacaoForm();
        form.setValorNegociado(new BigDecimal(valorNegociado));
        form.setQuantidade(parcelas);
        form.setPrimeiroVencimento(primeiroVencimento);
        form.setMotivo(motivo);
        honorarioService.negociar(pagamento.getId(), form);
    }

    private Funcionario funcionario(String nome, String cargo, LocalDate admissao, String salario, int diaPagamento) {
        FuncionarioForm form = new FuncionarioForm();
        form.setNome(nome);
        form.setCargo(cargo);
        form.setDataAdmissao(admissao);
        form.setSalarioBase(new BigDecimal(salario));
        form.setDiaPagamento(diaPagamento);
        form.setObservacoes(OBSERVACAO);
        return funcionarioService.criar(form);
    }

    private void lancamento(Funcionario funcionario, TipoLancamentoFuncionario tipo, LocalDate data, String valor,
                            String descricao) {
        LancamentoFuncionarioForm form = new LancamentoFuncionarioForm();
        form.setTipo(tipo);
        form.setData(data);
        form.setValor(new BigDecimal(valor));
        form.setFormaPagamento(FormaPagamento.PIX);
        form.setDescricao(descricao);
        lancamentoFuncionarioService.criar(funcionario.getId(), form);
    }

    /** Usa as categorias e formas de pagamento criadas pela migração (modelo da contabilidade). */
    private Despesa despesa(LocalDate data, String categoria, String forma, String historico, String numeroDocumento,
                            String valor, String fornecedor) {
        CategoriaDespesa categoriaDespesa = categoriaDespesaRepository.findAllByOrderByAtivoDescNomeAsc().stream()
                .filter(c -> c.getNome().equalsIgnoreCase(categoria))
                .findFirst()
                .orElseGet(() -> categoriaDespesaRepository.save(new CategoriaDespesa(categoria, historico)));
        FormaPagamentoDespesa formaPagamento = formaPagamentoDespesaRepository.findAllByOrderByAtivoDescNomeAsc().stream()
                .filter(f -> f.getNome().equalsIgnoreCase(forma))
                .findFirst()
                .orElseGet(() -> formaPagamentoDespesaRepository.save(new FormaPagamentoDespesa(forma, OBSERVACAO)));
        Despesa despesa = new Despesa(data, categoriaDespesa, formaPagamento, historico, new BigDecimal(valor), fornecedor);
        despesa.setNumeroDocumento(numeroDocumento);
        despesa.setObservacoes(OBSERVACAO);
        return despesaRepository.save(despesa);
    }
}
