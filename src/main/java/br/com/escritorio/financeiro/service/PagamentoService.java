package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.CalculoHonorarios;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Cpf;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.ResponsavelPagamento;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoNegociacao;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.AgendaPagamentos;
import br.com.escritorio.financeiro.dto.AgendaRepasses;
import br.com.escritorio.financeiro.dto.FiltroPagamentos;
import br.com.escritorio.financeiro.dto.FiltroRelatorio;
import br.com.escritorio.financeiro.dto.PagamentoForm;
import br.com.escritorio.financeiro.repository.NegociacaoHonorarioRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelPagamentoRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Agenda de pagamentos da recepção. Cada pagamento fica ligado direto ao cliente, que é
 * encontrado ou cadastrado pelo CPF no próprio lançamento.
 */
@Service
public class PagamentoService {

    public static final String ENTIDADE = "Pagamento agendado";

    private static final String ORIGEM = "agenda de pagamentos";
    private static final String CAMPO_NOME = "nomeCliente";
    private static final String CAMPO_CPF = "cpf";
    private static final String CAMPO_RESPONSAVEIS = "responsavelIds";
    private static final String CAMPO_VALOR_HONORARIO = "valorHonorario";
    private static final String CAMPO_VALOR_HONORARIO_PAGO = "valorHonorarioPago";
    private static final String CAMPO_SITUACAO_HONORARIO = "situacaoHonorario";
    private static final String CAMPO_PAGO_CONTINUACAO = "valorPagoContinuacao";
    private static final int DIGITOS_MINIMOS_BUSCA_CPF = 3;

    private final PagamentoAgendadoRepository pagamentoRepository;
    private final ResponsavelRepository responsavelRepository;
    private final ResponsavelPagamentoRepository responsavelPagamentoRepository;
    private final TipoServicoRepository tipoServicoRepository;
    private final ClienteService clienteService;
    private final AuditoriaService auditoriaService;
    private final NegociacaoHonorarioRepository negociacaoHonorarioRepository;
    private final Clock relogio;

    public PagamentoService(PagamentoAgendadoRepository pagamentoRepository, ResponsavelRepository responsavelRepository,
                            ResponsavelPagamentoRepository responsavelPagamentoRepository,
                            TipoServicoRepository tipoServicoRepository, ClienteService clienteService,
                            AuditoriaService auditoriaService, NegociacaoHonorarioRepository negociacaoHonorarioRepository,
                            Clock relogio) {
        this.pagamentoRepository = pagamentoRepository;
        this.responsavelRepository = responsavelRepository;
        this.responsavelPagamentoRepository = responsavelPagamentoRepository;
        this.tipoServicoRepository = tipoServicoRepository;
        this.clienteService = clienteService;
        this.auditoriaService = auditoriaService;
        this.negociacaoHonorarioRepository = negociacaoHonorarioRepository;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public PagamentoAgendado buscar(Long id) {
        return pagamentoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Pagamento não encontrado."));
    }

    @Transactional(readOnly = true)
    public AgendaPagamentos agenda(FiltroPagamentos filtro) {
        CriterioBusca criterio = criterio(filtro.getBusca(), filtro.getObservacao());
        List<StatusPagamento> status = filtro.getStatus() == null
                ? Arrays.asList(StatusPagamento.values())
                : List.of(filtro.getStatus());
        return new AgendaPagamentos(pagamentoRepository.pesquisar(filtro.getInicio(), filtro.getFim(), status,
                criterio.padraoCliente, criterio.padraoCpf, criterio.padraoObservacao, filtro.getResponsavelId(),
                filtro.getTipoServicoId()));
    }

    @Transactional(readOnly = true)
    public AgendaPagamentos honorarios(FiltroPagamentos filtro) {
        return new AgendaPagamentos(pesquisarHonorarios(filtro, false));
    }

    @Transactional(readOnly = true)
    public List<PagamentoAgendado> pesquisarHonorarios(FiltroPagamentos filtro, boolean somenteComValor) {
        CriterioBusca criterio = criterio(filtro.getBusca(), filtro.getObservacao());
        return pagamentoRepository.pesquisarHonorarios(filtro.getInicio(), filtro.getFim(),
                somenteComValor, null, criterio.padraoCliente, criterio.padraoCpf, criterio.padraoObservacao,
                filtro.getResponsavelId(), filtro.getTipoServicoId());
    }

    @Transactional(readOnly = true)
    public List<PagamentoAgendado> pesquisarHonorarios(FiltroRelatorio filtro, boolean somenteComValor) {
        CriterioBusca criterio = criterio(null, null);
        return pagamentoRepository.pesquisarHonorarios(filtro.getInicioEfetivo(), filtro.getFimEfetivo(),
                somenteComValor, null, criterio.padraoCliente, criterio.padraoCpf, criterio.padraoObservacao,
                filtro.getResponsavelId(), filtro.getTipoServicoId());
    }

    @Transactional(readOnly = true)
    public AgendaRepasses repasses(FiltroPagamentos filtro) {
        CriterioBusca criterio = criterio(filtro.getBusca(), filtro.getObservacao());
        return new AgendaRepasses(responsavelPagamentoRepository.pesquisarRepasses(filtro.getInicio(), filtro.getFim(),
                false, filtro.getSituacaoRepasse(), criterio.padraoCliente, criterio.padraoCpf,
                criterio.padraoObservacao, filtro.getResponsavelId(), filtro.getTipoServicoId()));
    }

    @Transactional(readOnly = true)
    public List<PagamentoAgendado> listarPorCliente(Long clienteId) {
        return pagamentoRepository.findByClienteIdOrderByDataRecebimentoDescIdDesc(clienteId);
    }

    @Transactional(readOnly = true)
    public PagamentoForm novoFormulario(LocalDate data, Long clienteId) {
        PagamentoForm form = new PagamentoForm();
        form.setDataRecebimento(data);
        if (clienteId != null) {
            Cliente cliente = clienteService.buscar(clienteId);
            form.setClienteId(cliente.getId());
            form.setNomeCliente(cliente.getNome());
            if (cliente.getCpf() != null) {
                form.setCpf(Cpf.formatar(cliente.getCpf()));
            }
        }
        return form;
    }

    @Transactional(readOnly = true)
    public AgendaPagamentos relatorioPagamentos(FiltroRelatorio filtro) {
        CriterioBusca criterio = criterio(null, null);
        StatusPagamento statusFiltro = filtro.situacaoComo(StatusPagamento.class);
        List<StatusPagamento> status = statusFiltro == null
                ? Arrays.asList(StatusPagamento.values()) : List.of(statusFiltro);
        return new AgendaPagamentos(pagamentoRepository.pesquisar(filtro.getInicioEfetivo(), filtro.getFimEfetivo(),
                status, criterio.padraoCliente, criterio.padraoCpf, criterio.padraoObservacao,
                filtro.getResponsavelId(), filtro.getTipoServicoId()));
    }

    @Transactional(readOnly = true)
    public AgendaPagamentos relatorioHonorarios(FiltroRelatorio filtro) {
        return new AgendaPagamentos(pesquisarHonorarios(filtro, true));
    }

    @Transactional(readOnly = true)
    public AgendaRepasses relatorioRepasses(FiltroRelatorio filtro) {
        CriterioBusca criterio = criterio(null, null);
        return new AgendaRepasses(responsavelPagamentoRepository.pesquisarRepasses(filtro.getInicioEfetivo(),
                filtro.getFimEfetivo(), true, filtro.situacaoComo(SituacaoRepassePagamento.class),
                criterio.padraoCliente, criterio.padraoCpf, criterio.padraoObservacao, filtro.getResponsavelId(),
                filtro.getTipoServicoId()));
    }

    /**
     * @param incluirHonorario se o usuário pode lançar honorários; sem essa permissão, o honorário do formulário é
     *                         ignorado e o pagamento nasce sem valor e "A receber"
     */
    @Transactional
    public PagamentoAgendado criar(PagamentoForm form, boolean incluirHonorario) {
        validarValorOpcional(form.getValor(), "O valor", "valor");
        boolean honorarioProprio = incluirHonorario && (form.getHonorarioOrigemId() == null || form.isHonorarioNovo());
        SituacaoHonorarioPagamento situacaoHonorario = honorarioProprio ? situacaoHonorarioDoFormulario(form) : null;
        BigDecimal valorHonorarioPago = honorarioProprio
                ? valorPagoValidado(form.getValorHonorario(), situacaoHonorario, form.getValorHonorarioPago(),
                form.getValorHonorarioFalta(), CamposParcial.FORMULARIO)
                : null;
        List<Responsavel> responsaveis = idsResponsaveis(form.getResponsavelIds()).stream()
                .map(this::responsavelAtivo)
                .toList();
        TipoServico tipo = tipoServicoAtivo(form.getTipoServicoId());
        Cliente cliente = cliente(form);
        PagamentoAgendado origem = escolherOrigem(cliente, tipo, form, null);

        PagamentoAgendado pagamento = new PagamentoAgendado(cliente, responsaveis, tipo, form.getDataRecebimento(),
                form.getValor(), Textos.opcional(form.getBanco()));
        pagamento.setStatus(form.getStatus());
        pagamento.setObservacoes(Textos.opcional(form.getObservacoes()));
        if (origem != null) {
            pagamento.setHonorarioOrigem(origem);
            pagamento.setSituacaoHonorario(origem.getSituacaoHonorario());
            if (incluirHonorario && !temNegociacaoAtiva(origem.getId())) {
                pagamento.setValorHonorarioPago(
                        pagoNaContinuacaoValidado(origem, null, form.getValorPagoContinuacao(), CAMPO_PAGO_CONTINUACAO));
            }
        } else if (honorarioProprio) {
            pagamento.setValorHonorario(form.getValorHonorario());
            pagamento.setValorHonorarioPago(valorHonorarioPago);
            pagamento.setSituacaoHonorario(situacaoHonorario);
        }
        pagamentoRepository.save(pagamento);
        if (origem != null) {
            atualizarSituacaoDaOrigem(origem);
            pagamento.setSituacaoHonorario(origem.getSituacaoHonorario());
        }

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, pagamento.getId(),
                "Pagamento de '" + cliente.getNome() + "' (" + tipo.getNome() + ") agendado para "
                        + Textos.data(pagamento.getDataRecebimento()) + ", valor " + Textos.moeda(pagamento.getValor())
                        + ", banco " + bancoOuNaoInformado(pagamento.getBanco()) + ", cliente recebeu? " + pagamento.getStatus().getRotulo()
                        + ", " + (responsaveis.size() == 1 ? "responsável " : "responsáveis ")
                        + pagamento.getNomesResponsaveis() + descricaoHonorarioNaCriacao(pagamento) + ".");
        return pagamento;
    }

    /**
     * @param incluirHonorario se o usuário pode lançar honorários; o honorário também fica de fora quando há
     *                         negociação ativa, que só é alterada pelo detalhe do honorário
     */
    @Transactional
    public PagamentoAgendado atualizar(Long id, PagamentoForm form, boolean incluirHonorario) {
        PagamentoAgendado pagamento = buscar(id);
        validarValorOpcional(form.getValor(), "O valor", "valor");
        boolean alterarHonorario = incluirHonorario && !temNegociacaoAtiva(id);
        List<String> alteracoes = new ArrayList<>();

        Cliente cliente = cliente(form);
        PagamentoAgendado origemAnterior = pagamento.getHonorarioOrigem();
        if (!cliente.getId().equals(pagamento.getCliente().getId())) {
            alteracoes.add("cliente de '" + pagamento.getCliente().getNome() + "' para '" + cliente.getNome() + "'");
            pagamento.setCliente(cliente);
        }
        alterarResponsaveis(pagamento, idsResponsaveis(form.getResponsavelIds()), alteracoes);
        if (!pagamento.getTipoServico().getId().equals(form.getTipoServicoId())) {
            TipoServico novo = tipoServicoAtivo(form.getTipoServicoId());
            alteracoes.add("benefício de '" + pagamento.getTipoServico().getNome() + "' para '" + novo.getNome() + "'");
            pagamento.setTipoServico(novo);
        }
        if (!pagamento.getDataRecebimento().equals(form.getDataRecebimento())) {
            alteracoes.add("dia de recebimento de " + Textos.data(pagamento.getDataRecebimento()) + " para "
                    + Textos.data(form.getDataRecebimento()));
            pagamento.setDataRecebimento(form.getDataRecebimento());
        }
        if (valorDiferente(pagamento.getValor(), form.getValor())) {
            alteracoes.add("valor de " + Textos.moeda(pagamento.getValor()) + " para " + Textos.moeda(form.getValor()));
            pagamento.setValor(form.getValor());
        }
        String banco = Textos.opcional(form.getBanco());
        if (!Objects.equals(pagamento.getBanco(), banco)) {
            alteracoes.add("banco de " + bancoOuNaoInformado(pagamento.getBanco()) + " para "
                    + bancoOuNaoInformado(banco));
            pagamento.setBanco(banco);
        }
        if (pagamento.getStatus() != form.getStatus()) {
            alteracoes.add(descricaoStatus(pagamento.getStatus(), form.getStatus()));
            pagamento.setStatus(form.getStatus());
        }
        String observacoes = Textos.opcional(form.getObservacoes());
        if (!Objects.equals(pagamento.getObservacoes(), observacoes)) {
            alteracoes.add("observações");
            pagamento.setObservacoes(observacoes);
        }
        PagamentoAgendado origem = origemAoAtualizar(pagamento, cliente, form, incluirHonorario);
        if (!mesmoPagamento(origemAnterior, origem)) {
            alteracoes.add(descricaoOrigem(origemAnterior, origem));
            pagamento.setHonorarioOrigem(origem);
            if (origemAnterior != null) {
                atualizarSituacaoDaOrigem(origemAnterior);
            }
        }
        if (origem != null) {
            if (pagamento.getValorHonorario() != null) {
                alteracoes.add("honorário de " + Textos.moeda(pagamento.getValorHonorario())
                        + " para sem valor (a dívida fica no honorário de origem)");
                pagamento.setValorHonorario(null);
            }
            if (temNegociacaoAtiva(origem.getId())) {
                aplicarPagoNaContinuacao(pagamento, null, alteracoes);
            } else if (incluirHonorario) {
                aplicarPagoNaContinuacao(pagamento, pagoNaContinuacaoValidado(origem, id,
                        form.getValorPagoContinuacao(), CAMPO_PAGO_CONTINUACAO), alteracoes);
            } else if (origemAnterior == null) {
                aplicarPagoNaContinuacao(pagamento, null, alteracoes);
            }
            atualizarSituacaoDaOrigem(origem);
            pagamento.setSituacaoHonorario(origem.getSituacaoHonorario());
        } else if (alterarHonorario) {
            SituacaoHonorarioPagamento situacaoHonorario = situacaoHonorarioDoFormulario(form);
            BigDecimal valorHonorarioPago = pagoNaOrigemValidado(pagamento, form.getValorHonorario(),
                    situacaoHonorario, form.getValorHonorarioPago(), form.getValorHonorarioFalta(),
                    CamposParcial.FORMULARIO);
            aplicarHonorario(pagamento, form.getValorHonorario(), situacaoHonorario, valorHonorarioPago, alteracoes);
            atualizarSituacaoDaOrigem(pagamento);
        } else if (origemAnterior != null) {
            pagamento.setValorHonorarioPago(null);
            pagamento.setSituacaoHonorario(SituacaoHonorarioPagamento.A_RECEBER);
        }

        if (!alteracoes.isEmpty()) {
            registrarAlteracao(pagamento, String.join(", ", alteracoes));
        }
        return pagamento;
    }

    @Transactional
    public PagamentoAgendado alterarStatus(Long id, StatusPagamento novo) {
        PagamentoAgendado pagamento = buscar(id);
        StatusPagamento anterior = pagamento.getStatus();
        if (anterior != novo) {
            pagamento.setStatus(novo);
            registrarAlteracao(pagamento, descricaoStatus(anterior, novo));
        }
        return pagamento;
    }

    /**
     * Valor em branco ({@code null}) significa que ainda não há honorário combinado para o pagamento.
     *
     * @param valorPago  quanto o cliente já pagou; só é usado na situação "Parcial"
     * @param valorFalta quanto ainda falta pagar, alternativa ao valor pago no "Parcial"
     */
    @Transactional
    public PagamentoAgendado lancarHonorario(Long id, BigDecimal valor, SituacaoHonorarioPagamento situacao,
                                             BigDecimal valorPago, BigDecimal valorFalta) {
        validarValorOpcional(valor, "O honorário", "valor");
        validarSituacaoSimples(situacao, "situacao");
        PagamentoAgendado pagamento = buscar(id);
        if (pagamento.isContinuacao()) {
            throw new RegraNegocioException("Este pagamento continua o honorário de "
                    + Textos.data(pagamento.getHonorarioOrigem().getDataRecebimento())
                    + ". Informe só quanto foi pago ao escritório neste crédito.");
        }
        if (temNegociacaoAtiva(id)) {
            throw new RegraNegocioException(
                    "Este honorário tem uma negociação. Altere parcelas e recebimentos no detalhe.");
        }
        BigDecimal pagoValidado = pagoNaOrigemValidado(pagamento, valor, situacao, valorPago, valorFalta,
                CamposParcial.LINHA);
        List<String> alteracoes = new ArrayList<>();
        aplicarHonorario(pagamento, valor, situacao, pagoValidado, alteracoes);
        atualizarSituacaoDaOrigem(pagamento);
        if (!alteracoes.isEmpty()) {
            registrarAlteracao(pagamento, String.join(", ", alteracoes));
        }
        return pagamento;
    }

    /**
     * Quanto a cliente pagou ao escritório com o crédito de um pagamento que continua outro honorário.
     * A situação não é escolhida aqui: acompanha a do honorário de origem.
     */
    @Transactional
    public PagamentoAgendado lancarPagoNaContinuacao(Long id, BigDecimal valorPago) {
        PagamentoAgendado pagamento = buscar(id);
        if (!pagamento.isContinuacao()) {
            throw new RegraNegocioException("Este pagamento não continua outro honorário.");
        }
        PagamentoAgendado origem = pagamento.getHonorarioOrigem();
        if (temNegociacaoAtiva(origem.getId())) {
            throw new RegraNegocioException(
                    "O honorário de origem tem uma negociação. Registre o recebimento nas parcelas do detalhe.");
        }
        BigDecimal pago = pagoNaContinuacaoValidado(origem, id, valorPago, "valorPago");
        List<String> alteracoes = new ArrayList<>();
        aplicarPagoNaContinuacao(pagamento, pago, alteracoes);
        atualizarSituacaoDaOrigem(origem);
        pagamento.setSituacaoHonorario(origem.getSituacaoHonorario());
        if (!alteracoes.isEmpty()) {
            registrarAlteracao(pagamento, String.join(", ", alteracoes));
        }
        return pagamento;
    }

    /**
     * Apaga o pagamento lançado por engano ou em duplicidade. Os dados e o motivo ficam na auditoria.
     * Pagamentos que já envolveram dinheiro do escritório não podem ser excluídos (ver {@link #impedimentoParaExcluir}).
     */
    @Transactional
    public PagamentoAgendado excluir(Long id, String motivo) {
        PagamentoAgendado pagamento = buscar(id);
        String motivoValidado = MotivoCancelamento.validar(motivo, "da exclusão");
        Optional<String> impedimento = impedimento(pagamento);
        if (impedimento.isPresent()) {
            throw new RegraNegocioException(impedimento.get());
        }
        String descricao = "Pagamento de '" + pagamento.getCliente().getNome() + "' ("
                + pagamento.getTipoServico().getNome() + ") de " + Textos.data(pagamento.getDataRecebimento())
                + " excluído. Tinha valor " + moedaOuSem(pagamento.getValor()) + ", banco "
                + bancoOuNaoInformado(pagamento.getBanco()) + ", cliente recebeu? " + pagamento.getStatus().getRotulo()
                + ", " + (pagamento.getResponsaveis().size() == 1 ? "responsável " : "responsáveis ")
                + pagamento.getNomesResponsaveis() + descricaoHonorarioNaCriacao(pagamento) + ". Motivo: "
                + motivoValidado;
        PagamentoAgendado origem = pagamento.getHonorarioOrigem();
        pagamentoRepository.delete(pagamento);
        if (origem != null) {
            atualizarSituacaoDaOrigem(origem);
        }
        auditoriaService.registrar(AcaoAuditoria.EXCLUSAO, ENTIDADE, id, descricao);
        return pagamento;
    }

    /** Por que o pagamento não pode ser excluído, ou vazio se pode. */
    @Transactional(readOnly = true)
    public Optional<String> impedimentoParaExcluir(Long id) {
        return impedimento(buscar(id));
    }

    private Optional<String> impedimento(PagamentoAgendado pagamento) {
        if (pagamentoRepository.existsByHonorarioOrigemId(pagamento.getId())) {
            return Optional.of(
                    "Há pagamentos posteriores que continuam o honorário deste. Exclua ou desvincule-os antes.");
        }
        if (negociacaoHonorarioRepository.existsByPagamentoId(pagamento.getId())) {
            return Optional.of("Este pagamento tem negociação de honorário e não pode ser excluído.");
        }
        if (pagamento.isContinuacao()) {
            if (pagamento.getValorHonorarioPago() != null) {
                return Optional.of("Há " + Textos.moeda(pagamento.getValorHonorarioPago())
                        + " pagos ao escritório neste crédito. Para excluir, retire esse valor.");
            }
        } else if (pagamento.getSituacaoHonorario() != SituacaoHonorarioPagamento.A_RECEBER) {
            return Optional.of("O honorário deste pagamento está \"" + pagamento.getSituacaoHonorario().getRotulo()
                    + "\". Para excluir, volte o honorário para \"A receber\".");
        }
        for (ResponsavelPagamento repasse : pagamento.getResponsaveis()) {
            if (repasse.getValorRepasse() != null || repasse.isComPagamento()) {
                return Optional.of("Há repasse lançado para '" + repasse.getResponsavel().getNome()
                        + "'. Para excluir, retire o repasse em Repasses.");
            }
        }
        return Optional.empty();
    }

    private boolean temNegociacaoAtiva(Long pagamentoId) {
        return negociacaoHonorarioRepository.findByPagamentoIdAndSituacao(pagamentoId, SituacaoNegociacao.ATIVA)
                .isPresent();
    }

    private static void aplicarHonorario(PagamentoAgendado pagamento, BigDecimal valor,
                                         SituacaoHonorarioPagamento situacao, BigDecimal valorPago,
                                         List<String> alteracoes) {
        if (valorDiferente(pagamento.getValorHonorario(), valor)) {
            alteracoes.add("honorário de " + moedaOuSem(pagamento.getValorHonorario()) + " para " + moedaOuSem(valor));
            pagamento.setValorHonorario(valor);
        }
        if (valorDiferente(pagamento.getValorHonorarioPago(), valorPago)) {
            alteracoes.add("valor já pago do honorário de " + moedaOuSem(pagamento.getValorHonorarioPago())
                    + " para " + moedaOuSem(valorPago));
            pagamento.setValorHonorarioPago(valorPago);
        }
        if (pagamento.getSituacaoHonorario() != situacao) {
            alteracoes.add(descricaoSituacaoHonorario(pagamento.getSituacaoHonorario(), situacao));
            pagamento.setSituacaoHonorario(situacao);
        }
    }

    /** No cadastro, "Recebido" exige o valor: é o que o cliente já pagou ao escritório. */
    private static SituacaoHonorarioPagamento situacaoHonorarioDoFormulario(PagamentoForm form) {
        validarValorOpcional(form.getValorHonorario(), "O honorário", CAMPO_VALOR_HONORARIO);
        SituacaoHonorarioPagamento situacao = form.getSituacaoHonorario() == null
                ? SituacaoHonorarioPagamento.A_RECEBER : form.getSituacaoHonorario();
        validarSituacaoSimples(situacao, CAMPO_SITUACAO_HONORARIO);
        if (situacao == SituacaoHonorarioPagamento.RECEBIDO && form.getValorHonorario() == null) {
            throw new RegraNegocioException(CAMPO_VALOR_HONORARIO, "Informe o valor do honorário recebido.");
        }
        return situacao;
    }

    /**
     * "Parcial" exige o honorário e quanto o cliente já pagou ou quanto ainda falta (o pago é o honorário menos
     * o que falta); o pago tem de ficar entre zero e o honorário. Nas outras situações o valor pago não é guardado.
     */
    private static BigDecimal valorPagoValidado(BigDecimal valorHonorario, SituacaoHonorarioPagamento situacao,
                                                BigDecimal valorPago, BigDecimal valorFalta, CamposParcial campos) {
        if (situacao != SituacaoHonorarioPagamento.PARCIAL) {
            return null;
        }
        return valorParcialValidado(valorHonorario, valorPago, valorFalta, campos);
    }

    /** Mesma regra do honorário "Parcial", aplicada ao repasse "Parcial" do responsável. */
    private static BigDecimal valorRepassePagoValidado(BigDecimal valorRepasse, SituacaoRepassePagamento situacao,
                                                       BigDecimal valorPago, BigDecimal valorFalta) {
        if (situacao != SituacaoRepassePagamento.PARCIAL) {
            return null;
        }
        return valorParcialValidado(valorRepasse, valorPago, valorFalta, CamposParcial.REPASSE);
    }

    /** Quanto já foi pago de um valor "Parcial", a partir do pago ou do que falta, entre zero e o total. */
    private static BigDecimal valorParcialValidado(BigDecimal total, BigDecimal valorPago, BigDecimal valorFalta,
                                                   CamposParcial campos) {
        TextosParcial textos = campos.textos();
        String nome = textos.nome();
        if (total == null) {
            throw new RegraNegocioException(campos.total(),
                    "No Parcial, informe o valor total do " + nome + " (não só o que foi pago) para calcular quanto falta.");
        }
        if (valorPago == null && valorFalta == null) {
            throw new RegraNegocioException(campos.pago(), textos.semValores());
        }
        if (valorPago == null) {
            validarValorOpcional(valorFalta, "O valor que falta", campos.falta());
            if (valorFalta.compareTo(total) >= 0) {
                throw new RegraNegocioException(campos.falta(), "O valor que falta deve ser menor que o " + nome + " ("
                        + Textos.moeda(total) + "). " + textos.nadaPago());
            }
            return total.subtract(valorFalta);
        }
        validarValorOpcional(valorPago, "O valor já pago", campos.pago());
        if (valorPago.compareTo(total) >= 0) {
            throw new RegraNegocioException(campos.pago(), "O valor já pago deve ser menor que o " + nome + " ("
                    + Textos.moeda(total) + "). " + textos.tudoPago());
        }
        if (valorFalta != null && valorPago.add(valorFalta).compareTo(total) != 0) {
            throw new RegraNegocioException(campos.falta(), "O valor já pago mais o que falta deve dar o " + nome + " ("
                    + Textos.moeda(total) + "). Preencha só um dos dois.");
        }
        return valorPago;
    }

    /** Nomes dos campos do valor "Parcial" em cada tela, para a mensagem de erro aparecer no campo certo. */
    private record CamposParcial(String total, String pago, String falta, TextosParcial textos) {
        static final CamposParcial FORMULARIO = new CamposParcial(CAMPO_VALOR_HONORARIO, CAMPO_VALOR_HONORARIO_PAGO,
                "valorHonorarioFalta", TextosParcial.HONORARIO);
        static final CamposParcial LINHA = new CamposParcial("valor", "valorPago", "valorFalta", TextosParcial.HONORARIO);
        static final CamposParcial REPASSE = new CamposParcial("valor", "valorPago", "valorFalta", TextosParcial.REPASSE);
    }

    /** O que muda nas mensagens do "Parcial" entre o honorário do cliente e o repasse ao responsável. */
    private record TextosParcial(String nome, String semValores, String nadaPago, String tudoPago) {
        static final TextosParcial HONORARIO = new TextosParcial("honorário",
                "Informe quanto o cliente já pagou ou quanto ainda falta pagar do honorário.",
                "Se o cliente ainda não pagou nada, escolha \"A receber\".",
                "Se o cliente pagou tudo, escolha \"Recebido\".");
        static final TextosParcial REPASSE = new TextosParcial("repasse",
                "Informe quanto já foi pago ao responsável ou quanto ainda falta pagar do repasse.",
                "Se nada foi pago ainda, escolha \"Pendente\".",
                "Se o repasse foi pago por inteiro, escolha \"Pago\".");
    }

    private static void validarSituacaoSimples(SituacaoHonorarioPagamento situacao, String campo) {
        if (!situacao.isLancamentoSimples()) {
            throw new RegraNegocioException(campo,
                    "A situação " + situacao.getRotulo() + " é calculada a partir da negociação.");
        }
    }

    private static String descricaoHonorarioNaCriacao(PagamentoAgendado pagamento) {
        if (pagamento.isContinuacao()) {
            String pago = pagamento.getValorHonorarioPago() == null
                    ? ""
                    : ", pago ao escritório neste crédito " + Textos.moeda(pagamento.getValorHonorarioPago());
            return ", continua honorário de " + Textos.data(pagamento.getHonorarioOrigem().getDataRecebimento())
                    + " (" + pagamento.getSituacaoHonorario().getRotulo() + ")" + pago;
        }
        if (pagamento.getValorHonorario() == null) {
            return "";
        }
        String pago = pagamento.getValorHonorarioPago() == null
                ? ""
                : ", já pago " + Textos.moeda(pagamento.getValorHonorarioPago());
        return ", honorário " + Textos.moeda(pagamento.getValorHonorario())
                + " (" + pagamento.getSituacaoHonorario().getRotulo() + pago + ")";
    }

    /**
     * Sem permissão de honorários o formulário não traz a escolha de continuar ou não; aí um pagamento
     * que já tem o próprio honorário nunca passa a continuar outro.
     */
    private PagamentoAgendado origemAoAtualizar(PagamentoAgendado pagamento, Cliente cliente, PagamentoForm form,
                                                boolean incluirHonorario) {
        if ((incluirHonorario && form.isHonorarioNovo())
                || pagamentoRepository.existsByHonorarioOrigemId(pagamento.getId())
                || temNegociacaoAtiva(pagamento.getId())) {
            return null;
        }
        PagamentoAgendado atual = pagamento.getHonorarioOrigem();
        if (atual != null && mesmoClienteETipo(atual, cliente, pagamento.getTipoServico())
                && (form.getHonorarioOrigemId() == null || form.getHonorarioOrigemId().equals(atual.getId()))) {
            return atual;
        }
        if (!incluirHonorario && atual == null && pagamento.getValorHonorario() != null) {
            return null;
        }
        return escolherOrigem(cliente, pagamento.getTipoServico(), form, pagamento.getId());
    }

    private PagamentoAgendado escolherOrigem(Cliente cliente, TipoServico tipo, PagamentoForm form, Long pagamentoId) {
        if (form.isHonorarioNovo() || cliente.getId() == null) {
            return null;
        }
        List<PagamentoAgendado> candidatas = pagamentoRepository
                .findByClienteIdAndTipoServicoIdAndHonorarioOrigemIsNullAndValorHonorarioNotNullOrderByDataRecebimentoAscIdAsc(
                        cliente.getId(), tipo.getId())
                .stream()
                .filter(p -> pagamentoId == null || !p.getId().equals(pagamentoId))
                .filter(p -> p.getSituacaoHonorario() != SituacaoHonorarioPagamento.RECEBIDO)
                .toList();
        if (form.getHonorarioOrigemId() != null) {
            return candidatas.stream()
                    .filter(p -> p.getId().equals(form.getHonorarioOrigemId()))
                    .findFirst()
                    .orElseThrow(() -> new RegraNegocioException("honorarioOrigemId",
                            "Esse honorário não está em aberto para este cliente e benefício."));
        }
        if (candidatas.size() == 1) {
            return candidatas.getFirst();
        }
        if (candidatas.size() > 1) {
            throw new RegraNegocioException("honorarioOrigemId",
                    "Esta cliente tem mais de um honorário em aberto neste benefício. Escolha qual continuar.");
        }
        return null;
    }

    /** O valor pago num crédito que continua o honorário não pode passar do saldo em aberto. */
    private BigDecimal pagoNaContinuacaoValidado(PagamentoAgendado origem, Long pagamentoId, BigDecimal valor,
                                                 String campo) {
        if (valor == null) {
            return null;
        }
        validarValorOpcional(valor, "O valor pago", campo);
        List<PagamentoAgendado> outros = pagamentoRepository
                .findByHonorarioOrigemIdOrderByDataRecebimentoAscIdAsc(origem.getId()).stream()
                .filter(p -> pagamentoId == null || !p.getId().equals(pagamentoId))
                .toList();
        BigDecimal jaPago = CalculoHonorarios.pagoNaOrigem(origem, true)
                .add(CalculoHonorarios.pagoNasContinuacoes(outros));
        BigDecimal saldo = CalculoHonorarios.saldo(origem.getValorHonorario(), jaPago);
        if (saldo != null && valor.compareTo(saldo) > 0) {
            throw new RegraNegocioException(campo, "O valor pago passa do saldo em aberto do honorário de "
                    + Textos.data(origem.getDataRecebimento()) + " (" + Textos.moeda(saldo) + ").");
        }
        return valor;
    }

    /**
     * Valor pago guardado na própria origem. Quando há continuações, "Recebido" quita o que falta além do
     * que já foi pago nelas, e "Parcial" pode vir sem valor: o pago nas continuações já basta.
     */
    private BigDecimal pagoNaOrigemValidado(PagamentoAgendado pagamento, BigDecimal valor,
                                            SituacaoHonorarioPagamento situacao, BigDecimal valorPago,
                                            BigDecimal valorFalta, CamposParcial campos) {
        List<PagamentoAgendado> filhos = pagamentoRepository
                .findByHonorarioOrigemIdOrderByDataRecebimentoAscIdAsc(pagamento.getId());
        if (filhos.isEmpty()) {
            return valorPagoValidado(valor, situacao, valorPago, valorFalta, campos);
        }
        if (valor == null) {
            throw new RegraNegocioException(campos.total(),
                    "Há pagamentos que continuam este honorário. Informe o valor total ou desvincule-os antes.");
        }
        if (situacao == SituacaoHonorarioPagamento.RECEBIDO) {
            BigDecimal resto = valor.subtract(CalculoHonorarios.pagoNasContinuacoes(filhos));
            return resto.signum() > 0 ? resto : null;
        }
        if (situacao == SituacaoHonorarioPagamento.PARCIAL && valorPago == null && valorFalta == null) {
            return null;
        }
        return valorPagoValidado(valor, situacao, valorPago, valorFalta, campos);
    }

    private static void aplicarPagoNaContinuacao(PagamentoAgendado pagamento, BigDecimal valorPago,
                                                 List<String> alteracoes) {
        if (valorDiferente(pagamento.getValorHonorarioPago(), valorPago)) {
            alteracoes.add("pago ao escritório neste crédito de " + moedaOuSem(pagamento.getValorHonorarioPago())
                    + " para " + moedaOuSem(valorPago));
            pagamento.setValorHonorarioPago(valorPago);
        }
    }

    /**
     * Recalcula a situação da origem pelo total pago e repete essa situação nas continuações: a dívida
     * é uma só, então todas mostram "Parcial" enquanto houver saldo e "Recebido" quando quitar.
     */
    private void atualizarSituacaoDaOrigem(PagamentoAgendado origem) {
        if (origem == null || origem.getId() == null) {
            return;
        }
        List<PagamentoAgendado> filhos = pagamentoRepository
                .findByHonorarioOrigemIdOrderByDataRecebimentoAscIdAsc(origem.getId());
        if (!temNegociacaoAtiva(origem.getId()) && (!filhos.isEmpty() || origem.getValorHonorario() != null)) {
            BigDecimal recebido = CalculoHonorarios.recebidoDaOrigem(origem, filhos, BigDecimal.ZERO, false);
            SituacaoHonorarioPagamento calculada = CalculoHonorarios.situacao(origem.getValorHonorario(),
                    origem.getSituacaoHonorario(), false, null, recebido, false, !filhos.isEmpty());
            if (origem.getSituacaoHonorario() != calculada) {
                origem.setSituacaoHonorario(calculada);
            }
        }
        filhos.forEach(filho -> filho.setSituacaoHonorario(origem.getSituacaoHonorario()));
    }

    private static boolean mesmoPagamento(PagamentoAgendado atual, PagamentoAgendado novo) {
        if (atual == null || novo == null) {
            return atual == novo;
        }
        return atual.getId().equals(novo.getId());
    }

    private static boolean mesmoClienteETipo(PagamentoAgendado origem, Cliente cliente, TipoServico tipo) {
        return origem.getCliente().getId().equals(cliente.getId())
                && origem.getTipoServico().getId().equals(tipo.getId());
    }

    private static String descricaoOrigem(PagamentoAgendado de, PagamentoAgendado para) {
        if (de == null) {
            return "passou a continuar o honorário de " + Textos.data(para.getDataRecebimento());
        }
        if (para == null) {
            return "deixou de continuar o honorário de " + Textos.data(de.getDataRecebimento());
        }
        return "honorário de origem de " + Textos.data(de.getDataRecebimento()) + " para "
                + Textos.data(para.getDataRecebimento());
    }

    /**
     * Repasse de um dos responsáveis do pagamento ({@code id} da linha de {@link ResponsavelPagamento}).
     * Valor em branco ({@code null}) significa que não há repasse a esse responsável.
     *
     * @param valorPago  quanto o responsável já recebeu; só é usado na situação "Parcial"
     * @param valorFalta quanto ainda falta pagar, alternativa ao valor pago no "Parcial"
     */
    @Transactional
    public ResponsavelPagamento lancarRepasse(Long id, BigDecimal valor, SituacaoRepassePagamento situacao,
                                              BigDecimal valorPago, BigDecimal valorFalta) {
        validarValorOpcional(valor, "O repasse", "valor");
        BigDecimal pagoValidado = valorRepassePagoValidado(valor, situacao, valorPago, valorFalta);
        ResponsavelPagamento repasse = responsavelPagamentoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Repasse não encontrado."));
        String nome = "'" + repasse.getResponsavel().getNome() + "'";
        List<String> alteracoes = new ArrayList<>();
        if (valorDiferente(repasse.getValorRepasse(), valor)) {
            alteracoes.add("repasse de " + nome + " de " + moedaOuSem(repasse.getValorRepasse())
                    + " para " + moedaOuSem(valor));
            repasse.setValorRepasse(valor);
        }
        if (valorDiferente(repasse.getValorRepassePago(), pagoValidado)) {
            alteracoes.add("valor já pago do repasse de " + nome + " de " + moedaOuSem(repasse.getValorRepassePago())
                    + " para " + moedaOuSem(pagoValidado));
            repasse.setValorRepassePago(pagoValidado);
        }
        if (repasse.getSituacaoRepasse() != situacao) {
            alteracoes.add("situação do repasse de " + nome + " de " + repasse.getSituacaoRepasse().getRotulo()
                    + " para " + situacao.getRotulo());
            repasse.setSituacaoRepasse(situacao);
        }
        if (!alteracoes.isEmpty()) {
            registrarAlteracao(repasse.getPagamento(), String.join(", ", alteracoes));
        }
        return repasse;
    }

    /** Ids escolhidos no formulário, sem repetição e na ordem em que vieram. */
    private static Set<Long> idsResponsaveis(List<Long> ids) {
        Set<Long> unicos = new LinkedHashSet<>();
        if (ids != null) {
            ids.stream().filter(Objects::nonNull).forEach(unicos::add);
        }
        if (unicos.isEmpty()) {
            throw new RegraNegocioException(CAMPO_RESPONSAVEIS, "Selecione ao menos um responsável.");
        }
        return unicos;
    }

    /**
     * Inclui e remove responsáveis conforme o formulário. Quem já recebeu o repasse não pode sair do pagamento,
     * para não perder o registro do que foi pago.
     */
    private void alterarResponsaveis(PagamentoAgendado pagamento, Set<Long> ids, List<String> alteracoes) {
        List<String> removidos = new ArrayList<>();
        for (ResponsavelPagamento vinculo : pagamento.getResponsaveis()) {
            if (ids.contains(vinculo.getResponsavel().getId())) {
                continue;
            }
            if (vinculo.isComPagamento()) {
                String pago = vinculo.getSituacaoRepasse() == SituacaoRepassePagamento.PAGO
                        ? "já foi pago" : "já foi pago em parte";
                throw new RegraNegocioException(CAMPO_RESPONSAVEIS, "O repasse de '"
                        + vinculo.getResponsavel().getNome() + "' " + pago + "; ele não pode sair deste pagamento.");
            }
            pagamento.removerResponsavel(vinculo);
            removidos.add("'" + vinculo.getResponsavel().getNome() + "'");
        }
        List<String> incluidos = new ArrayList<>();
        for (Long id : ids) {
            if (pagamento.responsavel(id).isEmpty()) {
                Responsavel novo = responsavelAtivo(id);
                pagamento.adicionarResponsavel(novo);
                incluidos.add("'" + novo.getNome() + "'");
            }
        }
        List<String> partes = new ArrayList<>();
        if (!incluidos.isEmpty()) {
            partes.add((incluidos.size() == 1 ? "incluído " : "incluídos ") + String.join(", ", incluidos));
        }
        if (!removidos.isEmpty()) {
            partes.add((removidos.size() == 1 ? "removido " : "removidos ") + String.join(", ", removidos));
        }
        if (!partes.isEmpty()) {
            alteracoes.add("responsáveis: " + String.join(", ", partes));
        }
    }

    private static void validarValorOpcional(BigDecimal valor, String nome, String campo) {
        if (valor == null) {
            return;
        }
        if (valor.signum() <= 0) {
            throw new RegraNegocioException(campo, nome + " deve ser maior que zero.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new RegraNegocioException(campo, nome + " deve ter no máximo duas casas decimais.");
        }
        if (valor.precision() - valor.scale() > 13) {
            throw new RegraNegocioException(campo, nome + " é grande demais.");
        }
    }

    private static boolean valorDiferente(BigDecimal atual, BigDecimal novo) {
        if (atual == null || novo == null) {
            return atual != novo;
        }
        return atual.compareTo(novo) != 0;
    }

    private Cliente cliente(PagamentoForm form) {
        if (form.getClienteId() != null) {
            return clienteService.obterEscolhido(form.getClienteId(), form.getCpf(), null, ORIGEM, CAMPO_CPF);
        }
        return clienteService.obterOuCadastrarPorCpf(form.getNomeCliente(), form.getCpf(), null, hoje(),
                ORIGEM, CAMPO_NOME, CAMPO_CPF);
    }

    private static String moedaOuSem(BigDecimal valor) {
        return valor == null ? "sem valor" : Textos.moeda(valor);
    }

    private static String descricaoSituacaoHonorario(SituacaoHonorarioPagamento de, SituacaoHonorarioPagamento para) {
        return "situação do honorário de " + de.getRotulo() + " para " + para.getRotulo();
    }

    private static String bancoOuNaoInformado(String banco) {
        return banco == null ? "não informado" : banco;
    }

    private Responsavel responsavelAtivo(Long id) {
        Responsavel responsavel = responsavelRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException(CAMPO_RESPONSAVEIS, "Responsável não encontrado."));
        if (!responsavel.isAtivo()) {
            throw new RegraNegocioException(CAMPO_RESPONSAVEIS,
                    "O responsável '" + responsavel.getNome() + "' está inativo e não pode ser usado em novos registros.");
        }
        return responsavel;
    }

    private TipoServico tipoServicoAtivo(Long id) {
        TipoServico tipo = tipoServicoRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("tipoServicoId", "Benefício não encontrado."));
        if (!tipo.isAtivo()) {
            throw new RegraNegocioException("tipoServicoId",
                    "O benefício '" + tipo.getNome() + "' está inativo e não pode ser usado em novos registros.");
        }
        return tipo;
    }

    private void registrarAlteracao(PagamentoAgendado pagamento, String alteracoes) {
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, pagamento.getId(),
                "Pagamento de '" + pagamento.getCliente().getNome() + "' alterado: " + alteracoes + ".");
    }

    private static String descricaoStatus(StatusPagamento de, StatusPagamento para) {
        return "recebimento de " + de.getRotulo() + " para " + para.getRotulo();
    }

    private static CriterioBusca criterio(String busca, String observacao) {
        String termo = busca == null ? "" : busca.trim();
        String digitos = termo.replaceAll("[\\s.\\-]", "");
        String padraoCpf = digitos.matches("\\d{" + DIGITOS_MINIMOS_BUSCA_CPF + ",11}") ? "%" + digitos + "%" : "";
        return new CriterioBusca(Textos.padraoBusca(termo), padraoCpf, Textos.padraoBuscaOpcional(observacao));
    }

    private record CriterioBusca(String padraoCliente, String padraoCpf, String padraoObservacao) {
    }
}
