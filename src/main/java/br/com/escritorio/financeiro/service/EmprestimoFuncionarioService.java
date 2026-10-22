package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.CalculoHonorarios;
import br.com.escritorio.financeiro.domain.EmprestimoFuncionario;
import br.com.escritorio.financeiro.domain.FechamentoFuncionario;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import br.com.escritorio.financeiro.dto.EmprestimoFuncionarioForm;
import br.com.escritorio.financeiro.dto.SaldoEmprestimo;
import br.com.escritorio.financeiro.repository.EmprestimoFuncionarioRepository;
import br.com.escritorio.financeiro.repository.FechamentoFuncionarioRepository;
import br.com.escritorio.financeiro.repository.LancamentoFuncionarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Empréstimos do escritório aos funcionários. Cada parcela é um lançamento que reduz o líquido do acerto da
 * sua competência; o saldo devedor são as parcelas de competências cujo acerto ainda não foi pago.
 */
@Service
public class EmprestimoFuncionarioService {

    public static final String ENTIDADE = "Empréstimo a funcionário";

    private static final String MOTIVO_QUITACAO = "Quitação antecipada do empréstimo.";

    private final EmprestimoFuncionarioRepository emprestimoRepository;
    private final LancamentoFuncionarioRepository lancamentoRepository;
    private final FechamentoFuncionarioRepository fechamentoRepository;
    private final FuncionarioService funcionarioService;
    private final FechamentoFuncionarioService fechamentoService;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public EmprestimoFuncionarioService(EmprestimoFuncionarioRepository emprestimoRepository,
                                        LancamentoFuncionarioRepository lancamentoRepository,
                                        FechamentoFuncionarioRepository fechamentoRepository,
                                        FuncionarioService funcionarioService,
                                        FechamentoFuncionarioService fechamentoService,
                                        AuditoriaService auditoriaService, Clock relogio) {
        this.emprestimoRepository = emprestimoRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.fechamentoRepository = fechamentoRepository;
        this.funcionarioService = funcionarioService;
        this.fechamentoService = fechamentoService;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public EmprestimoFuncionario buscar(Long id) {
        return emprestimoRepository.findComFuncionarioById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Empréstimo não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<LancamentoFuncionario> parcelas(Long emprestimoId) {
        return lancamentoRepository.doEmprestimo(emprestimoId);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> historico(Long id) {
        return auditoriaService.historico(ENTIDADE, id);
    }

    @Transactional(readOnly = true)
    public SaldoEmprestimo saldo(EmprestimoFuncionario emprestimo) {
        Set<YearMonth> pagas = fechamentoService.competenciasPagas(emprestimo.getFuncionario().getId());
        return saldo(emprestimo, parcelas(emprestimo.getId()), pagas);
    }

    @Transactional(readOnly = true)
    public List<SaldoEmprestimo> doFuncionario(Long funcionarioId) {
        Set<YearMonth> pagas = fechamentoService.competenciasPagas(funcionarioId);
        Map<Long, List<LancamentoFuncionario>> parcelas = lancamentoRepository
                .parcelasAtivasDoFuncionario(funcionarioId).stream()
                .collect(Collectors.groupingBy(l -> l.getEmprestimo().getId()));
        return emprestimoRepository.findByFuncionarioIdOrderByDataConcessaoDescIdDesc(funcionarioId).stream()
                .map(e -> saldo(e, parcelas.getOrDefault(e.getId(), List.of()), pagas))
                .toList();
    }

    /**
     * Empréstimos com saldo devedor, inclusive os quitados antecipadamente cuja parcela de quitação ainda não
     * foi descontada.
     */
    @Transactional(readOnly = true)
    public List<SaldoEmprestimo> comSaldo() {
        Map<Long, Set<YearMonth>> pagasPorFuncionario = fechamentoRepository.pagos().stream()
                .collect(Collectors.groupingBy(f -> f.getFuncionario().getId(),
                        Collectors.mapping(FechamentoFuncionario::getCompetencia, Collectors.toSet())));
        Map<Long, List<LancamentoFuncionario>> parcelas = lancamentoRepository
                .parcelasAtivasDeEmprestimosNaoCancelados().stream()
                .collect(Collectors.groupingBy(l -> l.getEmprestimo().getId()));
        return emprestimoRepository.naoCancelados().stream()
                .map(e -> saldo(e, parcelas.getOrDefault(e.getId(), List.of()),
                        pagasPorFuncionario.getOrDefault(e.getFuncionario().getId(), Set.of())))
                .filter(s -> s.saldo().signum() > 0)
                .toList();
    }

    @Transactional
    public EmprestimoFuncionario criar(Long funcionarioId, EmprestimoFuncionarioForm form) {
        Funcionario funcionario = funcionarioService.buscarAtivo(funcionarioId);
        if (form.getDataConcessao().isAfter(hoje())) {
            throw new RegraNegocioException("dataConcessao",
                    "A data do empréstimo não pode estar no futuro. Lance depois de entregar o dinheiro.");
        }
        YearMonth primeira = form.getPrimeiraCompetencia();
        if (primeira.isBefore(YearMonth.from(form.getDataConcessao()))) {
            throw new RegraNegocioException("primeiraCompetencia",
                    "A primeira parcela não pode ser antes do mês do empréstimo.");
        }
        int quantidade = form.getNumeroParcelas();
        YearMonth ultima = primeira.plusMonths(quantidade - 1L);
        if (!funcionario.trabalhouEm(primeira) || !funcionario.trabalhouEm(ultima)) {
            throw new RegraNegocioException("numeroParcelas", "As parcelas vão de " + Textos.mes(primeira) + " a "
                    + Textos.mes(ultima) + ", fora do período em que " + funcionario.getNome()
                    + " trabalha no escritório. Confira as parcelas e a data de desligamento.");
        }
        Set<YearMonth> pagas = fechamentoService.competenciasPagas(funcionarioId);
        for (int i = 0; i < quantidade; i++) {
            if (pagas.contains(primeira.plusMonths(i))) {
                fechamentoService.exigirAberto(funcionario, primeira.plusMonths(i), "primeiraCompetencia");
            }
        }

        EmprestimoFuncionario emprestimo = new EmprestimoFuncionario(funcionario, form.getDataConcessao(),
                form.getValorTotal(), quantidade, primeira, form.getFormaPagamento());
        emprestimo.setObservacoes(Textos.opcional(form.getObservacoes()));
        emprestimoRepository.save(emprestimo);

        List<BigDecimal> valores = CalculoHonorarios.dividir(form.getValorTotal(), quantidade);
        for (int i = 0; i < quantidade; i++) {
            int numero = i + 1;
            lancamentoRepository.save(LancamentoFuncionario.parcela(emprestimo, numero, primeira.plusMonths(i),
                    valores.get(i), "Parcela " + numero + "/" + quantidade + " do empréstimo"));
        }

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, emprestimo.getId(),
                "Empréstimo de " + Textos.moeda(emprestimo.getValorTotal()) + " a '" + funcionario.getNome()
                        + "' em " + quantidade + (quantidade == 1 ? " parcela" : " parcelas") + ", de "
                        + Textos.mes(primeira) + " a " + Textos.mes(ultima) + " ("
                        + emprestimo.getFormaPagamento().getRotulo() + ").");
        return emprestimo;
    }

    /**
     * Junta todas as parcelas ainda não descontadas numa só, descontada no acerto da competência escolhida.
     */
    @Transactional
    public EmprestimoFuncionario quitar(Long id, YearMonth competencia) {
        EmprestimoFuncionario emprestimo = buscar(id);
        exigirEmAberto(emprestimo);
        if (competencia == null) {
            throw new RegraNegocioException("competencia", "Informe o mês em que o saldo será descontado.");
        }
        Funcionario funcionario = emprestimo.getFuncionario();
        fechamentoService.exigirAberto(funcionario, competencia, "competencia");
        Set<YearMonth> pagas = fechamentoService.competenciasPagas(funcionario.getId());
        List<LancamentoFuncionario> abertas = parcelas(id).stream()
                .filter(LancamentoFuncionario::isAtivo)
                .filter(p -> !pagas.contains(p.getCompetencia()))
                .toList();
        BigDecimal saldo = abertas.stream()
                .map(LancamentoFuncionario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String usuario = auditoriaService.usuarioAtual();
        LocalDateTime agora = LocalDateTime.now(relogio);
        for (LancamentoFuncionario parcela : abertas) {
            parcela.cancelar(MOTIVO_QUITACAO, usuario, agora);
        }
        if (saldo.signum() > 0) {
            lancamentoRepository.save(LancamentoFuncionario.parcela(emprestimo, null, competencia, saldo,
                    "Quitação antecipada do empréstimo"));
        }
        emprestimo.quitar(usuario, agora);
        auditoriaService.registrar(AcaoAuditoria.CONFIRMACAO, ENTIDADE, emprestimo.getId(),
                "Empréstimo de " + Textos.moeda(emprestimo.getValorTotal()) + " a '" + funcionario.getNome()
                        + "' quitado antecipadamente: saldo de " + Textos.moeda(saldo) + " descontado em "
                        + Textos.mes(competencia) + ".");
        return emprestimo;
    }

    /** Cancela o empréstimo lançado por engano; só enquanto nenhuma parcela foi descontada num acerto pago. */
    @Transactional
    public EmprestimoFuncionario cancelar(Long id, String motivo) {
        EmprestimoFuncionario emprestimo = buscar(id);
        String motivoValidado = MotivoCancelamento.validar(motivo);
        exigirEmAberto(emprestimo);
        Funcionario funcionario = emprestimo.getFuncionario();
        Set<YearMonth> pagas = fechamentoService.competenciasPagas(funcionario.getId());
        List<LancamentoFuncionario> ativas = parcelas(id).stream().filter(LancamentoFuncionario::isAtivo).toList();
        if (ativas.stream().anyMatch(p -> pagas.contains(p.getCompetencia()))) {
            throw new RegraNegocioException("Há parcelas deste empréstimo descontadas em acertos já pagos. "
                    + "Para encerrar o empréstimo, use a quitação antecipada.");
        }
        String usuario = auditoriaService.usuarioAtual();
        LocalDateTime agora = LocalDateTime.now(relogio);
        for (LancamentoFuncionario parcela : ativas) {
            parcela.cancelar(motivoValidado, usuario, agora);
        }
        emprestimo.cancelar(motivoValidado, usuario, agora);
        auditoriaService.registrar(AcaoAuditoria.CANCELAMENTO, ENTIDADE, emprestimo.getId(),
                "Empréstimo de " + Textos.moeda(emprestimo.getValorTotal()) + " a '" + funcionario.getNome()
                        + "' cancelado. Motivo: " + motivoValidado);
        return emprestimo;
    }

    private static SaldoEmprestimo saldo(EmprestimoFuncionario emprestimo, List<LancamentoFuncionario> parcelas,
                                         Set<YearMonth> competenciasPagas) {
        List<LancamentoFuncionario> abertas = parcelas.stream()
                .filter(LancamentoFuncionario::isAtivo)
                .filter(p -> !competenciasPagas.contains(p.getCompetencia()))
                .toList();
        BigDecimal saldo = abertas.stream()
                .map(LancamentoFuncionario::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SaldoEmprestimo(emprestimo, saldo, abertas.size());
    }

    private static void exigirEmAberto(EmprestimoFuncionario emprestimo) {
        if (!emprestimo.isEmAberto()) {
            throw new RegraNegocioException("Este empréstimo está " + emprestimo.getSituacao().getRotulo().toLowerCase()
                    + " e não pode ser alterado.");
        }
    }
}
