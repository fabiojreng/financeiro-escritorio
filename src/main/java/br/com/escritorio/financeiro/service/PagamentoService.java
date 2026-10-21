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

    @Transactional
    public PagamentoAgendado criar(PagamentoForm form) {
        validarValor(form.getValor());
        List<Responsavel> responsaveis = idsResponsaveis(form.getResponsavelIds()).stream()
                .map(this::responsavelAtivo)
                .toList();
        TipoServico tipo = tipoServicoAtivo(form.getTipoServicoId());
        Cliente cliente = cliente(form);

        PagamentoAgendado pagamento = new PagamentoAgendado(cliente, responsaveis, tipo, form.getDataRecebimento(),
                form.getValor(), form.getBanco().trim());
        pagamento.setStatus(form.getStatus());
        pagamento.setObservacoes(Textos.opcional(form.getObservacoes()));
        pagamentoRepository.save(pagamento);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, pagamento.getId(),
                "Pagamento de '" + cliente.getNome() + "' (" + tipo.getNome() + ") agendado para "
                        + Textos.data(pagamento.getDataRecebimento()) + ", valor " + Textos.moeda(pagamento.getValor())
                        + ", banco " + pagamento.getBanco() + ", cliente recebeu? " + pagamento.getStatus().getRotulo()
                        + ", " + (responsaveis.size() == 1 ? "responsável " : "responsáveis ")
                        + pagamento.getNomesResponsaveis() + ".");
        return pagamento;
    }

    @Transactional
    public PagamentoAgendado atualizar(Long id, PagamentoForm form) {
        PagamentoAgendado pagamento = buscar(id);
        validarValor(form.getValor());
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
        if (pagamento.getValor().compareTo(form.getValor()) != 0) {
            alteracoes.add("valor de " + Textos.moeda(pagamento.getValor()) + " para " + Textos.moeda(form.getValor()));
            pagamento.setValor(form.getValor());
        }
        String banco = form.getBanco().trim();
        if (!pagamento.getBanco().equals(banco)) {
            alteracoes.add("banco de " + pagamento.getBanco() + " para " + banco);
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

    /** Valor em branco ({@code null}) significa que ainda não há honorário combinado para o pagamento. */
    @Transactional
    public PagamentoAgendado lancarHonorario(Long id, BigDecimal valor, SituacaoHonorarioPagamento situacao) {
        validarValorOpcional(valor, "O honorário");
        if (!situacao.isLancamentoSimples()) {
            throw new RegraNegocioException("situacao",
                    "A situação " + situacao.getRotulo() + " é calculada a partir da negociação.");
        }
        PagamentoAgendado pagamento = buscar(id);
        if (negociacaoHonorarioRepository.findByPagamentoIdAndSituacao(id, SituacaoNegociacao.ATIVA).isPresent()) {
            throw new RegraNegocioException(
                    "Este honorário tem uma negociação. Altere parcelas e recebimentos no detalhe.");
        }
        List<String> alteracoes = new ArrayList<>();
        if (valorDiferente(pagamento.getValorHonorario(), valor)) {
            alteracoes.add("honorário de " + moedaOuSem(pagamento.getValorHonorario()) + " para " + moedaOuSem(valor));
            pagamento.setValorHonorario(valor);
        }
        if (pagamento.getSituacaoHonorario() != situacao) {
            alteracoes.add(descricaoSituacaoHonorario(pagamento.getSituacaoHonorario(), situacao));
            pagamento.setSituacaoHonorario(situacao);
        }
        if (!alteracoes.isEmpty()) {
            registrarAlteracao(pagamento, String.join(", ", alteracoes));
        }
        return pagamento;
    }

    /**
     * Repasse de um dos responsáveis do pagamento ({@code id} da linha de {@link ResponsavelPagamento}).
     * Valor em branco ({@code null}) significa que não há repasse a esse responsável.
     */
    @Transactional
    public ResponsavelPagamento lancarRepasse(Long id, BigDecimal valor, SituacaoRepassePagamento situacao) {
        validarValorOpcional(valor, "O repasse");
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

    private static void validarValorOpcional(BigDecimal valor, String nome) {
        if (valor == null) {
            return;
        }
        if (valor.signum() <= 0) {
            throw new RegraNegocioException("valor", nome + " deve ser maior que zero.");
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new RegraNegocioException("valor", nome + " deve ter no máximo duas casas decimais.");
        }
        if (valor.precision() - valor.scale() > 13) {
            throw new RegraNegocioException("valor", nome + " é grande demais.");
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

    private static void validarValor(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new RegraNegocioException("valor", "O valor deve ser maior que zero.");
        }
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
