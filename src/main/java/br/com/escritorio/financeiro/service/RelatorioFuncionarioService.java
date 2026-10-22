package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import br.com.escritorio.financeiro.domain.TipoLancamentoFuncionario;
import br.com.escritorio.financeiro.dto.ExtratoFuncionario;
import br.com.escritorio.financeiro.dto.ResumoAcertosMes;
import br.com.escritorio.financeiro.dto.ValesFuncionario;
import br.com.escritorio.financeiro.repository.FechamentoFuncionarioRepository;
import br.com.escritorio.financeiro.repository.LancamentoFuncionarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Consultas do módulo de funcionários: extrato, vales do mês e total por mês. */
@Service
public class RelatorioFuncionarioService {

    public static final String ENTIDADE = "Relatório de funcionários";

    private final LancamentoFuncionarioRepository lancamentoRepository;
    private final FechamentoFuncionarioRepository fechamentoRepository;
    private final FechamentoFuncionarioService fechamentoService;
    private final AuditoriaService auditoriaService;

    public RelatorioFuncionarioService(LancamentoFuncionarioRepository lancamentoRepository,
                                       FechamentoFuncionarioRepository fechamentoRepository,
                                       FechamentoFuncionarioService fechamentoService,
                                       AuditoriaService auditoriaService) {
        this.lancamentoRepository = lancamentoRepository;
        this.fechamentoRepository = fechamentoRepository;
        this.fechamentoService = fechamentoService;
        this.auditoriaService = auditoriaService;
    }

    /** Lançamentos ativos e acertos pagos no período, de um funcionário ou de todos. */
    @Transactional(readOnly = true)
    public ExtratoFuncionario extrato(Long funcionarioId, LocalDate inicio, LocalDate fim) {
        return new ExtratoFuncionario(lancamentoRepository.ativosEntreDatas(funcionarioId, inicio, fim),
                fechamentoRepository.pagosEntreDatas(funcionarioId, inicio, fim));
    }

    @Transactional(readOnly = true)
    public List<ValesFuncionario> valesDoMes(YearMonth competencia) {
        Map<Funcionario, List<LancamentoFuncionario>> porFuncionario = lancamentoRepository
                .ativosEntreCompetencias(competencia.atDay(1), competencia.atDay(1)).stream()
                .filter(l -> l.getTipo() == TipoLancamentoFuncionario.VALE)
                .collect(Collectors.groupingBy(LancamentoFuncionario::getFuncionario, LinkedHashMap::new,
                        Collectors.toList()));
        return porFuncionario.entrySet().stream()
                .map(e -> new ValesFuncionario(e.getKey(), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResumoAcertosMes> totaisPorMes(YearMonth inicio, YearMonth fim) {
        return fechamentoService.resumoEntre(inicio, fim);
    }

    @Transactional
    public void registrarExportacao(String relatorio, String periodo, int linhas) {
        auditoriaService.registrar(AcaoAuditoria.EXPORTACAO, ENTIDADE, null,
                "Relatório '" + relatorio + "' de funcionários exportado em Excel (" + periodo + ", " + linhas
                        + (linhas == 1 ? " linha)." : " linhas)."));
    }
}
