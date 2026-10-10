package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.CalculoHonorarios;
import br.com.escritorio.financeiro.domain.FormaCobranca;
import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import br.com.escritorio.financeiro.domain.SituacaoParcela;
import br.com.escritorio.financeiro.dto.AcordoComTotais;
import br.com.escritorio.financeiro.dto.AcordoForm;
import br.com.escritorio.financeiro.dto.FiltroSaldo;
import br.com.escritorio.financeiro.dto.GerarParcelasForm;
import br.com.escritorio.financeiro.dto.ParcelaForm;
import br.com.escritorio.financeiro.dto.RecebimentoForm;
import br.com.escritorio.financeiro.dto.ResumoHonorarios;
import br.com.escritorio.financeiro.repository.AcordoHonorariosRepository;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.ParcelaHonorarioRepository;
import br.com.escritorio.financeiro.repository.RecebimentoHonorarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Regras dos honorários. O saldo é o valor contratado menos os recebimentos não cancelados; nada
 * do benefício do INSS entra na conta, nem para preencher o valor de um acordo percentual.
 * Lançamentos financeiros não são apagados: parcelas e recebimentos são cancelados com motivo.
 */
@Service
public class HonorariosService {

    public static final String ENTIDADE_ACORDO = "Acordo de honorários";
    public static final String ENTIDADE_PARCELA = "Parcela de honorários";
    public static final String ENTIDADE_RECEBIMENTO = "Recebimento de honorários";

    static final int TAMANHO_MINIMO_MOTIVO = 5;
    static final int TAMANHO_MAXIMO_MOTIVO = 500;

    private final AcordoHonorariosRepository acordoRepository;
    private final ParcelaHonorarioRepository parcelaRepository;
    private final RecebimentoHonorarioRepository recebimentoRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public HonorariosService(AcordoHonorariosRepository acordoRepository, ParcelaHonorarioRepository parcelaRepository,
                             RecebimentoHonorarioRepository recebimentoRepository,
                             AtendimentoRepository atendimentoRepository, AuditoriaService auditoriaService,
                             Clock relogio) {
        this.acordoRepository = acordoRepository;
        this.parcelaRepository = parcelaRepository;
        this.recebimentoRepository = recebimentoRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public AcordoHonorarios buscarAcordo(Long id) {
        return acordoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Acordo de honorários não encontrado."));
    }

    @Transactional(readOnly = true)
    public ParcelaHonorario buscarParcela(Long id) {
        return parcelaRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Parcela não encontrada."));
    }

    @Transactional(readOnly = true)
    public ResumoHonorarios resumo(Long acordoId) {
        AcordoHonorarios acordo = buscarAcordo(acordoId);
        return ResumoHonorarios.de(acordo, parcelaRepository.findByAcordoIdOrderByNumeroAsc(acordoId),
                recebimentoRepository.findByAcordoIdOrderByDataRecebimentoDescIdDesc(acordoId));
    }

    @Transactional(readOnly = true)
    public ResumoHonorarios resumoDoAtendimento(Long atendimentoId) {
        return acordoRepository.findByAtendimentoId(atendimentoId)
                .map(acordo -> resumo(acordo.getId()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AcordoComTotais> listar(String buscaCliente, Long responsavelId, FiltroSaldo filtro) {
        Map<Long, BigDecimal> recebidos = recebimentoRepository.totaisRecebidosPorAcordo().stream()
                .collect(Collectors.toMap(RecebimentoHonorarioRepository.TotalPorAcordo::getAcordoId,
                        RecebimentoHonorarioRepository.TotalPorAcordo::getTotal));
        FiltroSaldo filtroEfetivo = filtro == null ? FiltroSaldo.TODOS : filtro;
        return acordoRepository.pesquisar(Textos.padraoBusca(buscaCliente), responsavelId).stream()
                .map(acordo -> {
                    BigDecimal recebido = recebidos.getOrDefault(acordo.getId(), BigDecimal.ZERO);
                    return new AcordoComTotais(acordo, recebido,
                            CalculoHonorarios.saldo(acordo.getValorContratado(), recebido));
                })
                .filter(filtroEfetivo::aceita)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Auditoria> historico(Long acordoId) {
        List<Long> parcelas = parcelaRepository.findByAcordoIdOrderByNumeroAsc(acordoId).stream()
                .map(ParcelaHonorario::getId).toList();
        List<Long> recebimentos = recebimentoRepository.findByAcordoIdOrderByDataRecebimentoDescIdDesc(acordoId)
                .stream().map(RecebimentoHonorario::getId).toList();
        return auditoriaService.historico(Map.of(
                ENTIDADE_ACORDO, List.of(acordoId),
                ENTIDADE_PARCELA, parcelas,
                ENTIDADE_RECEBIMENTO, recebimentos));
    }

    @Transactional
    public AcordoHonorarios criarAcordo(Long atendimentoId, AcordoForm form) {
        Atendimento atendimento = atendimentoRepository.findComDetalhesById(atendimentoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Atendimento não encontrado."));
        if (atendimento.getSituacao() == SituacaoAtendimento.CANCELADO) {
            throw new RegraNegocioException(
                    "Este atendimento está cancelado. Altere a situação dele antes de registrar honorários.");
        }
        if (acordoRepository.existsByAtendimentoId(atendimentoId)) {
            throw new RegraNegocioException("Este atendimento já possui um acordo de honorários.");
        }
        validarCobranca(form);

        AcordoHonorarios acordo = new AcordoHonorarios(atendimento, form.getFormaCobranca(), form.getDataAcordo());
        aplicar(acordo, form);
        acordoRepository.save(acordo);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE_ACORDO, acordo.getId(),
                "Acordo de honorários de '" + atendimento.getCliente().getNome() + "' ("
                        + atendimento.getTipoServico().getNome() + "): " + descricaoCobranca(acordo)
                        + ", valor contratado " + valorContratado(acordo.getValorContratado()) + ".");
        return acordo;
    }

    @Transactional
    public AcordoHonorarios atualizarAcordo(Long id, AcordoForm form) {
        AcordoHonorarios acordo = buscarAcordo(id);
        validarCobranca(form);
        if (form.getValorContratado() != null) {
            BigDecimal recebido = recebimentoRepository.totalRecebidoNoAcordo(id);
            if (form.getValorContratado().compareTo(recebido) < 0) {
                throw new RegraNegocioException("valorContratado",
                        "O valor contratado não pode ser menor que o total já recebido (" + Textos.moeda(recebido) + ").");
            }
        }

        List<String> alteracoes = new ArrayList<>();
        String cobrancaAntes = descricaoCobranca(acordo);
        BigDecimal valorAntes = acordo.getValorContratado();
        Integer parcelasAntes = acordo.getQuantidadeParcelas();
        String condicoesAntes = acordo.getCondicoes();
        LocalDate dataAntes = acordo.getDataAcordo();

        aplicar(acordo, form);

        if (!cobrancaAntes.equals(descricaoCobranca(acordo))) {
            alteracoes.add("cobrança de " + cobrancaAntes + " para " + descricaoCobranca(acordo));
        }
        if (!mesmoValor(valorAntes, acordo.getValorContratado())) {
            alteracoes.add("valor contratado de " + valorContratado(valorAntes) + " para "
                    + valorContratado(acordo.getValorContratado()));
        }
        if (!Objects.equals(parcelasAntes, acordo.getQuantidadeParcelas())) {
            alteracoes.add("quantidade de parcelas");
        }
        if (!Objects.equals(condicoesAntes, acordo.getCondicoes())) {
            alteracoes.add("condições");
        }
        if (!dataAntes.equals(acordo.getDataAcordo())) {
            alteracoes.add("data do acordo de " + Textos.data(dataAntes) + " para " + Textos.data(acordo.getDataAcordo()));
        }
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE_ACORDO, acordo.getId(),
                    "Acordo de honorários de '" + acordo.getAtendimento().getCliente().getNome() + "' alterado: "
                            + String.join(", ", alteracoes) + ".");
        }
        return acordo;
    }

    @Transactional
    public List<ParcelaHonorario> gerarParcelas(Long acordoId, GerarParcelasForm form) {
        AcordoHonorarios acordo = buscarAcordo(acordoId);
        if (parcelaRepository.existsByAcordoIdAndSituacaoNot(acordoId, SituacaoParcela.CANCELADA)) {
            throw new RegraNegocioException(
                    "Este acordo já tem parcelas ativas. Adicione parcelas uma a uma ou cancele as atuais antes.");
        }
        int numero = parcelaRepository.maiorNumero(acordoId);
        List<BigDecimal> valores = CalculoHonorarios.dividir(form.getValorTotal(), form.getQuantidade());
        List<ParcelaHonorario> parcelas = new ArrayList<>();
        for (int i = 0; i < valores.size(); i++) {
            parcelas.add(new ParcelaHonorario(acordo, ++numero, form.getPrimeiroVencimento().plusMonths(i),
                    valores.get(i)));
        }
        parcelaRepository.saveAll(parcelas);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE_ACORDO, acordo.getId(),
                valores.size() + " parcela(s) gerada(s) para '" + acordo.getAtendimento().getCliente().getNome()
                        + "', total " + Textos.moeda(form.getValorTotal()) + ", primeiro vencimento em "
                        + Textos.data(form.getPrimeiroVencimento()) + ".");
        return parcelas;
    }

    @Transactional
    public ParcelaHonorario adicionarParcela(Long acordoId, ParcelaForm form) {
        AcordoHonorarios acordo = buscarAcordo(acordoId);
        ParcelaHonorario parcela = new ParcelaHonorario(acordo, parcelaRepository.maiorNumero(acordoId) + 1,
                form.getVencimento(), form.getValor());
        parcelaRepository.save(parcela);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE_PARCELA, parcela.getId(),
                "Parcela " + parcela.getNumero() + " de '" + acordo.getAtendimento().getCliente().getNome()
                        + "' cadastrada: " + Textos.moeda(parcela.getValor()) + " com vencimento em "
                        + Textos.data(parcela.getVencimento()) + ".");
        return parcela;
    }

    @Transactional
    public ParcelaHonorario atualizarParcela(Long id, ParcelaForm form) {
        ParcelaHonorario parcela = buscarParcela(id);
        if (parcela.getSituacao() != SituacaoParcela.ABERTA) {
            throw new RegraNegocioException("Só parcelas abertas podem ser alteradas.");
        }
        List<String> alteracoes = new ArrayList<>();
        if (!parcela.getVencimento().equals(form.getVencimento())) {
            alteracoes.add("vencimento de " + Textos.data(parcela.getVencimento()) + " para "
                    + Textos.data(form.getVencimento()));
            parcela.setVencimento(form.getVencimento());
        }
        if (parcela.getValor().compareTo(form.getValor()) != 0) {
            alteracoes.add("valor de " + Textos.moeda(parcela.getValor()) + " para " + Textos.moeda(form.getValor()));
            parcela.setValor(form.getValor());
            parcela.atualizarPagamento(recebimentoRepository.totalRecebidoNaParcela(id));
        }
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE_PARCELA, parcela.getId(),
                    "Parcela " + parcela.getNumero() + " de '" + parcela.getAcordo().getAtendimento().getCliente().getNome()
                            + "' alterada: " + String.join(", ", alteracoes) + ".");
        }
        return parcela;
    }

    @Transactional
    public ParcelaHonorario cancelarParcela(Long id, String motivo) {
        ParcelaHonorario parcela = buscarParcela(id);
        String motivoValidado = validarMotivo(motivo);
        if (parcela.getSituacao() != SituacaoParcela.ABERTA) {
            throw new RegraNegocioException("Só parcelas abertas podem ser canceladas.");
        }
        if (recebimentoRepository.totalRecebidoNaParcela(id).signum() > 0) {
            throw new RegraNegocioException(
                    "Esta parcela tem recebimentos. Cancele os recebimentos antes de cancelar a parcela.");
        }
        parcela.cancelar(motivoValidado, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        auditoriaService.registrar(AcaoAuditoria.CANCELAMENTO, ENTIDADE_PARCELA, parcela.getId(),
                "Parcela " + parcela.getNumero() + " de '" + parcela.getAcordo().getAtendimento().getCliente().getNome()
                        + "' (" + Textos.moeda(parcela.getValor()) + ") cancelada. Motivo: " + motivoValidado);
        return parcela;
    }

    @Transactional
    public RecebimentoHonorario registrarRecebimento(Long acordoId, RecebimentoForm form) {
        AcordoHonorarios acordo = buscarAcordo(acordoId);
        if (form.getDataRecebimento() == null || form.getDataRecebimento().isAfter(hoje())) {
            throw new RegraNegocioException("dataRecebimento", "A data do recebimento não pode estar no futuro.");
        }
        if (form.getValor() == null || form.getValor().signum() <= 0) {
            throw new RegraNegocioException("valor", "O valor recebido deve ser maior que zero.");
        }
        if (acordo.getValorContratado() != null) {
            BigDecimal saldo = CalculoHonorarios.saldo(acordo.getValorContratado(),
                    recebimentoRepository.totalRecebidoNoAcordo(acordoId));
            if (form.getValor().compareTo(saldo) > 0) {
                throw new RegraNegocioException("valor", "O valor ultrapassa o saldo pendente de "
                        + Textos.moeda(saldo) + ". Se o acordo mudou, atualize o valor contratado antes.");
            }
        }

        ParcelaHonorario parcela = null;
        BigDecimal recebidoNaParcela = BigDecimal.ZERO;
        if (form.getParcelaId() != null) {
            parcela = parcelaRepository.findById(form.getParcelaId())
                    .filter(p -> Objects.equals(p.getAcordo().getId(), acordoId))
                    .orElseThrow(() -> new RegraNegocioException("parcelaId", "Parcela não encontrada neste acordo."));
            if (parcela.getSituacao() != SituacaoParcela.ABERTA) {
                throw new RegraNegocioException("parcelaId", "A parcela " + parcela.getNumero() + " está "
                        + parcela.getSituacao().getRotulo().toLowerCase(Locale.ROOT) + ".");
            }
            recebidoNaParcela = recebimentoRepository.totalRecebidoNaParcela(parcela.getId());
        }

        RecebimentoHonorario recebimento = new RecebimentoHonorario(acordo, parcela, form.getDataRecebimento(),
                form.getValor(), form.getFormaPagamento());
        recebimento.setObservacoes(Textos.opcional(form.getObservacoes()));
        recebimentoRepository.save(recebimento);
        if (parcela != null) {
            parcela.atualizarPagamento(recebidoNaParcela.add(form.getValor()));
        }

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE_RECEBIMENTO, recebimento.getId(),
                "Recebimento de " + Textos.moeda(recebimento.getValor()) + " (" + recebimento.getFormaPagamento().getRotulo()
                        + ") em " + Textos.data(recebimento.getDataRecebimento()) + " de '"
                        + acordo.getAtendimento().getCliente().getNome() + "'"
                        + (parcela == null ? "" : ", parcela " + parcela.getNumero()) + ".");
        return recebimento;
    }

    @Transactional
    public RecebimentoHonorario cancelarRecebimento(Long id, String motivo) {
        RecebimentoHonorario recebimento = recebimentoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Recebimento não encontrado."));
        String motivoValidado = validarMotivo(motivo);
        if (recebimento.isCancelado()) {
            throw new RegraNegocioException("Este recebimento já está cancelado.");
        }
        recebimento.cancelar(motivoValidado, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        ParcelaHonorario parcela = recebimento.getParcela();
        if (parcela != null) {
            BigDecimal restante = recebimentoRepository.totalRecebidoNaParcela(parcela.getId());
            parcela.atualizarPagamento(restante);
        }
        auditoriaService.registrar(AcaoAuditoria.CANCELAMENTO, ENTIDADE_RECEBIMENTO, recebimento.getId(),
                "Recebimento de " + Textos.moeda(recebimento.getValor()) + " em "
                        + Textos.data(recebimento.getDataRecebimento()) + " de '"
                        + recebimento.getAcordo().getAtendimento().getCliente().getNome()
                        + "' cancelado. Motivo: " + motivoValidado);
        return recebimento;
    }

    @Transactional(readOnly = true)
    public Long acordoDaParcela(Long parcelaId) {
        return buscarParcela(parcelaId).getAcordo().getId();
    }

    @Transactional(readOnly = true)
    public Long acordoDoRecebimento(Long recebimentoId) {
        return recebimentoRepository.findById(recebimentoId)
                .map(r -> r.getAcordo().getId())
                .orElseThrow(() -> new RegistroNaoEncontradoException("Recebimento não encontrado."));
    }

    private void validarCobranca(AcordoForm form) {
        if (form.getFormaCobranca() == FormaCobranca.VALOR_FIXO && form.getValorContratado() == null) {
            throw new RegraNegocioException("valorContratado", "Informe o valor contratado.");
        }
        if (form.getFormaCobranca() == FormaCobranca.PERCENTUAL) {
            if (form.getPercentual() == null) {
                throw new RegraNegocioException("percentual", "Informe o percentual acordado.");
            }
            if (Textos.opcional(form.getBaseCalculo()) == null) {
                throw new RegraNegocioException("baseCalculo",
                        "Descreva a base de cálculo do percentual (por exemplo, valores retroativos do benefício).");
            }
        }
        if (form.getValorContratado() != null && form.getValorContratado().signum() < 0) {
            throw new RegraNegocioException("valorContratado", "O valor contratado não pode ser negativo.");
        }
    }

    private static void aplicar(AcordoHonorarios acordo, AcordoForm form) {
        acordo.definirCobranca(form.getFormaCobranca(), form.getPercentual(), Textos.opcional(form.getBaseCalculo()));
        acordo.setValorContratado(form.getValorContratado());
        acordo.setQuantidadeParcelas(form.getQuantidadeParcelas());
        acordo.setCondicoes(Textos.opcional(form.getCondicoes()));
        acordo.setDataAcordo(form.getDataAcordo());
    }

    private static String validarMotivo(String motivo) {
        String limpo = Textos.opcional(motivo);
        if (limpo == null || limpo.length() < TAMANHO_MINIMO_MOTIVO) {
            throw new RegraNegocioException("motivo",
                    "Informe o motivo do cancelamento (pelo menos " + TAMANHO_MINIMO_MOTIVO + " caracteres).");
        }
        if (limpo.length() > TAMANHO_MAXIMO_MOTIVO) {
            throw new RegraNegocioException("motivo",
                    "O motivo pode ter no máximo " + TAMANHO_MAXIMO_MOTIVO + " caracteres.");
        }
        return limpo;
    }

    private static String descricaoCobranca(AcordoHonorarios acordo) {
        if (acordo.getFormaCobranca() != FormaCobranca.PERCENTUAL) {
            return acordo.getFormaCobranca().getRotulo();
        }
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.of("pt", "BR"));
        formato.setMaximumFractionDigits(2);
        return "Percentual de " + formato.format(acordo.getPercentual()) + "% sobre " + acordo.getBaseCalculo();
    }

    private static String valorContratado(BigDecimal valor) {
        return valor == null ? "a definir" : Textos.moeda(valor);
    }

    private static boolean mesmoValor(BigDecimal a, BigDecimal b) {
        return a == null ? b == null : b != null && a.compareTo(b) == 0;
    }
}
