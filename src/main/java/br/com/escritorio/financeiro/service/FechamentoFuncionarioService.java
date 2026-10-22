package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.AcertoMensal;
import br.com.escritorio.financeiro.domain.CalculoAcerto;
import br.com.escritorio.financeiro.domain.FechamentoFuncionario;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import br.com.escritorio.financeiro.dto.AcertoFuncionario;
import br.com.escritorio.financeiro.dto.ResumoAcertosMes;
import br.com.escritorio.financeiro.repository.FechamentoFuncionarioRepository;
import br.com.escritorio.financeiro.repository.LancamentoFuncionarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Acerto mensal com cada funcionário. Marcar o acerto como pago fecha a competência do funcionário; só quem
 * tem a permissão de reabrir reabre, com motivo, para correções.
 */
@Service
public class FechamentoFuncionarioService {

    public static final String ENTIDADE = "Acerto de funcionário";

    static final int TAMANHO_MINIMO_MOTIVO = 5;
    static final int TAMANHO_MAXIMO_MOTIVO = 500;
    static final int MAXIMO_MESES_RELATORIO = 36;

    private final FechamentoFuncionarioRepository fechamentoRepository;
    private final LancamentoFuncionarioRepository lancamentoRepository;
    private final FuncionarioService funcionarioService;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public FechamentoFuncionarioService(FechamentoFuncionarioRepository fechamentoRepository,
                                        LancamentoFuncionarioRepository lancamentoRepository,
                                        FuncionarioService funcionarioService, AuditoriaService auditoriaService,
                                        Clock relogio) {
        this.fechamentoRepository = fechamentoRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.funcionarioService = funcionarioService;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public Optional<FechamentoFuncionario> buscar(Long funcionarioId, YearMonth competencia) {
        return fechamentoRepository.findByFuncionarioIdAndCompetencia(funcionarioId, competencia.atDay(1));
    }

    @Transactional(readOnly = true)
    public boolean isPago(Long funcionarioId, YearMonth competencia) {
        return buscar(funcionarioId, competencia).map(FechamentoFuncionario::isPago).orElse(false);
    }

    @Transactional(readOnly = true)
    public Set<YearMonth> competenciasPagas(Long funcionarioId) {
        return fechamentoRepository.competenciasPagas(funcionarioId).stream()
                .map(YearMonth::from)
                .collect(Collectors.toSet());
    }

    /** Impede lançar, alterar ou cancelar movimentações de uma competência com o acerto já pago. */
    @Transactional(readOnly = true)
    public void exigirAberto(Funcionario funcionario, YearMonth competencia, String campo) {
        if (isPago(funcionario.getId(), competencia)) {
            throw new RegraNegocioException(campo, "O acerto de " + funcionario.getNome() + " de "
                    + Textos.mes(competencia) + " já foi pago. Peça a quem pode reabrir o acerto para reabrir o mês "
                    + "antes de alterar.");
        }
    }

    @Transactional(readOnly = true)
    public AcertoFuncionario acerto(Funcionario funcionario, YearMonth competencia) {
        FechamentoFuncionario fechamento = buscar(funcionario.getId(), competencia).orElse(null);
        if (fechamento != null && fechamento.isPago()) {
            return new AcertoFuncionario(funcionario, competencia, fechamento.getAcerto(), fechamento);
        }
        List<LancamentoFuncionario> lancamentos =
                lancamentoRepository.daCompetencia(funcionario.getId(), competencia.atDay(1));
        return new AcertoFuncionario(funcionario, competencia, calcular(funcionario, competencia, lancamentos),
                fechamento);
    }

    @Transactional(readOnly = true)
    public ResumoAcertosMes resumoDoMes(YearMonth competencia) {
        return resumoEntre(competencia, competencia).get(0);
    }

    /**
     * Acertos de cada mês do intervalo. Entra no mês quem trabalhou nele ou teve algum lançamento ou acerto.
     */
    @Transactional(readOnly = true)
    public List<ResumoAcertosMes> resumoEntre(YearMonth inicio, YearMonth fim) {
        if (fim.isBefore(inicio)) {
            return resumoEntre(fim, inicio);
        }
        if (inicio.plusMonths(MAXIMO_MESES_RELATORIO).isBefore(fim.plusMonths(1))) {
            throw new RegraNegocioException("Escolha um período de no máximo " + MAXIMO_MESES_RELATORIO + " meses.");
        }
        List<LancamentoFuncionario> lancamentos =
                lancamentoRepository.ativosEntreCompetencias(inicio.atDay(1), fim.atDay(1));
        Map<YearMonth, Map<Long, List<LancamentoFuncionario>>> lancamentosPorMes = lancamentos.stream()
                .collect(Collectors.groupingBy(LancamentoFuncionario::getCompetencia,
                        Collectors.groupingBy(l -> l.getFuncionario().getId())));
        Map<YearMonth, Map<Long, FechamentoFuncionario>> fechamentosPorMes =
                fechamentoRepository.entreCompetencias(inicio.atDay(1), fim.atDay(1)).stream()
                        .collect(Collectors.groupingBy(FechamentoFuncionario::getCompetencia,
                                Collectors.toMap(f -> f.getFuncionario().getId(), Function.identity())));
        List<Funcionario> funcionarios = funcionarioService.listarPorNome();

        List<ResumoAcertosMes> resumos = new ArrayList<>();
        for (YearMonth mes = inicio; !mes.isAfter(fim); mes = mes.plusMonths(1)) {
            Map<Long, List<LancamentoFuncionario>> doMes = lancamentosPorMes.getOrDefault(mes, Map.of());
            Map<Long, FechamentoFuncionario> fechamentos = fechamentosPorMes.getOrDefault(mes, Map.of());
            List<AcertoFuncionario> acertos = new ArrayList<>();
            for (Funcionario funcionario : funcionarios) {
                Long id = funcionario.getId();
                if (!funcionario.trabalhouEm(mes) && !doMes.containsKey(id) && !fechamentos.containsKey(id)) {
                    continue;
                }
                FechamentoFuncionario fechamento = fechamentos.get(id);
                AcertoMensal acerto = fechamento != null && fechamento.isPago()
                        ? fechamento.getAcerto()
                        : calcular(funcionario, mes, doMes.getOrDefault(id, List.of()));
                acertos.add(new AcertoFuncionario(funcionario, mes, acerto, fechamento));
            }
            resumos.add(new ResumoAcertosMes(mes, acertos));
        }
        return resumos;
    }

    @Transactional
    public FechamentoFuncionario marcarPago(Long funcionarioId, YearMonth competencia, LocalDate dataPagamento,
                                            FormaPagamento formaPagamento) {
        Funcionario funcionario = funcionarioService.buscar(funcionarioId);
        if (competencia.isAfter(YearMonth.from(hoje()))) {
            throw new RegraNegocioException("O acerto de " + Textos.mes(competencia)
                    + " ainda não pode ser pago: o mês não começou.");
        }
        if (dataPagamento == null) {
            throw new RegraNegocioException("dataPagamento", "Informe a data do pagamento.");
        }
        if (dataPagamento.isAfter(hoje())) {
            throw new RegraNegocioException("dataPagamento",
                    "A data do pagamento não pode estar no futuro. Marque como pago depois de pagar.");
        }
        if (formaPagamento == null) {
            throw new RegraNegocioException("formaPagamento", "Selecione a forma de pagamento.");
        }
        FechamentoFuncionario fechamento = buscar(funcionarioId, competencia)
                .orElseGet(() -> new FechamentoFuncionario(funcionario, competencia));
        if (fechamento.isPago()) {
            throw new RegraNegocioException("O acerto de " + funcionario.getNome() + " de "
                    + Textos.mes(competencia) + " já está pago.");
        }
        List<LancamentoFuncionario> lancamentos = lancamentoRepository.daCompetencia(funcionarioId,
                competencia.atDay(1));
        if (!funcionario.trabalhouEm(competencia) && lancamentos.stream().noneMatch(LancamentoFuncionario::isAtivo)) {
            throw new RegraNegocioException(funcionario.getNome() + " não trabalhou em " + Textos.mes(competencia)
                    + " e não tem lançamentos no mês: não há acerto a pagar.");
        }
        AcertoMensal acerto = calcular(funcionario, competencia, lancamentos);
        fechamento.marcarPago(acerto, dataPagamento, formaPagamento, auditoriaService.usuarioAtual(),
                LocalDateTime.now(relogio));
        fechamentoRepository.save(fechamento);
        auditoriaService.registrar(AcaoAuditoria.CONFIRMACAO, ENTIDADE, fechamento.getId(),
                "Acerto de '" + funcionario.getNome() + "' de " + Textos.mes(competencia) + " pago em "
                        + Textos.data(dataPagamento) + " (" + formaPagamento.getRotulo() + "): líquido de "
                        + Textos.moeda(acerto.liquido()) + ".");
        return fechamento;
    }

    @Transactional
    public FechamentoFuncionario reabrir(Long funcionarioId, YearMonth competencia, String motivo,
                                         boolean podeReabrir) {
        if (!podeReabrir) {
            throw new RegraNegocioException("Seu perfil não tem permissão para reabrir um acerto já pago.");
        }
        String motivoValidado = validarMotivo(motivo);
        Funcionario funcionario = funcionarioService.buscar(funcionarioId);
        FechamentoFuncionario fechamento = buscar(funcionarioId, competencia)
                .filter(FechamentoFuncionario::isPago)
                .orElseThrow(() -> new RegraNegocioException("O acerto de " + funcionario.getNome() + " de "
                        + Textos.mes(competencia) + " não está pago."));
        fechamento.reabrir(motivoValidado, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, fechamento.getId(),
                "Acerto de '" + funcionario.getNome() + "' de " + Textos.mes(competencia)
                        + " reaberto para correção. Motivo: " + motivoValidado);
        return fechamento;
    }

    private static AcertoMensal calcular(Funcionario funcionario, YearMonth competencia,
                                         List<LancamentoFuncionario> lancamentos) {
        BigDecimal salario = funcionario.trabalhouEm(competencia) ? funcionario.getSalarioBase() : BigDecimal.ZERO;
        return CalculoAcerto.calcular(salario, lancamentos);
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
