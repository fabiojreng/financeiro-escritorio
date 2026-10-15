package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import br.com.escritorio.financeiro.dto.FormaPagamentoDespesaForm;
import br.com.escritorio.financeiro.repository.FormaPagamentoDespesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class FormaPagamentoDespesaService {

    public static final String ENTIDADE = "Forma de pagamento de despesa";

    private final FormaPagamentoDespesaRepository formaRepository;
    private final AuditoriaService auditoriaService;

    public FormaPagamentoDespesaService(FormaPagamentoDespesaRepository formaRepository,
                                        AuditoriaService auditoriaService) {
        this.formaRepository = formaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<FormaPagamentoDespesa> listar() {
        return formaRepository.findAllByOrderByAtivoDescNomeAsc();
    }

    /**
     * Formas ativas, mais a forma já usada pela despesa em edição (mesmo que tenha sido desativada).
     */
    @Transactional(readOnly = true)
    public List<FormaPagamentoDespesa> listarParaSelecao(Long idAtual) {
        return formaRepository.findByAtivoTrueOrIdOrderByNomeAsc(idAtual);
    }

    @Transactional(readOnly = true)
    public FormaPagamentoDespesa buscar(Long id) {
        return formaRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Forma de pagamento não encontrada."));
    }

    @Transactional
    public FormaPagamentoDespesa criar(FormaPagamentoDespesaForm form) {
        String nome = form.getNome().trim();
        if (formaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("nome", "Já existe uma forma de pagamento com esta sigla.");
        }
        FormaPagamentoDespesa forma = new FormaPagamentoDespesa(nome, Textos.opcional(form.getDescricao()));
        formaRepository.save(forma);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, forma.getId(),
                "Forma de pagamento de despesa '" + nome + "' cadastrada.");
        return forma;
    }

    @Transactional
    public FormaPagamentoDespesa atualizar(Long id, FormaPagamentoDespesaForm form) {
        FormaPagamentoDespesa forma = buscar(id);
        String nome = form.getNome().trim();
        if (formaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("nome", "Já existe uma forma de pagamento com esta sigla.");
        }
        String descricao = Textos.opcional(form.getDescricao());
        if (forma.getNome().equals(nome) && Objects.equals(forma.getDescricao(), descricao)) {
            return forma;
        }

        String nomeAnterior = forma.getNome();
        forma.setNome(nome);
        forma.setDescricao(descricao);
        String detalhe = nomeAnterior.equals(nome)
                ? "Forma de pagamento de despesa '" + nome + "' alterada."
                : "Forma de pagamento de despesa renomeada de '" + nomeAnterior + "' para '" + nome + "'.";
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, forma.getId(), detalhe);
        return forma;
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo) {
        FormaPagamentoDespesa forma = buscar(id);
        if (forma.isAtivo() == ativo) {
            return;
        }
        forma.setAtivo(ativo);
        auditoriaService.registrar(ativo ? AcaoAuditoria.ATIVACAO : AcaoAuditoria.DESATIVACAO, ENTIDADE,
                forma.getId(), "Forma de pagamento de despesa '" + forma.getNome() + "' "
                        + (ativo ? "ativada." : "desativada."));
    }
}
