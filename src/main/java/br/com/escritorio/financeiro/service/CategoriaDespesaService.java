package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import br.com.escritorio.financeiro.dto.CategoriaDespesaForm;
import br.com.escritorio.financeiro.repository.CategoriaDespesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class CategoriaDespesaService {

    public static final String ENTIDADE = "Categoria de despesa";

    private final CategoriaDespesaRepository categoriaRepository;
    private final AuditoriaService auditoriaService;

    public CategoriaDespesaService(CategoriaDespesaRepository categoriaRepository, AuditoriaService auditoriaService) {
        this.categoriaRepository = categoriaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDespesa> listar() {
        return categoriaRepository.findAllByOrderByAtivoDescNomeAsc();
    }

    /**
     * Categorias ativas, mais a categoria já usada pela despesa em edição (mesmo que tenha sido desativada).
     */
    @Transactional(readOnly = true)
    public List<CategoriaDespesa> listarParaSelecao(Long idAtual) {
        return categoriaRepository.findByAtivoTrueOrIdOrderByNomeAsc(idAtual);
    }

    @Transactional(readOnly = true)
    public CategoriaDespesa buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Categoria de despesa não encontrada."));
    }

    @Transactional
    public CategoriaDespesa criar(CategoriaDespesaForm form) {
        String nome = form.getNome().trim();
        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("nome", "Já existe uma categoria com este nome.");
        }
        CategoriaDespesa categoria = new CategoriaDespesa(nome, Textos.opcional(form.getHistoricoPadrao()));
        categoriaRepository.save(categoria);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, categoria.getId(),
                "Categoria de despesa '" + nome + "' cadastrada.");
        return categoria;
    }

    @Transactional
    public CategoriaDespesa atualizar(Long id, CategoriaDespesaForm form) {
        CategoriaDespesa categoria = buscar(id);
        String nome = form.getNome().trim();
        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("nome", "Já existe uma categoria com este nome.");
        }
        String historicoPadrao = Textos.opcional(form.getHistoricoPadrao());
        if (categoria.getNome().equals(nome) && Objects.equals(categoria.getHistoricoPadrao(), historicoPadrao)) {
            return categoria;
        }

        String nomeAnterior = categoria.getNome();
        categoria.setNome(nome);
        categoria.setHistoricoPadrao(historicoPadrao);
        String detalhe = nomeAnterior.equals(nome)
                ? "Categoria de despesa '" + nome + "' alterada."
                : "Categoria de despesa renomeada de '" + nomeAnterior + "' para '" + nome + "'.";
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, categoria.getId(), detalhe);
        return categoria;
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo) {
        CategoriaDespesa categoria = buscar(id);
        if (categoria.isAtivo() == ativo) {
            return;
        }
        categoria.setAtivo(ativo);
        auditoriaService.registrar(ativo ? AcaoAuditoria.ATIVACAO : AcaoAuditoria.DESATIVACAO, ENTIDADE,
                categoria.getId(), "Categoria de despesa '" + categoria.getNome() + "' "
                        + (ativo ? "ativada." : "desativada."));
    }
}
