package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
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
import java.util.Set;

/**
 * Agenda de pagamentos da recepção. Cada pagamento fica ligado direto ao cliente, que é
 * encontrado ou cadastrado pelo CPF no próprio lançamento.
 */
@Service
public class PagamentoService {

    public static final String ENTIDADE = "Pagamento agendado";

    private static final String CAMPO_NOME = "nomeCliente";
    private static final String CAMPO_CPF = "cpf";
    private static final String CAMPO_RESPONSAVEIS = "responsavelIds";
    private static final String CAMPO_VALOR_HONORARIO = "valorHonorario";
    private static final String CAMPO_VALOR_HONORARIO_PAGO = "valorHonorarioPago";
    private static final String CAMPO_SITUACAO_HONORARIO = "situacaoHonorario";
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
        CriterioBusca criterio = criterio(filtro.getBusca());
        List<StatusPagamento> status = filtro.getStatus() == null
                ? Arrays.asList(StatusPagamento.values())
                : List.of(filtro.getStatus());
        return new AgendaPagamentos(pagamentoRepository.pesquisar(filtro.getInicio(), filtro.getFim(), status,
                criterio.padraoCliente, criterio.padraoCpf, filtro.getResponsavelId(), filtro.getTipoServicoId()));
    }

    @Transactional(readOnly = true)
    public AgendaPagamentos honorarios(FiltroPagamentos filtro) {
        return new AgendaPagamentos(pesquisarHonorarios(filtro, false));
    }

    @Transactional(readOnly = true)
    public List<PagamentoAgendado> pesquisarHonorarios(FiltroPagamentos filtro, boolean somenteComValor) {
        CriterioBusca criterio = criterio(filtro.getBusca());
        return pagamentoRepository.pesquisarHonorarios(filtro.getInicio(), filtro.getFim(),
                somenteComValor, null, criterio.padraoCliente, criterio.padraoCpf,
                filtro.getResponsavelId(), filtro.getTipoServicoId());
    }

    @Transactional(readOnly = true)
    public List<PagamentoAgendado> pesquisarHonorarios(FiltroRelatorio filtro, boolean somenteComValor) {
        CriterioBusca criterio = criterio(null);
        return pagamentoRepository.pesquisarHonorarios(filtro.getInicioEfetivo(), filtro.getFimEfetivo(),
                somenteComValor, null, criterio.padraoCliente, criterio.padraoCpf, filtro.getResponsavelId(),
                filtro.getTipoServicoId());
    }

    @Transactional(readOnly = true)
    public AgendaRepasses repasses(FiltroPagamentos filtro) {
        CriterioBusca criterio = criterio(filtro.getBusca());
        return new AgendaRepasses(responsavelPagamentoRepository.pesquisarRepasses(filtro.getInicio(), filtro.getFim(),
                false, filtro.getSituacaoRepasse(), criterio.padraoCliente, criterio.padraoCpf,
                filtro.getResponsavelId(), filtro.getTipoServicoId()));
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
        CriterioBusca criterio = criterio(null);
        StatusPagamento statusFiltro = filtro.situacaoComo(StatusPagamento.class);
        List<StatusPagamento> status = statusFiltro == null
                ? Arrays.asList(StatusPagamento.values()) : List.of(statusFiltro);
        return new AgendaPagamentos(pagamentoRepository.pesquisar(filtro.getInicioEfetivo(), filtro.getFimEfetivo(),
                status, criterio.padraoCliente, criterio.padraoCpf, filtro.getResponsavelId(),
                filtro.getTipoServicoId()));
    }

    @Transactional(readOnly = true)
    public AgendaPagamentos relatorioHonorarios(FiltroRelatorio filtro) {
        return new AgendaPagamentos(pesquisarHonorarios(filtro, true));
    }

    @Transactional(readOnly = true)
    public AgendaRepasses relatorioRepasses(FiltroRelatorio filtro) {
        CriterioBusca criterio = criterio(null);
        return new AgendaRepasses(responsavelPagamentoRepository.pesquisarRepasses(filtro.getInicioEfetivo(),
                filtro.getFimEfetivo(), true, filtro.situacaoComo(SituacaoRepassePagamento.class),
                criterio.padraoCliente, criterio.padraoCpf, filtro.getResponsavelId(), filtro.getTipoServicoId()));
    }

    /**
     * @param incluirHonorario se o usuário pode lançar honorários; sem essa permissão, o honorário do formulário é
     *                         ignorado e o pagamento nasce sem valor e "A receber"
     */
    @Transactional
    public PagamentoAgendado criar(PagamentoForm form, boolean incluirHonorario) {
        validarValorOpcional(form.getValor(), "O valor", "valor");
        SituacaoHonorarioPagamento situacaoHonorario = incluirHonorario ? situacaoHonorarioDoFormulario(form) : null;
        BigDecimal valorHonorarioPago = incluirHonorario
                ? valorPagoValidado(form.getValorHonorario(), situacaoHonorario, form.getValorHonorarioPago(),
                form.getValorHonorarioFalta(), CamposParcial.FORMULARIO)
                : null;
        List<Responsavel> responsaveis = idsResponsaveis(form.getResponsavelIds()).stream()
                .map(this::responsavelAtivo)
                .toList();
        TipoServico tipo = tipoServicoAtivo(form.getTipoServicoId());
        Cliente cliente = cliente(form);

        PagamentoAgendado pagamento = new PagamentoAgendado(cliente, responsaveis, tipo, form.getDataRecebimento(),
                form.getValor(), Textos.opcional(form.getBanco()));
        pagamento.setStatus(form.getStatus());
        pagamento.setObservacoes(Textos.opcional(form.getObservacoes()));
        if (incluirHonorario) {
            pagamento.setValorHonorario(form.getValorHonorario());
            pagamento.setValorHonorarioPago(valorHonorarioPago);
            pagamento.setSituacaoHonorario(situacaoHonorario);
        }
        pagamentoRepository.save(pagamento);

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
        SituacaoHonorarioPagamento situacaoHonorario = alterarHonorario ? situacaoHonorarioDoFormulario(form) : null;
        BigDecimal valorHonorarioPago = alterarHonorario
                ? valorPagoValidado(form.getValorHonorario(), situacaoHonorario, form.getValorHonorarioPago(),
                form.getValorHonorarioFalta(), CamposParcial.FORMULARIO)
                : null;
        List<String> alteracoes = new ArrayList<>();

        Cliente cliente = cliente(form);
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
        if (alterarHonorario) {
            aplicarHonorario(pagamento, form.getValorHonorario(), situacaoHonorario, valorHonorarioPago, alteracoes);
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
        BigDecimal pagoValidado = valorPagoValidado(valor, situacao, valorPago, valorFalta, CamposParcial.LINHA);
        PagamentoAgendado pagamento = buscar(id);
        if (temNegociacaoAtiva(id)) {
            throw new RegraNegocioException(
                    "Este honorário tem uma negociação. Altere parcelas e recebimentos no detalhe.");
        }
        List<String> alteracoes = new ArrayList<>();
        aplicarHonorario(pagamento, valor, situacao, pagoValidado, alteracoes);
        if (!alteracoes.isEmpty()) {
            registrarAlteracao(pagamento, String.join(", ", alteracoes));
        }
        return pagamento;
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
        if (valorHonorario == null) {
            throw new RegraNegocioException(campos.total(),
                    "No Parcial, informe o valor total do honorário (não só o que foi pago) para calcular quanto falta.");
        }
        if (valorPago == null && valorFalta == null) {
            throw new RegraNegocioException(campos.pago(),
                    "Informe quanto o cliente já pagou ou quanto ainda falta pagar do honorário.");
        }
        if (valorPago == null) {
            validarValorOpcional(valorFalta, "O valor que falta", campos.falta());
            if (valorFalta.compareTo(valorHonorario) >= 0) {
                throw new RegraNegocioException(campos.falta(), "O valor que falta deve ser menor que o honorário ("
                        + Textos.moeda(valorHonorario) + "). Se o cliente ainda não pagou nada, escolha \"A receber\".");
            }
            return valorHonorario.subtract(valorFalta);
        }
        validarValorOpcional(valorPago, "O valor já pago", campos.pago());
        if (valorPago.compareTo(valorHonorario) >= 0) {
            throw new RegraNegocioException(campos.pago(), "O valor já pago deve ser menor que o honorário ("
                    + Textos.moeda(valorHonorario) + "). Se o cliente pagou tudo, escolha \"Recebido\".");
        }
        if (valorFalta != null && valorPago.add(valorFalta).compareTo(valorHonorario) != 0) {
            throw new RegraNegocioException(campos.falta(), "O valor já pago mais o que falta deve dar o honorário ("
                    + Textos.moeda(valorHonorario) + "). Preencha só um dos dois.");
        }
        return valorPago;
    }

    /** Nomes dos campos do honorário "Parcial" em cada tela, para a mensagem de erro aparecer no campo certo. */
    private record CamposParcial(String total, String pago, String falta) {
        static final CamposParcial FORMULARIO =
                new CamposParcial(CAMPO_VALOR_HONORARIO, CAMPO_VALOR_HONORARIO_PAGO, "valorHonorarioFalta");
        static final CamposParcial LINHA = new CamposParcial("valor", "valorPago", "valorFalta");
    }

    private static void validarSituacaoSimples(SituacaoHonorarioPagamento situacao, String campo) {
        if (!situacao.isLancamentoSimples()) {
            throw new RegraNegocioException(campo,
                    "A situação " + situacao.getRotulo() + " é calculada a partir da negociação.");
        }
    }

    private static String descricaoHonorarioNaCriacao(PagamentoAgendado pagamento) {
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
     * Repasse de um dos responsáveis do pagamento ({@code id} da linha de {@link ResponsavelPagamento}).
     * Valor em branco ({@code null}) significa que não há repasse a esse responsável.
     */
    @Transactional
    public ResponsavelPagamento lancarRepasse(Long id, BigDecimal valor, SituacaoRepassePagamento situacao) {
        validarValorOpcional(valor, "O repasse", "valor");
        ResponsavelPagamento repasse = responsavelPagamentoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Repasse não encontrado."));
        String nome = "'" + repasse.getResponsavel().getNome() + "'";
        List<String> alteracoes = new ArrayList<>();
        if (valorDiferente(repasse.getValorRepasse(), valor)) {
            alteracoes.add("repasse de " + nome + " de " + moedaOuSem(repasse.getValorRepasse())
                    + " para " + moedaOuSem(valor));
            repasse.setValorRepasse(valor);
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
            if (vinculo.getSituacaoRepasse() == SituacaoRepassePagamento.PAGO) {
                throw new RegraNegocioException(CAMPO_RESPONSAVEIS, "O repasse de '"
                        + vinculo.getResponsavel().getNome() + "' já foi pago; ele não pode sair deste pagamento.");
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
            return clienteService.obterEscolhido(form.getClienteId(), form.getCpf(), CAMPO_CPF);
        }
        return clienteService.obterOuCadastrarPorCpf(form.getNomeCliente(), form.getCpf(), hoje(),
                CAMPO_NOME, CAMPO_CPF);
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

    private static CriterioBusca criterio(String busca) {
        String termo = busca == null ? "" : busca.trim();
        String digitos = termo.replaceAll("[\\s.\\-]", "");
        String padraoCpf = digitos.matches("\\d{" + DIGITOS_MINIMOS_BUSCA_CPF + ",11}") ? "%" + digitos + "%" : "";
        return new CriterioBusca(Textos.padraoBusca(termo), padraoCpf);
    }

    private record CriterioBusca(String padraoCliente, String padraoCpf) {
    }
}
