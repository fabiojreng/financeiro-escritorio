package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import br.com.escritorio.financeiro.domain.SituacaoRepasse;
import br.com.escritorio.financeiro.dto.PagamentoRepasseForm;
import br.com.escritorio.financeiro.dto.RepasseForm;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.RepasseRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Lançamento manual de comissões e repasses internos. O service guarda exatamente os valores
 * informados e não calcula nada sozinho; os totais de repasse não se misturam com honorários
 * nem com o benefício do INSS.
 */
@Service
public class RepasseService {

    public static final String ENTIDADE = "Repasse";

    private final RepasseRepository repasseRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final ResponsavelRepository responsavelRepository;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public RepasseService(RepasseRepository repasseRepository, AtendimentoRepository atendimentoRepository,
                          ResponsavelRepository responsavelRepository, AuditoriaService auditoriaService,
                          Clock relogio) {
        this.repasseRepository = repasseRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.responsavelRepository = responsavelRepository;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public Repasse buscar(Long id) {
        return repasseRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Repasse não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Repasse> listarPorAtendimento(Long atendimentoId) {
        return repasseRepository.findByAtendimentoIdOrderByDataPrevistaAscIdAsc(atendimentoId);
    }

    @Transactional(readOnly = true)
    public List<Repasse> pesquisar(String buscaCliente, Long destinatarioId, SituacaoRepasse situacao) {
        return repasseRepository.pesquisar(Textos.padraoBusca(buscaCliente), destinatarioId, situacao);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> historico(Long id) {
        return auditoriaService.historico(ENTIDADE, id);
    }

    @Transactional
    public Repasse criar(Long atendimentoId, RepasseForm form) {
        Atendimento atendimento = atendimentoRepository.findComDetalhesById(atendimentoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Atendimento não encontrado."));
        if (atendimento.getSituacao() == SituacaoAtendimento.CANCELADO) {
            throw new RegraNegocioException(
                    "Este atendimento está cancelado. Altere a situação dele antes de lançar repasses.");
        }
        Responsavel destinatario = destinatarioAtivo(form.getDestinatarioId());

        Repasse repasse = new Repasse(atendimento, destinatario, form.getDescricao().trim(), form.getValorPrevisto(),
                form.getDataPrevista());
        repasse.setObservacoes(Textos.opcional(form.getObservacoes()));
        repasseRepository.save(repasse);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, repasse.getId(),
                "Repasse de " + Textos.moeda(repasse.getValorPrevisto()) + " para '" + destinatario.getNome()
                        + "' lançado no atendimento de '" + atendimento.getCliente().getNome() + "' ("
                        + atendimento.getTipoServico().getNome() + "), previsto para "
                        + Textos.data(repasse.getDataPrevista()) + ". Motivo: " + repasse.getDescricao());
        return repasse;
    }

    @Transactional
    public Repasse atualizar(Long id, RepasseForm form) {
        Repasse repasse = buscar(id);
        if (!repasse.isPendente()) {
            throw new RegraNegocioException("Só repasses pendentes podem ser alterados.");
        }
        List<String> alteracoes = new ArrayList<>();
        if (!repasse.getDestinatario().getId().equals(form.getDestinatarioId())) {
            Responsavel novo = destinatarioAtivo(form.getDestinatarioId());
            alteracoes.add("destinatário de '" + repasse.getDestinatario().getNome() + "' para '" + novo.getNome() + "'");
            repasse.setDestinatario(novo);
        }
        String descricao = form.getDescricao().trim();
        if (!repasse.getDescricao().equals(descricao)) {
            alteracoes.add("motivo");
            repasse.setDescricao(descricao);
        }
        if (repasse.getValorPrevisto().compareTo(form.getValorPrevisto()) != 0) {
            alteracoes.add("valor previsto de " + Textos.moeda(repasse.getValorPrevisto()) + " para "
                    + Textos.moeda(form.getValorPrevisto()));
            repasse.setValorPrevisto(form.getValorPrevisto());
        }
        if (!repasse.getDataPrevista().equals(form.getDataPrevista())) {
            alteracoes.add("data prevista de " + Textos.data(repasse.getDataPrevista()) + " para "
                    + Textos.data(form.getDataPrevista()));
            repasse.setDataPrevista(form.getDataPrevista());
        }
        String observacoes = Textos.opcional(form.getObservacoes());
        if (!Objects.equals(repasse.getObservacoes(), observacoes)) {
            alteracoes.add("observações");
            repasse.setObservacoes(observacoes);
        }
        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, repasse.getId(),
                    "Repasse para '" + repasse.getDestinatario().getNome() + "' ("
                            + repasse.getAtendimento().getCliente().getNome() + ") alterado: "
                            + String.join(", ", alteracoes) + ".");
        }
        return repasse;
    }

    @Transactional
    public Repasse registrarPagamento(Long id, PagamentoRepasseForm form) {
        Repasse repasse = buscar(id);
        if (!repasse.isPendente()) {
            throw new RegraNegocioException("Este repasse está " + repasse.getSituacao().getRotulo().toLowerCase(Locale.ROOT)
                    + " e não pode receber pagamento.");
        }
        if (form.getDataPagamento() == null || form.getDataPagamento().isAfter(hoje())) {
            throw new RegraNegocioException("dataPagamento", "A data do pagamento não pode estar no futuro.");
        }
        if (form.getValorPago() == null || form.getValorPago().signum() <= 0) {
            throw new RegraNegocioException("valorPago", "O valor pago deve ser maior que zero.");
        }
        repasse.registrarPagamento(form.getDataPagamento(), form.getValorPago());
        auditoriaService.registrar(AcaoAuditoria.CONFIRMACAO, ENTIDADE, repasse.getId(),
                "Pagamento de " + Textos.moeda(repasse.getValorPago()) + " em " + Textos.data(repasse.getDataPagamento())
                        + " do repasse para '" + repasse.getDestinatario().getNome() + "' ("
                        + repasse.getAtendimento().getCliente().getNome() + "), previsto em "
                        + Textos.moeda(repasse.getValorPrevisto()) + ".");
        return repasse;
    }

    /**
     * Cancela com motivo, sem apagar. Repasse já pago é lançamento confirmado: só o administrador cancela.
     */
    @Transactional
    public Repasse cancelar(Long id, String motivo, boolean administrador) {
        Repasse repasse = buscar(id);
        String motivoValidado = MotivoCancelamento.validar(motivo);
        if (repasse.isCancelado()) {
            throw new RegraNegocioException("Este repasse já está cancelado.");
        }
        if (repasse.isPago() && !administrador) {
            throw new RegraNegocioException("Repasse já pago só pode ser cancelado por um administrador.");
        }
        boolean estavaPago = repasse.isPago();
        repasse.cancelar(motivoValidado, auditoriaService.usuarioAtual(), LocalDateTime.now(relogio));
        auditoriaService.registrar(AcaoAuditoria.CANCELAMENTO, ENTIDADE, repasse.getId(),
                "Repasse " + (estavaPago ? "pago" : "pendente") + " de "
                        + Textos.moeda(estavaPago ? repasse.getValorPago() : repasse.getValorPrevisto())
                        + " para '" + repasse.getDestinatario().getNome() + "' ("
                        + repasse.getAtendimento().getCliente().getNome() + ") cancelado. Motivo: " + motivoValidado);
        return repasse;
    }

    private Responsavel destinatarioAtivo(Long id) {
        Responsavel responsavel = responsavelRepository.findById(id)
                .orElseThrow(() -> new RegraNegocioException("destinatarioId", "Destinatário não encontrado."));
        if (!responsavel.isAtivo()) {
            throw new RegraNegocioException("destinatarioId",
                    "O responsável '" + responsavel.getNome() + "' está inativo e não pode receber novos repasses.");
        }
        return responsavel;
    }
}
