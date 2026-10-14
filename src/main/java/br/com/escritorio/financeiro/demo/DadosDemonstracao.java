package br.com.escritorio.financeiro.demo;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Despesa;
import br.com.escritorio.financeiro.domain.FormaCobranca;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.domain.TipoPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
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

/**
 * Dados fictícios para apresentar o sistema. Só existe no profile {@code demo}, que aponta para um
 * banco próprio ({@code DB_DEMO_NAME}), e só carrega quando o banco ainda não tem clientes nem
 * responsáveis. As datas são relativas ao dia em que a aplicação sobe, para a agenda e o painel
 * sempre terem movimento. Nenhum cliente fictício tem CPF.
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
    private final AtendimentoRepository atendimentoRepository;
    private final PrevisaoBeneficioRepository previsaoRepository;
    private final AcordoHonorariosRepository acordoRepository;
    private final ParcelaHonorarioRepository parcelaRepository;
    private final RecebimentoHonorarioRepository recebimentoRepository;
    private final RepasseRepository repasseRepository;
    private final CategoriaDespesaRepository categoriaDespesaRepository;
    private final FormaPagamentoDespesaRepository formaPagamentoDespesaRepository;
    private final DespesaRepository despesaRepository;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    private LocalDate hoje;
    private LocalDateTime agora;

    public DadosDemonstracao(ClienteRepository clienteRepository, ResponsavelRepository responsavelRepository,
                             TipoServicoRepository tipoServicoRepository, AtendimentoRepository atendimentoRepository,
                             PrevisaoBeneficioRepository previsaoRepository, AcordoHonorariosRepository acordoRepository,
                             ParcelaHonorarioRepository parcelaRepository,
                             RecebimentoHonorarioRepository recebimentoRepository, RepasseRepository repasseRepository,
                             CategoriaDespesaRepository categoriaDespesaRepository,
                             FormaPagamentoDespesaRepository formaPagamentoDespesaRepository,
                             DespesaRepository despesaRepository, AuditoriaService auditoriaService, Clock relogio) {
        this.clienteRepository = clienteRepository;
        this.responsavelRepository = responsavelRepository;
        this.tipoServicoRepository = tipoServicoRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.previsaoRepository = previsaoRepository;
        this.acordoRepository = acordoRepository;
        this.parcelaRepository = parcelaRepository;
        this.recebimentoRepository = recebimentoRepository;
        this.repasseRepository = repasseRepository;
        this.categoriaDespesaRepository = categoriaDespesaRepository;
        this.formaPagamentoDespesaRepository = formaPagamentoDespesaRepository;
        this.despesaRepository = despesaRepository;
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

        // Competências mensais, honorários parcelados com uma parcela vencida.
        Atendimento juliana = atendimento(cliente("Juliana Prado Ferreira", "(11) 90000-0001", 3),
                maternidade, helena, SituacaoAtendimento.EM_ANDAMENTO);
        liberar(previsao(juliana, hoje.minusMonths(1), "1518.00", TipoPagamento.COMPETENCIA_MENSAL),
                hoje.minusMonths(1).plusDays(1), "1518.00");
        previsao(juliana, hoje, "1518.00", TipoPagamento.COMPETENCIA_MENSAL);
        previsao(juliana, hoje.plusMonths(1), "1518.00", TipoPagamento.COMPETENCIA_MENSAL);
        previsao(juliana, hoje.plusMonths(2), "1518.00", TipoPagamento.COMPETENCIA_MENSAL);
        AcordoHonorarios acordoJuliana = acordo(juliana, FormaCobranca.VALOR_FIXO, "1800.00", hoje.minusMonths(3));
        acordoJuliana.setQuantidadeParcelas(3);
        ParcelaHonorario primeiraJuliana = parcela(acordoJuliana, 1, hoje.minusMonths(1), "600.00");
        parcela(acordoJuliana, 2, hoje.minusDays(3), "600.00");
        parcela(acordoJuliana, 3, hoje.plusMonths(1), "600.00");
        receber(acordoJuliana, primeiraJuliana, hoje.minusMonths(1).plusDays(2), "600.00", FormaPagamento.PIX);

        // Atrasados previstos e já vencidos, primeiro pagamento nos próximos dias, cobrança percentual.
        Atendimento antonio = atendimento(cliente("Antônio Carlos Ribeiro", "(11) 90000-0002", 8),
                idade, rafael, SituacaoAtendimento.EM_ANDAMENTO);
        antonio.setNumeroProcesso("0000001-00.0000.0.00.0000");
        previsao(antonio, hoje.minusDays(10), "18450.00", TipoPagamento.RETROATIVO);
        previsao(antonio, hoje.plusDays(3), "2350.00", TipoPagamento.PRIMEIRO_PAGAMENTO)
                .alterarSituacao(SituacaoPrevisao.AGUARDANDO_CONFIRMACAO);
        AcordoHonorarios acordoAntonio = acordoPercentual(antonio, "30.00", "Atrasados do benefício", "5535.00",
                hoje.minusMonths(8));
        acordoAntonio.setCondicoes("Pagamento à vista quando os atrasados forem liberados.");
        repasse(antonio, rafael, "Comissão de indicação", "200.00", hoje.minusDays(8))
                .cancelar("Lançado no atendimento errado.", "sistema", agora);

        // Liberação confirmada neste mês, honorários quitados e repasse pago.
        Atendimento maria = atendimento(cliente("Maria das Graças Lima", "(11) 90000-0003", 6),
                incapacidade, camila, SituacaoAtendimento.CONCLUIDO);
        liberar(previsao(maria, hoje.minusDays(6), "3200.00", TipoPagamento.PRIMEIRO_PAGAMENTO),
                hoje.minusDays(4), "3275.40");
        AcordoHonorarios acordoMaria = acordo(maria, FormaCobranca.VALOR_FIXO, "2500.00", hoje.minusMonths(6));
        receber(acordoMaria, null, hoje.minusDays(2), "2500.00", FormaPagamento.TRANSFERENCIA);
        repasse(maria, rafael, "Comissão de indicação", "250.00", hoje.minusDays(1))
                .registrarPagamento(hoje.minusDays(1), new BigDecimal("250.00"));

        // Previsão para hoje, acordo ainda sem valor e repasse atrasado.
        Atendimento jose = atendimento(cliente("José Aparecido Nunes", "(11) 90000-0004", 4),
                idade, helena, SituacaoAtendimento.EM_ANDAMENTO);
        previsao(jose, hoje, "2100.00", TipoPagamento.PRIMEIRO_PAGAMENTO);
        AcordoHonorarios acordoJose = acordo(jose, FormaCobranca.OUTRA, null, hoje.minusMonths(4));
        acordoJose.setCondicoes("Valor a definir depois da concessão.");
        repasse(jose, camila, "Ajuda de custo para diligência", "180.00", hoje.minusDays(5));

        // Previsão adiada e remarcada, recebimento cancelado por duplicidade.
        Atendimento francisca = atendimento(cliente("Francisca Souza Martins", "(11) 90000-0005", 2),
                maternidade, camila, SituacaoAtendimento.EM_ANDAMENTO);
        PrevisaoBeneficio adiada = previsao(francisca, hoje.minusDays(15), "6072.00", TipoPagamento.PRIMEIRO_PAGAMENTO);
        adiada.alterarSituacao(SituacaoPrevisao.ADIADO);
        adiada.setObservacoes("O INSS pediu documento complementar.");
        previsao(francisca, hoje.plusDays(20), "6072.00", TipoPagamento.PRIMEIRO_PAGAMENTO);
        AcordoHonorarios acordoFrancisca = acordo(francisca, FormaCobranca.VALOR_FIXO, "1500.00", hoje.minusMonths(2));
        acordoFrancisca.setQuantidadeParcelas(2);
        ParcelaHonorario primeiraFrancisca = parcela(acordoFrancisca, 1, hoje.minusDays(20), "750.00");
        parcela(acordoFrancisca, 2, hoje.plusDays(10), "750.00");
        receber(acordoFrancisca, primeiraFrancisca, hoje.minusDays(18), "750.00", FormaPagamento.DINHEIRO);
        receber(acordoFrancisca, null, hoje.minusDays(18), "750.00", FormaPagamento.DINHEIRO)
                .cancelar("Lançado em duplicidade.", "sistema", agora);
        repasse(francisca, rafael, "Comissão de indicação", "150.00", hoje.plusDays(12));

        // Atendimento cancelado de um cliente inativo.
        Cliente paulo = cliente("Paulo Henrique Alves", "(11) 90000-0006", 10);
        paulo.setAtivo(false);
        Atendimento atendimentoPaulo = atendimento(paulo, outros, rafael, SituacaoAtendimento.CANCELADO);
        previsao(atendimentoPaulo, hoje.minusMonths(2), "1412.00", TipoPagamento.OUTRO)
                .alterarSituacao(SituacaoPrevisao.CANCELADO);

        // Atrasados liberados no mês passado, competência no mês seguinte e honorários com saldo.
        Atendimento luzia = atendimento(cliente("Luzia Fernandes Costa", "(11) 90000-0007", 5),
                idade, helena, SituacaoAtendimento.EM_ANDAMENTO);
        liberar(previsao(luzia, hoje.minusMonths(1).minusDays(5), "9800.00", TipoPagamento.RETROATIVO),
                hoje.minusMonths(1).minusDays(3), "9800.00");
        previsao(luzia, hoje.plusMonths(1), "1518.00", TipoPagamento.COMPETENCIA_MENSAL);
        AcordoHonorarios acordoLuzia = acordo(luzia, FormaCobranca.VALOR_FIXO, "2940.00", hoje.minusMonths(5));
        receber(acordoLuzia, null, hoje.minusMonths(1), "1500.00", FormaPagamento.PIX);
        repasse(luzia, helena, "Participação no atendimento", "500.00", hoje.minusDays(20))
                .registrarPagamento(hoje.minusDays(20), new BigDecimal("500.00"));

        // Cliente recém-cadastrado, ainda sem atendimento.
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

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, null,
                "Dados fictícios de demonstração carregados pelo profile demo.");
        log.info("Dados de demonstração carregados: {} clientes, {} atendimentos e {} previsões fictícios.",
                clienteRepository.count(), atendimentoRepository.count(), previsaoRepository.count());
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

    private Atendimento atendimento(Cliente cliente, TipoServico tipo, Responsavel responsavel,
                                    SituacaoAtendimento situacao) {
        Atendimento atendimento = new Atendimento(cliente, tipo, responsavel, cliente.getDataCadastro());
        atendimento.setSituacao(situacao);
        atendimento.setObservacoes(OBSERVACAO);
        return atendimentoRepository.save(atendimento);
    }

    private PrevisaoBeneficio previsao(Atendimento atendimento, LocalDate data, String valor, TipoPagamento tipo) {
        return previsaoRepository.save(new PrevisaoBeneficio(atendimento, data, new BigDecimal(valor), tipo));
    }

    private void liberar(PrevisaoBeneficio previsao, LocalDate dataEfetiva, String valorConfirmado) {
        previsao.confirmarLiberacao(dataEfetiva, new BigDecimal(valorConfirmado));
    }

    private AcordoHonorarios acordo(Atendimento atendimento, FormaCobranca forma, String valorContratado,
                                    LocalDate data) {
        AcordoHonorarios acordo = new AcordoHonorarios(atendimento, forma, data);
        acordo.setValorContratado(valorContratado == null ? null : new BigDecimal(valorContratado));
        return acordoRepository.save(acordo);
    }

    private AcordoHonorarios acordoPercentual(Atendimento atendimento, String percentual, String baseCalculo,
                                              String valorContratado, LocalDate data) {
        AcordoHonorarios acordo = new AcordoHonorarios(atendimento, FormaCobranca.PERCENTUAL, data);
        acordo.definirCobranca(FormaCobranca.PERCENTUAL, new BigDecimal(percentual), baseCalculo);
        acordo.setValorContratado(new BigDecimal(valorContratado));
        return acordoRepository.save(acordo);
    }

    private ParcelaHonorario parcela(AcordoHonorarios acordo, int numero, LocalDate vencimento, String valor) {
        return parcelaRepository.save(new ParcelaHonorario(acordo, numero, vencimento, new BigDecimal(valor)));
    }

    private RecebimentoHonorario receber(AcordoHonorarios acordo, ParcelaHonorario parcela, LocalDate data,
                                         String valor, FormaPagamento forma) {
        BigDecimal quantia = new BigDecimal(valor);
        if (parcela != null) {
            parcela.atualizarPagamento(quantia);
        }
        return recebimentoRepository.save(new RecebimentoHonorario(acordo, parcela, data, quantia, forma));
    }

    private Repasse repasse(Atendimento atendimento, Responsavel destinatario, String descricao, String valor,
                            LocalDate dataPrevista) {
        return repasseRepository.save(new Repasse(atendimento, destinatario, descricao, new BigDecimal(valor),
                dataPrevista));
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
