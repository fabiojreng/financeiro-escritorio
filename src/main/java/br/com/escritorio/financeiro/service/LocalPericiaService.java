package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.LocalPericia;
import br.com.escritorio.financeiro.dto.LocalPericiaForm;
import br.com.escritorio.financeiro.repository.LocalPericiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class LocalPericiaService {

    public static final String ENTIDADE = "Local de perícia";

    private final LocalPericiaRepository localPericiaRepository;
    private final AuditoriaService auditoriaService;

    public LocalPericiaService(LocalPericiaRepository localPericiaRepository, AuditoriaService auditoriaService) {
        this.localPericiaRepository = localPericiaRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<LocalPericia> listar() {
        return localPericiaRepository.findAllByOrderByAtivoDescNomeAsc();
    }

    /**
     * Locais ativos, mais o local já usado pela perícia em edição (mesmo que tenha sido desativado).
     */
    @Transactional(readOnly = true)
    public List<LocalPericia> listarParaSelecao(Long idAtual) {
        return localPericiaRepository.findByAtivoTrueOrIdOrderByNomeAsc(idAtual);
    }

    @Transactional(readOnly = true)
    public LocalPericia buscar(Long id) {
        return localPericiaRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Local de perícia não encontrado."));
    }

    @Transactional
    public LocalPericia criar(LocalPericiaForm form) {
        String nome = form.getNome().trim();
        if (localPericiaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("nome", "Já existe um local de perícia com este nome.");
        }
        LocalPericia local = new LocalPericia(nome, Textos.opcional(form.getEndereco()));
        localPericiaRepository.save(local);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, local.getId(),
                "Local de perícia '" + nome + "' cadastrado.");
        return local;
    }

    @Transactional
    public LocalPericia atualizar(Long id, LocalPericiaForm form) {
        LocalPericia local = buscar(id);
        String nome = form.getNome().trim();
        if (localPericiaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("nome", "Já existe um local de perícia com este nome.");
        }
        String endereco = Textos.opcional(form.getEndereco());
        if (local.getNome().equals(nome) && Objects.equals(local.getEndereco(), endereco)) {
            return local;
        }

        String nomeAnterior = local.getNome();
        local.setNome(nome);
        local.setEndereco(endereco);
        String detalhe = nomeAnterior.equals(nome)
                ? "Local de perícia '" + nome + "' alterado."
                : "Local de perícia renomeado de '" + nomeAnterior + "' para '" + nome + "'.";
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, local.getId(), detalhe);
        return local;
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo) {
        LocalPericia local = buscar(id);
        if (local.isAtivo() == ativo) {
            return;
        }
        local.setAtivo(ativo);
        auditoriaService.registrar(ativo ? AcaoAuditoria.ATIVACAO : AcaoAuditoria.DESATIVACAO, ENTIDADE,
                local.getId(), "Local de perícia '" + local.getNome() + "' " + (ativo ? "ativado." : "desativado."));
    }
}
