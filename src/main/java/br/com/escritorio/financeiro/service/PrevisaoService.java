package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.dto.AgendaMensal;
import br.com.escritorio.financeiro.dto.ConfirmacaoLiberacaoForm;
import br.com.escritorio.financeiro.dto.FiltroAgenda;
import br.com.escritorio.financeiro.dto.PrevisaoForm;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.PrevisaoBeneficioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Regras das previsões de benefício. A passagem da data nunca altera a situação: a liberação só
 * acontece por {@link #confirmarLiberacao}, e a edição da previsão não toca no valor confirmado.
 */
@Service
public class PrevisaoService {

    public static final String ENTIDADE = "Previsão de benefício";

    private static final LocalDate DATA_MINIMA = LocalDate.of(1900, 1, 1);
    private static final LocalDate DATA_MAXIMA = LocalDate.of(9999, 12, 31);

    private final PrevisaoBeneficioRepository previsaoRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final AuditoriaService auditoriaService;
    private final Clock relogio;

    public PrevisaoService(PrevisaoBeneficioRepository previsaoRepository, AtendimentoRepository atendimentoRepository,
                           AuditoriaService auditoriaService, Clock relogio) {
        this.previsaoRepository = previsaoRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.auditoriaService = auditoriaService;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    @Transactional(readOnly = true)
    public List<PrevisaoBeneficio> listarPorAtendimento(Long atendimentoId) {
        return previsaoRepository.findByAtendimentoIdOrderByDataPrevistaAscIdAsc(atendimentoId);
    }

    @Transactional(readOnly = true)
    public PrevisaoBeneficio buscar(Long id) {
        return previsaoRepository.findComDetalhesById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Previsão não encontrada."));
    }

    @Transactional(readOnly = true)
    public AgendaMensal agenda(FiltroAgenda filtro) {
        String padraoCliente = Textos.padraoBusca(filtro.getBusca());
        List<PrevisaoBeneficio> previsoes;
        if (filtro.isAtrasadas()) {
            previsoes = previsaoRepository.pesquisar(DATA_MINIMA, hoje().minusDays(1), SituacaoPrevisao.PENDENTES,
                    padraoCliente, filtro.getResponsavelId(), filtro.getTipoServicoId());
        } else {
            List<SituacaoPrevisao> situacoes = filtro.getSituacao() == null
                    ? Arrays.asList(SituacaoPrevisao.values())
                    : List.of(filtro.getSituacao());
            previsoes = previsaoRepository.pesquisar(
                    Objects.requireNonNullElse(filtro.getInicioEfetivo(), DATA_MINIMA),
                    Objects.requireNonNullElse(filtro.getFimEfetivo(), DATA_MAXIMA),
                    situacoes, padraoCliente, filtro.getResponsavelId(), filtro.getTipoServicoId());
        }
        return AgendaMensal.de(previsoes);
    }

    @Transactional(readOnly = true)
    public long contarAtrasadas() {
        return previsaoRepository.countBySituacaoInAndDataPrevistaBefore(SituacaoPrevisao.PENDENTES, hoje());
    }

    @Transactional
    public PrevisaoBeneficio criar(Long atendimentoId, PrevisaoForm form) {
        Atendimento atendimento = atendimentoRepository.findComDetalhesById(atendimentoId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Atendimento não encontrado."));
        if (atendimento.getSituacao() == SituacaoAtendimento.CANCELADO) {
            throw new RegraNegocioException(
                    "Este atendimento está cancelado. Altere a situação dele antes de registrar novas previsões.");
        }
        validarValorEstimado(form.getValorEstimado());
        validarSituacaoEscolhida(form.getSituacao());

        PrevisaoBeneficio previsao = new PrevisaoBeneficio(atendimento, form.getDataPrevista(),
                form.getValorEstimado(), form.getTipoPagamento());
        previsao.alterarSituacao(form.getSituacao());
        previsao.setObservacoes(Textos.opcional(form.getObservacoes()));
        previsaoRepository.save(previsao);

        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, previsao.getId(),
                "Previsão de '" + atendimento.getCliente().getNome() + "' (" + atendimento.getTipoServico().getNome()
                        + ") para " + Textos.data(previsao.getDataPrevista()) + ", valor estimado "
                        + Textos.moeda(previsao.getValorEstimado()) + ", " + previsao.getTipoPagamento().getRotulo()
                        + ".");
        return previsao;
    }

    @Transactional
    public PrevisaoBeneficio atualizar(Long id, PrevisaoForm form) {
        PrevisaoBeneficio previsao = buscar(id);
        validarValorEstimado(form.getValorEstimado());
        List<String> alteracoes = new ArrayList<>();

        if (!previsao.getDataPrevista().equals(form.getDataPrevista())) {
            alteracoes.add("data prevista de " + Textos.data(previsao.getDataPrevista()) + " para "
                    + Textos.data(form.getDataPrevista()));
            previsao.setDataPrevista(form.getDataPrevista());
        }
        if (previsao.getValorEstimado().compareTo(form.getValorEstimado()) != 0) {
            alteracoes.add("valor estimado de " + Textos.moeda(previsao.getValorEstimado()) + " para "
                    + Textos.moeda(form.getValorEstimado()));
            previsao.setValorEstimado(form.getValorEstimado());
        }
        if (previsao.getTipoPagamento() != form.getTipoPagamento()) {
            alteracoes.add("tipo de pagamento de " + previsao.getTipoPagamento().getRotulo() + " para "
                    + form.getTipoPagamento().getRotulo());
            previsao.setTipoPagamento(form.getTipoPagamento());
        }
        if (!previsao.isLiberada() && previsao.getSituacao() != form.getSituacao()) {
            validarSituacaoEscolhida(form.getSituacao());
            alteracoes.add(descricaoSituacao(previsao.getSituacao(), form.getSituacao()));
            previsao.alterarSituacao(form.getSituacao());
        }
        String observacoes = Textos.opcional(form.getObservacoes());
        if (!Objects.equals(previsao.getObservacoes(), observacoes)) {
            alteracoes.add("observações");
            previsao.setObservacoes(observacoes);
        }

        if (!alteracoes.isEmpty()) {
            registrarAlteracao(previsao, String.join(", ", alteracoes));
        }
        return previsao;
    }

    @Transactional
    public PrevisaoBeneficio alterarSituacao(Long id, SituacaoPrevisao nova) {
        PrevisaoBeneficio previsao = buscar(id);
        if (previsao.isLiberada()) {
            throw new RegraNegocioException("situacao",
                    "Esta previsão já foi liberada. Para mudar a situação, um administrador precisa desfazer a liberação.");
        }
        validarSituacaoEscolhida(nova);
        if (previsao.getSituacao() != nova) {
            String descricao = descricaoSituacao(previsao.getSituacao(), nova);
            previsao.alterarSituacao(nova);
            registrarAlteracao(previsao, descricao);
        }
        return previsao;
    }

    @Transactional
    public PrevisaoBeneficio confirmarLiberacao(Long id, ConfirmacaoLiberacaoForm form) {
        PrevisaoBeneficio previsao = buscar(id);
        if (previsao.getSituacao() == SituacaoPrevisao.CANCELADO) {
            throw new RegraNegocioException(
                    "Esta previsão está cancelada. Altere a situação antes de confirmar a liberação.");
        }
        if (form.getDataEfetiva() == null) {
            throw new RegraNegocioException("dataEfetiva", "Informe a data efetiva da liberação.");
        }
        if (form.getDataEfetiva().isAfter(hoje())) {
            throw new RegraNegocioException("dataEfetiva", "A data efetiva não pode estar no futuro.");
        }
        if (form.getValorConfirmado() == null || form.getValorConfirmado().signum() <= 0) {
            throw new RegraNegocioException("valorConfirmado", "O valor confirmado deve ser maior que zero.");
        }

        boolean correcao = previsao.isLiberada();
        String anterior = correcao
                ? " (antes: " + Textos.data(previsao.getDataEfetiva()) + ", "
                        + Textos.moeda(previsao.getValorConfirmado()) + ")"
                : "";
        previsao.confirmarLiberacao(form.getDataEfetiva(), form.getValorConfirmado());

        auditoriaService.registrar(correcao ? AcaoAuditoria.ALTERACAO : AcaoAuditoria.CONFIRMACAO, ENTIDADE,
                previsao.getId(),
                (correcao ? "Liberação corrigida" : "Liberação confirmada") + " para '"
                        + previsao.getAtendimento().getCliente().getNome() + "': em "
                        + Textos.data(previsao.getDataEfetiva()) + ", valor confirmado "
                        + Textos.moeda(previsao.getValorConfirmado()) + " (estimado "
                        + Textos.moeda(previsao.getValorEstimado()) + ")" + anterior + ".");
        return previsao;
    }

    @Transactional
    public PrevisaoBeneficio desfazerLiberacao(Long id) {
        PrevisaoBeneficio previsao = buscar(id);
        if (!previsao.isLiberada()) {
            throw new RegraNegocioException("Esta previsão não está liberada.");
        }
        String descricao = "Liberação desfeita para '" + previsao.getAtendimento().getCliente().getNome()
                + "' (era " + Textos.data(previsao.getDataEfetiva()) + ", "
                + Textos.moeda(previsao.getValorConfirmado()) + "). Situação voltou para "
                + SituacaoPrevisao.AGUARDANDO_CONFIRMACAO.getRotulo() + ".";
        previsao.desfazerLiberacao();
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, previsao.getId(), descricao);
        return previsao;
    }

    private void validarValorEstimado(BigDecimal valor) {
        if (valor == null) {
            throw new RegraNegocioException("valorEstimado", "Informe o valor estimado.");
        }
        if (valor.signum() < 0) {
            throw new RegraNegocioException("valorEstimado", "O valor estimado não pode ser negativo.");
        }
    }

    private void validarSituacaoEscolhida(SituacaoPrevisao situacao) {
        if (situacao == SituacaoPrevisao.LIBERADO) {
            throw new RegraNegocioException("situacao",
                    "Para marcar como liberado, use a ação \"Confirmar liberação\" e informe a data e o valor efetivos.");
        }
    }

    private void registrarAlteracao(PrevisaoBeneficio previsao, String alteracoes) {
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, previsao.getId(),
                "Previsão de '" + previsao.getAtendimento().getCliente().getNome() + "' alterada: "
                        + alteracoes + ".");
    }

    private static String descricaoSituacao(SituacaoPrevisao de, SituacaoPrevisao para) {
        return "situação de " + de.getRotulo() + " para " + para.getRotulo();
    }
}
