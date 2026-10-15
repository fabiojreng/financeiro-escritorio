package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Despesa;
import br.com.escritorio.financeiro.domain.FechamentoDespesa;
import br.com.escritorio.financeiro.domain.SituacaoEnvioDespesas;
import br.com.escritorio.financeiro.dto.PendenciaEnvioDespesas;
import br.com.escritorio.financeiro.dto.ResumoDespesas;
import br.com.escritorio.financeiro.repository.DespesaRepository;
import br.com.escritorio.financeiro.repository.FechamentoDespesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Envio mensal da planilha de despesas à contabilidade (até o dia 20 do mês seguinte). Marcar o mês
 * como enviado trava as despesas dele; só um administrador reabre, com motivo, para correções.
 */
@Service
public class FechamentoDespesaService {

    public static final String ENTIDADE = "Envio de despesas";

    static final int TAMANHO_MINIMO_MOTIVO = 5;
    static final int TAMANHO_MAXIMO_MOTIVO = 500;

    private final FechamentoDespesaRepository fechamentoRepository;
    private final DespesaRepository despesaRepository;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public FechamentoDespesaService(FechamentoDespesaRepository fechamentoRepository,
                                    DespesaRepository despesaRepository, AuditoriaService auditoriaService,
                                    Clock relogio) {
        this.fechamentoRepository = fechamentoRepository;
        this.despesaRepository = despesaRepository;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public Optional<FechamentoDespesa> buscar(YearMonth competencia) {
        return fechamentoRepository.findByCompetencia(competencia.atDay(1));
    }

    @Transactional(readOnly = true)
    public boolean isEnviado(YearMonth competencia) {
        return buscar(competencia).map(FechamentoDespesa::isEnviado).orElse(false);
    }

    @Transactional(readOnly = true)
    public SituacaoEnvioDespesas situacao(YearMonth competencia) {
        return SituacaoEnvioDespesas.de(competencia, isEnviado(competencia), hoje());
    }

    /** Impede lançar, alterar ou cancelar despesas de um mês já enviado à contabilidade. */
    @Transactional(readOnly = true)
    public void exigirAberto(YearMonth competencia, String campo) {
        if (isEnviado(competencia)) {
            throw new RegraNegocioException(campo, "As despesas de " + Textos.mes(competencia)
                    + " já foram enviadas à contabilidade. Peça a um administrador para reabrir o mês antes de alterar.");
        }
    }

    @Transactional
    public FechamentoDespesa marcarEnviado(YearMonth competencia) {
        if (!competencia.isBefore(YearMonth.from(hoje()))) {
            throw new RegraNegocioException("O mês de " + Textos.mes(competencia)
                    + " ainda não terminou. A planilha só pode ser marcada como enviada a partir do mês seguinte.");
        }
        FechamentoDespesa fechamento = buscar(competencia).orElseGet(() -> new FechamentoDespesa(competencia));
        if (fechamento.isEnviado()) {
            throw new RegraNegocioException("A planilha de " + Textos.mes(competencia) + " já está marcada como enviada.");
        }
        List<Despesa> despesas = despesaRepository.ativasEntre(competencia.atDay(1), competencia.atEndOfMonth());
        ResumoDespesas resumo = ResumoDespesas.de(despesas);
        fechamento.marcarEnviado(auditoriaService.usuarioAtual(), LocalDateTime.now(relogio), resumo.quantidade(),
                resumo.total());
        fechamentoRepository.save(fechamento);
        auditoriaService.registrar(AcaoAuditoria.CONFIRMACAO, ENTIDADE, fechamento.getId(),
                "Planilha de despesas de " + Textos.mes(competencia) + " marcada como enviada à contabilidade ("
                        + resumo.quantidade() + (resumo.quantidade() == 1 ? " despesa, " : " despesas, ")
                        + Textos.moeda(resumo.total()) + ").");
        return fechamento;
    }

    @Transactional
    public FechamentoDespesa reabrir(YearMonth competencia, String motivo, boolean administrador) {
        if (!administrador) {
            throw new RegraNegocioException("Só um administrador pode reabrir um mês já enviado à contabilidade.");
        }
        String motivoValidado = validarMotivo(motivo);
        FechamentoDespesa fechamento = buscar(competencia)
                .filter(FechamentoDespesa::isEnviado)
                .orElseThrow(() -> new RegraNegocioException(
                        "A planilha de " + Textos.mes(competencia) + " não está marcada como enviada."));
        fechamento.reabrir(motivoValidado, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, fechamento.getId(),
                "Despesas de " + Textos.mes(competencia) + " reabertas para correção. Motivo: " + motivoValidado);
        return fechamento;
    }

    /**
     * Meses que precisam de atenção no painel: o mês anterior, a partir do dia {@value SituacaoEnvioDespesas#DIA_AVISO},
     * e qualquer mês mais antigo com despesas lançadas que ainda não foi enviado.
     */
    @Transactional(readOnly = true)
    public List<PendenciaEnvioDespesas> pendencias() {
        LocalDate hoje = hoje();
        YearMonth anterior = YearMonth.from(hoje).minusMonths(1);
        Set<YearMonth> enviados = fechamentoRepository.competenciasEnviadas().stream()
                .map(YearMonth::from)
                .collect(Collectors.toSet());
        Set<YearMonth> meses = despesaRepository.datasAtivasAntesDe(anterior.atDay(1)).stream()
                .map(YearMonth::from)
                .collect(Collectors.toCollection(TreeSet::new));
        meses.add(anterior);
        meses.removeAll(enviados);
        return meses.stream()
                .map(mes -> new PendenciaEnvioDespesas(mes, Textos.mes(mes), SituacaoEnvioDespesas.prazo(mes),
                        SituacaoEnvioDespesas.de(mes, false, hoje)))
                .filter(p -> p.situacao().isAvisar())
                .toList();
    }

    /** Fica no histórico de operações, como as exportações em CSV. */
    @Transactional
    public void registrarExportacao(YearMonth competencia, int linhas) {
        auditoriaService.registrar(AcaoAuditoria.EXPORTACAO, ENTIDADE, null,
                "Planilha de despesas de " + Textos.mes(competencia) + " baixada para a contabilidade ("
                        + linhas + (linhas == 1 ? " linha)." : " linhas)."));
    }

    private static String validarMotivo(String motivo) {
        String limpo = Textos.opcional(motivo);
        if (limpo == null || limpo.length() < TAMANHO_MINIMO_MOTIVO) {
            throw new RegraNegocioException("motivo",
                    "Informe o motivo da reabertura (pelo menos " + TAMANHO_MINIMO_MOTIVO + " caracteres).");
        }
        if (limpo.length() > TAMANHO_MAXIMO_MOTIVO) {
            throw new RegraNegocioException("motivo",
                    "O motivo pode ter no máximo " + TAMANHO_MAXIMO_MOTIVO + " caracteres.");
        }
        return limpo;
    }
}
