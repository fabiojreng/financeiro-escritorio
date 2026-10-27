package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.TipoPericia;
import br.com.escritorio.financeiro.dto.TipoPericiaForm;
import br.com.escritorio.financeiro.repository.TipoPericiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class TipoPericiaService {

    public static final String ENTIDADE = "Tipo de perícia";

    private final TipoPericiaRepository tipoPericiaRepository;
    private final AuditoriaService auditoriaService;

    public TipoPericiaService(TipoPericiaRepository tipoPericiaRepository, AuditoriaService auditoriaService) {
        this.tipoPericiaRepository = tipoPericiaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<TipoPericia> listar() {
        return tipoPericiaRepository.findAllByOrderByAtivoDescNomeAsc();
    }

    /**
     * Tipos ativos, mais o tipo já usado pela perícia em edição (mesmo que tenha sido desativado).
     */
    @Transactional(readOnly = true)
    public List<TipoPericia> listarParaSelecao(Long idAtual) {
        return tipoPericiaRepository.findByAtivoTrueOrIdOrderByNomeAsc(idAtual);
    }

    @Transactional(readOnly = true)
    public TipoPericia buscar(Long id) {
        return tipoPericiaRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Tipo de perícia não encontrado."));
    }

    @Transactional
    public TipoPericia criar(TipoPericiaForm form) {
        String nome = form.getNome().trim();
        if (tipoPericiaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("nome", "Já existe um tipo de perícia com este nome.");
        }
        TipoPericia tipo = new TipoPericia(nome, Textos.opcional(form.getDescricao()));
        tipoPericiaRepository.save(tipo);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, tipo.getId(),
                "Tipo de perícia '" + nome + "' cadastrado.");
        return tipo;
    }

    @Transactional
    public TipoPericia atualizar(Long id, TipoPericiaForm form) {
        TipoPericia tipo = buscar(id);
        String nome = form.getNome().trim();
        if (tipoPericiaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("nome", "Já existe um tipo de perícia com este nome.");
        }
        String descricao = Textos.opcional(form.getDescricao());
        if (tipo.getNome().equals(nome) && Objects.equals(tipo.getDescricao(), descricao)) {
            return tipo;
        }

        String nomeAnterior = tipo.getNome();
        tipo.setNome(nome);
        tipo.setDescricao(descricao);
        String detalhe = nomeAnterior.equals(nome)
                ? "Tipo de perícia '" + nome + "' alterado."
                : "Tipo de perícia renomeado de '" + nomeAnterior + "' para '" + nome + "'.";
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, tipo.getId(), detalhe);
        return tipo;
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo) {
        TipoPericia tipo = buscar(id);
        if (tipo.isAtivo() == ativo) {
            return;
        }
        tipo.setAtivo(ativo);
        auditoriaService.registrar(ativo ? AcaoAuditoria.ATIVACAO : AcaoAuditoria.DESATIVACAO, ENTIDADE,
                tipo.getId(), "Tipo de perícia '" + tipo.getNome() + "' " + (ativo ? "ativado." : "desativado."));
    }
}
