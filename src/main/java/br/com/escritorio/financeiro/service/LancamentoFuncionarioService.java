package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import br.com.escritorio.financeiro.domain.TipoLancamentoFuncionario;
import br.com.escritorio.financeiro.dto.LancamentoFuncionarioForm;
import br.com.escritorio.financeiro.repository.LancamentoFuncionarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Vales, descontos, bonificações, horas extras e reembolsos dos funcionários. Um lançamento nunca é apagado:
 * quando feito por engano, é cancelado com motivo e sai do acerto.
 */
@Service
public class LancamentoFuncionarioService {

    public static final String ENTIDADE = "Lançamento de funcionário";

    private final LancamentoFuncionarioRepository lancamentoRepository;
    private final FuncionarioService funcionarioService;
    private final FechamentoFuncionarioService fechamentoService;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public LancamentoFuncionarioService(LancamentoFuncionarioRepository lancamentoRepository,
                                        FuncionarioService funcionarioService,
                                        FechamentoFuncionarioService fechamentoService,
                                        AuditoriaService auditoriaService, Clock relogio) {
        this.lancamentoRepository = lancamentoRepository;
        this.funcionarioService = funcionarioService;
        this.fechamentoService = fechamentoService;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public LancamentoFuncionario buscar(Long id) {
        return lancamentoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Lançamento não encontrado."));
    }

    /** Lançamentos da competência, inclusive os cancelados. */
    @Transactional(readOnly = true)
    public List<LancamentoFuncionario> daCompetencia(Long funcionarioId, YearMonth competencia) {
        return lancamentoRepository.daCompetencia(funcionarioId, competencia.atDay(1));
    }

    @Transactional(readOnly = true)
    public List<Auditoria> historico(Long id) {
        return auditoriaService.historico(ENTIDADE, id);
    }

    @Transactional
    public LancamentoFuncionario criar(Long funcionarioId, LancamentoFuncionarioForm form) {
        Funcionario funcionario = funcionarioService.buscarAtivo(funcionarioId);
        YearMonth competencia = validar(funcionario, form);
        fechamentoService.exigirAberto(funcionario, competencia, "competencia");

        LancamentoFuncionario lancamento = new LancamentoFuncionario(funcionario, form.getTipo(), form.getData(),
                competencia, form.getValor());
        lancamento.setFormaPagamento(formaPagamento(form));
        lancamento.setDescricao(Textos.opcional(form.getDescricao()));
        lancamento.setObservacoes(Textos.opcional(form.getObservacoes()));
        lancamentoRepository.save(lancamento);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, lancamento.getId(),
                descrever(lancamento) + " lançado para '" + funcionario.getNome() + "' em "
                        + Textos.data(lancamento.getData()) + " (competência " + Textos.mes(competencia) + ").");
        return lancamento;
    }

    @Transactional
    public LancamentoFuncionario atualizar(Long id, LancamentoFuncionarioForm form) {
        LancamentoFuncionario lancamento = buscar(id);
        exigirAlteravel(lancamento, "alterado");
        Funcionario funcionario = lancamento.getFuncionario();
        fechamentoService.exigirAberto(funcionario, lancamento.getCompetencia(), null);
        YearMonth competencia = validar(funcionario, form);
        fechamentoService.exigirAberto(funcionario, competencia, "competencia");

        List<String> alteracoes = new ArrayList<>();
        if (lancamento.getTipo() != form.getTipo()) {
            alteracoes.add("tipo de '" + lancamento.getTipo().getRotulo() + "' para '" + form.getTipo().getRotulo() + "'");
            lancamento.setTipo(form.getTipo());
        }
        if (!lancamento.getData().equals(form.getData())) {
            alteracoes.add("data de " + Textos.data(lancamento.getData()) + " para " + Textos.data(form.getData()));
            lancamento.setData(form.getData());
        }
        if (!lancamento.getCompetencia().equals(competencia)) {
            alteracoes.add("competência de " + Textos.mes(lancamento.getCompetencia()) + " para "
                    + Textos.mes(competencia));
            lancamento.setCompetencia(competencia);
        }
        if (lancamento.getValor().compareTo(form.getValor()) != 0) {
            alteracoes.add("valor de " + Textos.moeda(lancamento.getValor()) + " para " + Textos.moeda(form.getValor()));
            lancamento.setValor(form.getValor());
        }
        FormaPagamento forma = formaPagamento(form);
        if (lancamento.getFormaPagamento() != forma) {
            alteracoes.add("forma de pagamento");
            lancamento.setFormaPagamento(forma);
        }
        String descricao = Textos.opcional(form.getDescricao());
        if (!Objects.equals(lancamento.getDescricao(), descricao)) {
            alteracoes.add(lancamento.getTipo().isExigeMotivo() ? "motivo" : "descrição");
            lancamento.setDescricao(descricao);
        }
        String observacoes = Textos.opcional(form.getObservacoes());
        if (!Objects.equals(lancamento.getObservacoes(), observacoes)) {
            alteracoes.add("observações");
            lancamento.setObservacoes(observacoes);
        }
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, lancamento.getId(),
                    lancamento.getTipo().getRotulo() + " de '" + funcionario.getNome() + "' alterado: "
                            + String.join(", ", alteracoes) + ".");
        }
        return lancamento;
    }

    /** Cancela com motivo, sem apagar; o lançamento deixa de entrar no acerto. */
    @Transactional
    public LancamentoFuncionario cancelar(Long id, String motivo) {
        LancamentoFuncionario lancamento = buscar(id);
        String motivoValidado = MotivoCancelamento.validar(motivo);
        exigirAlteravel(lancamento, "cancelado de novo");
        fechamentoService.exigirAberto(lancamento.getFuncionario(), lancamento.getCompetencia(), null);
        lancamento.cancelar(motivoValidado, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        auditoriaService.registrar(AcaoAuditoria.CANCELAMENTO, ENTIDADE, lancamento.getId(),
                descrever(lancamento) + " de '" + lancamento.getFuncionario().getNome() + "' ("
                        + Textos.data(lancamento.getData()) + ") cancelado. Motivo: " + motivoValidado);
        return lancamento;
    }

    private YearMonth validar(Funcionario funcionario, LancamentoFuncionarioForm form) {
        TipoLancamentoFuncionario tipo = form.getTipo();
        if (form.getData().isAfter(hoje())) {
            throw new RegraNegocioException("data", "A data não pode estar no futuro.");
        }
        if (tipo.isExigeMotivo() && Textos.opcional(form.getDescricao()) == null) {
            throw new RegraNegocioException("descricao", "Informe o motivo do desconto.");
        }
        if (tipo.isPagoAoFuncionario() && form.getFormaPagamento() == null) {
            throw new RegraNegocioException("formaPagamento", "Selecione a forma de pagamento.");
        }
        YearMonth competencia = form.getCompetenciaEfetiva();
        if (!funcionario.trabalhouEm(competencia)) {
            throw new RegraNegocioException("competencia", funcionario.getNome() + " não trabalhava no escritório em "
                    + Textos.mes(competencia) + ". Confira a competência e as datas de admissão e desligamento.");
        }
        return competencia;
    }

    private static FormaPagamento formaPagamento(LancamentoFuncionarioForm form) {
        return form.getTipo().isPagoAoFuncionario() ? form.getFormaPagamento() : null;
    }

    private static void exigirAlteravel(LancamentoFuncionario lancamento, String acao) {
        if (lancamento.isCancelado()) {
            throw new RegraNegocioException("Este lançamento está cancelado e não pode ser " + acao + ".");
        }
    }

    private static String descrever(LancamentoFuncionario lancamento) {
        return lancamento.getTipo().getRotulo() + " de " + Textos.moeda(lancamento.getValor());
    }
}
