package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.CalculoHonorarios;
import br.com.escritorio.financeiro.domain.NegociacaoHonorario;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoNegociacao;
import br.com.escritorio.financeiro.domain.SituacaoParcela;
import br.com.escritorio.financeiro.dto.AbatimentoHonorario;
import br.com.escritorio.financeiro.dto.AgendaHonorarios;
import br.com.escritorio.financeiro.dto.DetalheHonorario;
import br.com.escritorio.financeiro.dto.FiltroPagamentos;
import br.com.escritorio.financeiro.dto.FiltroRelatorio;
import br.com.escritorio.financeiro.dto.HonorarioAberto;
import br.com.escritorio.financeiro.dto.LinhaHonorario;
import br.com.escritorio.financeiro.dto.NegociacaoForm;
import br.com.escritorio.financeiro.dto.ParcelaComRecebimentos;
import br.com.escritorio.financeiro.dto.ParcelaNegociacaoItem;
import br.com.escritorio.financeiro.dto.RecebimentoForm;
import br.com.escritorio.financeiro.dto.ValoresHonorario;
import br.com.escritorio.financeiro.repository.NegociacaoHonorarioRepository;
import br.com.escritorio.financeiro.repository.PagamentoAgendadoRepository;
import br.com.escritorio.financeiro.repository.ParcelaHonorarioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Negociação, parcelas e recebimentos do honorário de um pagamento agendado. Sem acordo, o
 * lançamento simples (valor + A receber/Recebido) continua no {@link PagamentoService}.
 */
@Service
public class HonorarioService {

    static final String MOTIVO_SUBSTITUICAO = "Substituída por nova negociação.";
    private final PagamentoService pagamentoService;
    private final PagamentoAgendadoRepository pagamentoRepository;
    private final NegociacaoHonorarioRepository negociacaoRepository;
    private final ParcelaHonorarioRepository parcelaRepository;
    private final RecebimentoHonorarioRepository recebimentoRepository;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public HonorarioService(PagamentoService pagamentoService, PagamentoAgendadoRepository pagamentoRepository,
                            NegociacaoHonorarioRepository negociacaoRepository,
                            ParcelaHonorarioRepository parcelaRepository,
                            RecebimentoHonorarioRepository recebimentoRepository, AuditoriaService auditoriaService,
                            Clock relogio) {
        this.pagamentoService = pagamentoService;
        this.pagamentoRepository = pagamentoRepository;
        this.negociacaoRepository = negociacaoRepository;
        this.parcelaRepository = parcelaRepository;
        this.recebimentoRepository = recebimentoRepository;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional
    public AgendaHonorarios listar(FiltroPagamentos filtro) {
        return montar(pagamentoService.pesquisarHonorarios(filtro, false), filtro.getSituacaoHonorario());
    }

    @Transactional
    public AgendaHonorarios relatorio(FiltroRelatorio filtro) {
        return montar(pagamentoService.pesquisarHonorarios(filtro, true),
                filtro.situacaoComo(SituacaoHonorarioPagamento.class));
    }

    @Transactional
    public AgendaHonorarios doCliente(Long clienteId) {
        return montar(pagamentoService.listarPorCliente(clienteId), null);
    }

    @Transactional
    public Map<Long, ValoresHonorario> valoresPorPagamento(List<PagamentoAgendado> pagamentos) {
        return montarLinhas(pagamentos, true).stream()
                .collect(Collectors.toMap(l -> l.pagamento().getId(), LinhaHonorario::valores));
    }

    @Transactional
    public DetalheHonorario detalhe(Long pagamentoId) {
        PagamentoAgendado pagamento = pagamentoService.buscar(pagamentoId);
        NegociacaoHonorario ativa = negociacaoAtiva(pagamentoId);
        BigDecimal recebido = recebido(pagamento);
        List<ParcelaHonorario> parcelas = ativa == null
                ? List.of()
                : parcelaRepository.findByNegociacaoIdOrderByNumeroAsc(ativa.getId());
        List<PagamentoAgendado> continuacoes = pagamento.isContinuacao()
                ? List.of()
                : pagamentoRepository.findByHonorarioOrigemIdOrderByDataRecebimentoAscIdAsc(pagamentoId);
        atualizarSituacao(pagamento, ativa, parcelas, recebido, continuacoes);

        Map<Long, BigDecimal> recebidosParcela = totaisPorParcela(parcelas);
        Map<Long, List<RecebimentoHonorario>> recebimentosPorParcela = recebimentosPorParcela(parcelas);
        List<ParcelaComRecebimentos> comRecebimentos = parcelas.stream()
                .map(p -> {
                    BigDecimal pago = recebidosParcela.getOrDefault(p.getId(), BigDecimal.ZERO);
                    return new ParcelaComRecebimentos(p, pago, CalculoHonorarios.saldo(p.getValor(), pago),
                            recebimentosPorParcela.getOrDefault(p.getId(), List.of()));
                })
                .toList();

        List<NegociacaoHonorario> anteriores = negociacaoRepository
                .findByPagamentoIdOrderByDataNegociacaoDescIdDesc(pagamentoId).stream()
                .filter(n -> n.getSituacao() == SituacaoNegociacao.SUBSTITUIDA)
                .toList();
        int quantidadeAtivas = (int) parcelas.stream().filter(ParcelaHonorario::isAtiva).count();
        return new DetalheHonorario(pagamento, ValoresHonorario.de(pagamento, ativa, recebido, quantidadeAtivas),
                ativa, comRecebimentos, anteriores, abatimentos(pagamento, continuacoes),
                auditoriaService.historico(PagamentoService.ENTIDADE, pagamentoId));
    }

    /**
     * Honorários em aberto que um pagamento do cliente e benefício pode continuar. Na edição, o
     * honorário que o pagamento já continua aparece mesmo se já tiver sido quitado.
     */
    @Transactional(readOnly = true)
    public HonorarioAberto.Lista emAberto(Long clienteId, Long tipoServicoId, Long excetoPagamentoId) {
        if (clienteId == null || tipoServicoId == null) {
            return new HonorarioAberto.Lista(List.of());
        }
        Long origemAtual = excetoPagamentoId == null
                ? null
                : pagamentoRepository.findById(excetoPagamentoId)
                .map(PagamentoAgendado::getHonorarioOrigem)
                .map(PagamentoAgendado::getId)
                .orElse(null);
        List<HonorarioAberto> origens = pagamentoRepository
                .findByClienteIdAndTipoServicoIdAndHonorarioOrigemIsNullAndValorHonorarioNotNullOrderByDataRecebimentoAscIdAsc(
                        clienteId, tipoServicoId)
                .stream()
                .filter(p -> excetoPagamentoId == null || !p.getId().equals(excetoPagamentoId))
                .filter(p -> p.getSituacaoHonorario() != SituacaoHonorarioPagamento.RECEBIDO
                        || p.getId().equals(origemAtual))
                .map(this::paraAberto)
                .toList();
        return new HonorarioAberto.Lista(origens);
    }

    @Transactional(readOnly = true)
    public NegociacaoForm formularioNegociacao(Long pagamentoId) {
        PagamentoAgendado pagamento = pagamentoService.buscar(pagamentoId);
        exigirHonorarioLancado(pagamento);
        NegociacaoHonorario ativa = negociacaoAtiva(pagamentoId);
        NegociacaoForm form = new NegociacaoForm();
        BigDecimal jaRecebido = recebido(pagamento);
        BigDecimal sugerido = ativa != null ? ativa.getValorNegociado() : pagamento.getValorHonorario();
        if (sugerido.compareTo(jaRecebido) < 0) {
            sugerido = jaRecebido;
        }
        form.setValorNegociado(sugerido);
        form.setQuantidade(1);
        form.setPrimeiroVencimento(hoje());
        preencherParcelasSugeridas(form, CalculoHonorarios.saldo(sugerido, jaRecebido));
        return form;
    }

    @Transactional
    public NegociacaoHonorario negociar(Long pagamentoId, NegociacaoForm form) {
        PagamentoAgendado pagamento = pagamentoService.buscar(pagamentoId);
        exigirHonorarioLancado(pagamento);
        BigDecimal original = pagamento.getValorHonorario();
        BigDecimal negociado = form.getValorNegociado();
        if (negociado.compareTo(original) > 0) {
            throw new RegraNegocioException("valorNegociado",
                    "O valor negociado não pode ser maior que o honorário original (" + Textos.moeda(original) + ").");
        }
        BigDecimal jaRecebido = recebido(pagamento);
        if (negociado.compareTo(jaRecebido) < 0) {
            throw new RegraNegocioException("valorNegociado",
                    "O valor negociado não pode ser menor que o total já recebido (" + Textos.moeda(jaRecebido) + ").");
        }

        BigDecimal saldo = CalculoHonorarios.saldo(negociado, jaRecebido);
        List<ParcelaNegociacaoItem> itens = itensValidados(form, saldo);
        NegociacaoHonorario anterior = negociacaoAtiva(pagamentoId);
        if (anterior != null) {
            substituir(anterior);
        }

        NegociacaoHonorario negociacao = negociacaoRepository.save(new NegociacaoHonorario(pagamento, original,
                negociado, Textos.opcional(form.getMotivo()), hoje(), auditoriaService.usuarioAtual()));
        List<ParcelaHonorario> parcelas = persistirParcelas(negociacao, itens);
        atualizarSituacao(pagamento, negociacao, parcelas, jaRecebido);

        String tipo = anterior == null ? "Negociação" : "Renegociação";
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, PagamentoService.ENTIDADE, pagamento.getId(),
                tipo + " de honorário de '" + pagamento.getCliente().getNome() + "': original "
                        + Textos.moeda(original) + ", negociado " + Textos.moeda(negociado) + ", desconto "
                        + Textos.moeda(negociacao.getDesconto()) + ", " + parcelas.size() + " parcela(s)."
                        + (negociacao.getMotivo() == null ? "" : " Motivo: " + negociacao.getMotivo()));
        return negociacao;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalRecebido(Long pagamentoId) {
        return recebido(pagamentoService.buscar(pagamentoId));
    }

    @Transactional
    public RecebimentoHonorario registrarRecebimento(Long pagamentoId, Long parcelaId, RecebimentoForm form) {
        PagamentoAgendado pagamento = pagamentoService.buscar(pagamentoId);
        ParcelaHonorario parcela = parcelaDoPagamento(parcelaId, pagamentoId);
        if (parcela.getSituacao() != SituacaoParcela.ABERTA) {
            throw new RegraNegocioException("A parcela " + parcela.getNumero() + " está "
                    + parcela.getSituacao().getRotulo().toLowerCase() + ".");
        }
        if (form.getDataRecebimento() == null || form.getDataRecebimento().isAfter(hoje())) {
            throw new RegraNegocioException("dataRecebimento", "A data do recebimento não pode estar no futuro.");
        }
        BigDecimal saldoParcela = CalculoHonorarios.saldo(parcela.getValor(),
                recebimentoRepository.totalRecebidoNaParcela(parcelaId));
        if (form.getValor().compareTo(saldoParcela) > 0) {
            throw new RegraNegocioException("valor",
                    "O valor ultrapassa o saldo da parcela (" + Textos.moeda(saldoParcela) + ").");
        }

        RecebimentoHonorario recebimento = new RecebimentoHonorario(parcela, form.getDataRecebimento(),
                form.getValor(), form.getFormaPagamento());
        recebimento.setObservacoes(Textos.opcional(form.getObservacoes()));
        recebimentoRepository.save(recebimento);
        parcela.atualizarPagamento(recebimentoRepository.totalRecebidoNaParcela(parcelaId));
        recalcularPagamento(pagamento);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, PagamentoService.ENTIDADE, pagamento.getId(),
                "Recebimento de " + Textos.moeda(recebimento.getValor()) + " ("
                        + recebimento.getFormaPagamento().getRotulo() + ") em "
                        + Textos.data(recebimento.getDataRecebimento()) + " de '"
                        + pagamento.getCliente().getNome() + "', parcela " + parcela.getNumero() + ".");
        return recebimento;
    }

    @Transactional
    public RecebimentoHonorario estornar(Long pagamentoId, Long recebimentoId, String motivo) {
        PagamentoAgendado pagamento = pagamentoService.buscar(pagamentoId);
        RecebimentoHonorario recebimento = recebimentoRepository.findComDetalhesById(recebimentoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Recebimento não encontrado."));
        if (!Objects.equals(recebimento.getParcela().getNegociacao().getPagamento().getId(), pagamentoId)) {
            throw new RegistroNaoEncontradoException("Recebimento não encontrado.");
        }
        String motivoValidado = MotivoCancelamento.validar(motivo);
        if (recebimento.isCancelado()) {
            throw new RegraNegocioException("Este recebimento já está cancelado.");
        }
        recebimento.cancelar(motivoValidado, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        ParcelaHonorario parcela = recebimento.getParcela();
        parcela.atualizarPagamento(recebimentoRepository.totalRecebidoNaParcela(parcela.getId()));
        recalcularPagamento(pagamento);
        auditoriaService.registrar(AcaoAuditoria.CANCELAMENTO, PagamentoService.ENTIDADE, pagamento.getId(),
                "Recebimento de " + Textos.moeda(recebimento.getValor()) + " em "
                        + Textos.data(recebimento.getDataRecebimento()) + " de '"
                        + pagamento.getCliente().getNome() + "' estornado. Motivo: " + motivoValidado);
        return recebimento;
    }

    @Transactional(readOnly = true)
    public boolean temNegociacaoAtiva(Long pagamentoId) {
        return negociacaoAtiva(pagamentoId) != null;
    }

    private AgendaHonorarios montar(List<PagamentoAgendado> pagamentos, SituacaoHonorarioPagamento filtro) {
        List<LinhaHonorario> linhas = montarLinhas(pagamentos, true);
        if (filtro != null) {
            linhas = linhas.stream()
                    .filter(l -> (l.pagamento().getValorHonorario() != null || l.valores().continuacao())
                            && l.pagamento().getSituacaoHonorario() == filtro)
                    .toList();
        }
        return new AgendaHonorarios(linhas);
    }

    private List<LinhaHonorario> montarLinhas(List<PagamentoAgendado> pagamentos, boolean persistirSituacao) {
        if (pagamentos.isEmpty()) {
            return List.of();
        }
        ContextoHonorario contexto = carregar(pagamentos);
        Map<Long, ValoresHonorario> acordoPorOrigem = new HashMap<>();
        List<LinhaHonorario> linhas = new ArrayList<>();
        for (PagamentoAgendado pagamento : pagamentos) {
            if (pagamento.isContinuacao()) {
                PagamentoAgendado origem = pagamento.getHonorarioOrigem();
                ValoresHonorario acordo = acordoPorOrigem.computeIfAbsent(origem.getId(),
                        id -> valoresDaOrigem(origem, contexto, persistirSituacao));
                linhas.add(new LinhaHonorario(pagamento, ValoresHonorario.fatia(pagamento, origem, acordo)));
                continue;
            }
            linhas.add(new LinhaHonorario(pagamento, valoresDaOrigem(pagamento, contexto, persistirSituacao)));
        }
        return linhas;
    }

    private ContextoHonorario carregar(List<PagamentoAgendado> pagamentos) {
        Set<Long> ids = pagamentos.stream().map(PagamentoAgendado::getId).collect(Collectors.toCollection(HashSet::new));
        pagamentos.stream()
                .filter(PagamentoAgendado::isContinuacao)
                .map(p -> p.getHonorarioOrigem().getId())
                .forEach(ids::add);
        List<PagamentoAgendado> continuacoes = pagamentoRepository.findByHonorarioOrigemIdIn(ids);
        continuacoes.stream().map(p -> p.getHonorarioOrigem().getId()).forEach(ids::add);
        Map<Long, List<PagamentoAgendado>> porOrigem = continuacoes.stream()
                .collect(Collectors.groupingBy(p -> p.getHonorarioOrigem().getId()));
        List<Long> idLista = List.copyOf(ids);
        Map<Long, NegociacaoHonorario> ativas = idLista.isEmpty()
                ? Map.of()
                : negociacaoRepository.findByPagamentoIdInAndSituacao(idLista, SituacaoNegociacao.ATIVA).stream()
                .collect(Collectors.toMap(n -> n.getPagamento().getId(), n -> n));
        List<Long> negociacaoIds = ativas.values().stream().map(NegociacaoHonorario::getId).toList();
        Map<Long, List<ParcelaHonorario>> parcelasPorNegociacao = negociacaoIds.isEmpty()
                ? Map.of()
                : parcelaRepository.findByNegociacaoIdInOrderByNumeroAsc(negociacaoIds).stream()
                .collect(Collectors.groupingBy(p -> p.getNegociacao().getId()));
        Map<Long, BigDecimal> recebidos = idLista.isEmpty()
                ? Map.of()
                : recebimentoRepository.totaisRecebidosDosPagamentos(idLista).stream()
                .collect(Collectors.toMap(RecebimentoHonorarioRepository.TotalPorPagamento::getPagamentoId,
                        RecebimentoHonorarioRepository.TotalPorPagamento::getTotal));
        return new ContextoHonorario(ativas, parcelasPorNegociacao, recebidos, porOrigem);
    }

    private ValoresHonorario valoresDaOrigem(PagamentoAgendado origem, ContextoHonorario contexto,
                                             boolean persistirSituacao) {
        NegociacaoHonorario ativa = contexto.ativas.get(origem.getId());
        List<ParcelaHonorario> parcelas = ativa == null
                ? List.of()
                : contexto.parcelasPorNegociacao.getOrDefault(ativa.getId(), List.of());
        List<PagamentoAgendado> filhos = contexto.continuacoesPorOrigem.getOrDefault(origem.getId(), List.of());
        BigDecimal recebido = CalculoHonorarios.recebidoDaOrigem(origem, filhos,
                contexto.recebidosParcelas.getOrDefault(origem.getId(), BigDecimal.ZERO), ativa != null);
        if (persistirSituacao) {
            atualizarSituacao(origem, ativa, parcelas, recebido, filhos);
        }
        int quantidadeAtivas = (int) parcelas.stream().filter(ParcelaHonorario::isAtiva).count();
        return ValoresHonorario.de(origem, ativa, recebido, quantidadeAtivas);
    }

    private record ContextoHonorario(Map<Long, NegociacaoHonorario> ativas,
                                     Map<Long, List<ParcelaHonorario>> parcelasPorNegociacao,
                                     Map<Long, BigDecimal> recebidosParcelas,
                                     Map<Long, List<PagamentoAgendado>> continuacoesPorOrigem) {
    }

    private void recalcularPagamento(PagamentoAgendado pagamento) {
        NegociacaoHonorario ativa = negociacaoAtiva(pagamento.getId());
        List<ParcelaHonorario> parcelas = ativa == null
                ? List.of()
                : parcelaRepository.findByNegociacaoIdOrderByNumeroAsc(ativa.getId());
        List<PagamentoAgendado> filhos = pagamentoRepository
                .findByHonorarioOrigemIdOrderByDataRecebimentoAscIdAsc(pagamento.getId());
        atualizarSituacao(pagamento, ativa, parcelas, recebido(pagamento), filhos);
    }

    /**
     * Recebimentos das parcelas mais o valor que já tinha sido pago no lançamento "Parcial" e, sem
     * negociação, o que foi pago nos pagamentos que continuam este honorário.
     */
    private BigDecimal recebido(PagamentoAgendado pagamento) {
        if (pagamento.isContinuacao()) {
            return CalculoHonorarios.pagoNaContinuacao(pagamento);
        }
        List<PagamentoAgendado> filhos = pagamentoRepository
                .findByHonorarioOrigemIdOrderByDataRecebimentoAscIdAsc(pagamento.getId());
        boolean negociado = negociacaoAtiva(pagamento.getId()) != null;
        return CalculoHonorarios.recebidoDaOrigem(pagamento, filhos,
                recebimentoRepository.totalRecebidoDoPagamento(pagamento.getId()), negociado);
    }

    private void atualizarSituacao(PagamentoAgendado pagamento, NegociacaoHonorario ativa,
                                   List<ParcelaHonorario> parcelas, BigDecimal recebido) {
        atualizarSituacao(pagamento, ativa, parcelas, recebido,
                pagamentoRepository.findByHonorarioOrigemIdOrderByDataRecebimentoAscIdAsc(pagamento.getId()));
    }

    /** As continuações repetem a situação da origem: a dívida com o escritório é uma só. */
    private void atualizarSituacao(PagamentoAgendado pagamento, NegociacaoHonorario ativa,
                                   List<ParcelaHonorario> parcelas, BigDecimal recebido,
                                   List<PagamentoAgendado> continuacoes) {
        if (pagamento.isContinuacao()) {
            return;
        }
        boolean vencida = parcelas.stream().anyMatch(p -> p.isVencida(hoje()));
        SituacaoHonorarioPagamento calculada = CalculoHonorarios.situacao(pagamento.getValorHonorario(),
                pagamento.getSituacaoHonorario(), ativa != null,
                ativa == null ? null : ativa.getValorNegociado(), recebido, vencida, !continuacoes.isEmpty());
        if (pagamento.getSituacaoHonorario() != calculada) {
            pagamento.setSituacaoHonorario(calculada);
        }
        for (PagamentoAgendado continuacao : continuacoes) {
            if (continuacao.getSituacaoHonorario() != calculada) {
                continuacao.setSituacaoHonorario(calculada);
            }
        }
    }

    private HonorarioAberto paraAberto(PagamentoAgendado origem) {
        BigDecimal recebido = recebido(origem);
        NegociacaoHonorario ativa = negociacaoAtiva(origem.getId());
        BigDecimal devido = ativa != null ? ativa.getValorNegociado() : origem.getValorHonorario();
        return new HonorarioAberto(origem.getId(), origem.getDataRecebimento().toString(),
                Textos.data(origem.getDataRecebimento()), origem.getTipoServico().getNome(),
                ativa != null ? ativa.getValorOriginal() : origem.getValorHonorario(), recebido,
                CalculoHonorarios.saldo(devido, recebido), ativa != null);
    }

    private List<AbatimentoHonorario> abatimentos(PagamentoAgendado origem, List<PagamentoAgendado> continuacoes) {
        if (origem.isContinuacao() || continuacoes.isEmpty()) {
            return List.of();
        }
        List<AbatimentoHonorario> itens = new ArrayList<>();
        itens.add(new AbatimentoHonorario(origem, CalculoHonorarios.pagoNaOrigem(origem, true), true));
        for (PagamentoAgendado continuacao : continuacoes) {
            itens.add(new AbatimentoHonorario(continuacao, CalculoHonorarios.pagoNaContinuacao(continuacao), false));
        }
        return itens;
    }

    private List<ParcelaNegociacaoItem> itensValidados(NegociacaoForm form, BigDecimal saldo) {
        if (saldo == null || saldo.signum() <= 0) {
            return List.of();
        }
        List<ParcelaNegociacaoItem> itens = parcelasInformadas(form);
        if (itens.isEmpty()) {
            itens = sugerirItens(form.getQuantidade(), form.getPrimeiroVencimento(), saldo);
        }
        BigDecimal soma = itens.stream().map(ParcelaNegociacaoItem::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (soma.compareTo(saldo) != 0) {
            throw new RegraNegocioException("parcelas",
                    "A soma das parcelas deve ser igual ao valor a parcelar (" + Textos.moeda(saldo) + ").");
        }
        if (itens.stream().anyMatch(item -> item.getVencimento() == null)) {
            throw new RegraNegocioException("parcelas", "Informe o vencimento de todas as parcelas.");
        }
        return itens;
    }

    private List<ParcelaHonorario> persistirParcelas(NegociacaoHonorario negociacao, List<ParcelaNegociacaoItem> itens) {
        List<ParcelaHonorario> parcelas = new ArrayList<>();
        int numero = 1;
        for (ParcelaNegociacaoItem item : itens) {
            ParcelaHonorario parcela = new ParcelaHonorario(negociacao, numero++, item.getVencimento(), item.getValor());
            parcelaRepository.save(parcela);
            parcelas.add(parcela);
        }
        return parcelas;
    }

    private void substituir(NegociacaoHonorario anterior) {
        String usuario = auditoriaService.usuarioAtual();
        LocalDateTime agora = LocalDateTime.now(relogio);
        for (ParcelaHonorario parcela : parcelaRepository.findByNegociacaoIdOrderByNumeroAsc(anterior.getId())) {
            if (parcela.getSituacao() == SituacaoParcela.ABERTA) {
                parcela.cancelar(MOTIVO_SUBSTITUICAO, usuario, agora);
            }
        }
        anterior.substituir();
        negociacaoRepository.flush();
    }

    private NegociacaoHonorario negociacaoAtiva(Long pagamentoId) {
        return negociacaoRepository.findByPagamentoIdAndSituacao(pagamentoId, SituacaoNegociacao.ATIVA).orElse(null);
    }

    private ParcelaHonorario parcelaDoPagamento(Long parcelaId, Long pagamentoId) {
        ParcelaHonorario parcela = parcelaRepository.findComDetalhesById(parcelaId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Parcela não encontrada."));
        if (!Objects.equals(parcela.getNegociacao().getPagamento().getId(), pagamentoId)) {
            throw new RegistroNaoEncontradoException("Parcela não encontrada.");
        }
        return parcela;
    }

    private static void exigirHonorarioLancado(PagamentoAgendado pagamento) {
        if (pagamento.isContinuacao()) {
            throw new RegraNegocioException("Este pagamento continua o honorário de "
                    + Textos.data(pagamento.getHonorarioOrigem().getDataRecebimento())
                    + ". Negocie no honorário original.");
        }
        if (pagamento.getValorHonorario() == null) {
            throw new RegraNegocioException("Lance o valor do honorário antes de registrar a negociação.");
        }
    }

    private Map<Long, BigDecimal> totaisPorParcela(List<ParcelaHonorario> parcelas) {
        List<Long> ids = parcelas.stream().map(ParcelaHonorario::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return recebimentoRepository.totaisRecebidosDasParcelas(ids).stream()
                .collect(Collectors.toMap(RecebimentoHonorarioRepository.TotalPorParcela::getParcelaId,
                        RecebimentoHonorarioRepository.TotalPorParcela::getTotal));
    }

    private Map<Long, List<RecebimentoHonorario>> recebimentosPorParcela(List<ParcelaHonorario> parcelas) {
        List<Long> ids = parcelas.stream().map(ParcelaHonorario::getId).filter(Objects::nonNull).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<RecebimentoHonorario>> porParcela = new HashMap<>();
        for (RecebimentoHonorario recebimento : recebimentoRepository.findByParcelaIdInOrderByDataRecebimentoAscIdAsc(ids)) {
            porParcela.computeIfAbsent(recebimento.getParcela().getId(), k -> new ArrayList<>()).add(recebimento);
        }
        return porParcela;
    }

    private static List<ParcelaNegociacaoItem> parcelasInformadas(NegociacaoForm form) {
        return form.getParcelas().stream()
                .filter(p -> p.getValor() != null && p.getVencimento() != null)
                .toList();
    }

    static void preencherParcelasSugeridas(NegociacaoForm form, BigDecimal saldo) {
        form.setParcelas(sugerirItens(form.getQuantidade() == null ? 1 : form.getQuantidade(),
                form.getPrimeiroVencimento(), saldo == null ? BigDecimal.ZERO : saldo));
    }

    private static List<ParcelaNegociacaoItem> sugerirItens(int quantidade, LocalDate primeiro, BigDecimal total) {
        if (quantidade < 1 || primeiro == null || total == null || total.signum() <= 0) {
            return List.of();
        }
        List<BigDecimal> valores = CalculoHonorarios.dividir(total, quantidade);
        List<ParcelaNegociacaoItem> itens = new ArrayList<>();
        for (int i = 0; i < valores.size(); i++) {
            ParcelaNegociacaoItem item = new ParcelaNegociacaoItem();
            item.setVencimento(primeiro.plusMonths(i));
            item.setValor(valores.get(i));
            itens.add(item);
        }
        return itens;
    }
}
