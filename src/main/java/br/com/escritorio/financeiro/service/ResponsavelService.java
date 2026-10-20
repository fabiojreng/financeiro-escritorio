package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.dto.ResponsavelForm;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class ResponsavelService {

    public static final String ENTIDADE = "Responsável";

    private final ResponsavelRepository responsavelRepository;
    private final AuditoriaService auditoriaService;

    public ResponsavelService(ResponsavelRepository responsavelRepository, AuditoriaService auditoriaService) {
        this.responsavelRepository = responsavelRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Responsavel> listar() {
        return responsavelRepository.findAllByOrderByAtivoDescNomeAsc();
    }

    /**
     * Responsáveis ativos, mais os já usados pelo registro em edição (mesmo que tenham sido desativados).
     */
    @Transactional(readOnly = true)
    public List<Responsavel> listarParaSelecao(Collection<Long> idsAtuais) {
        if (idsAtuais == null || idsAtuais.isEmpty()) {
            return responsavelRepository.findByAtivoTrueOrderByNomeAsc();
        }
        return responsavelRepository.findByAtivoTrueOrIdInOrderByNomeAsc(idsAtuais);
    }

    @Transactional(readOnly = true)
    public Responsavel buscar(Long id) {
        return responsavelRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Responsável não encontrado."));
    }

    @Transactional
    public Responsavel criar(ResponsavelForm form) {
        String nome = form.getNome().trim();
        if (responsavelRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("nome", "Já existe um responsável com este nome.");
        }
        Responsavel responsavel = new Responsavel(nome, textoOpcional(form.getObservacoes()));
        responsavelRepository.save(responsavel);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, responsavel.getId(),
                "Responsável '" + nome + "' cadastrado.");
        return responsavel;
    }

    @Transactional
    public Responsavel atualizar(Long id, ResponsavelForm form) {
        Responsavel responsavel = buscar(id);
        String nome = form.getNome().trim();
        if (responsavelRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("nome", "Já existe um responsável com este nome.");
        }
        String observacoes = textoOpcional(form.getObservacoes());
        if (responsavel.getNome().equals(nome) && Objects.equals(responsavel.getObservacoes(), observacoes)) {
            return responsavel;
        }

        String nomeAnterior = responsavel.getNome();
        responsavel.setNome(nome);
        responsavel.setObservacoes(observacoes);
        String detalhe = nomeAnterior.equals(nome)
                ? "Responsável '" + nome + "' alterado."
                : "Responsável renomeado de '" + nomeAnterior + "' para '" + nome + "'.";
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, responsavel.getId(), detalhe);
        return responsavel;
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo) {
        Responsavel responsavel = buscar(id);
        if (responsavel.isAtivo() == ativo) {
            return;
        }
        responsavel.setAtivo(ativo);
        auditoriaService.registrar(ativo ? AcaoAuditoria.ATIVACAO : AcaoAuditoria.DESATIVACAO, ENTIDADE,
                responsavel.getId(),
                "Responsável '" + responsavel.getNome() + "' " + (ativo ? "ativado." : "desativado."));
    }

    private static String textoOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
