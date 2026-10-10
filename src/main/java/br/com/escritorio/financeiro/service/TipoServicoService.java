package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.TipoServicoForm;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class TipoServicoService {

    public static final String ENTIDADE = "Tipo de serviço";

    private final TipoServicoRepository tipoServicoRepository;
    private final AuditoriaService auditoriaService;

    public TipoServicoService(TipoServicoRepository tipoServicoRepository, AuditoriaService auditoriaService) {
        this.tipoServicoRepository = tipoServicoRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<TipoServico> listar() {
        return tipoServicoRepository.findAllByOrderByAtivoDescNomeAsc();
    }

    @Transactional(readOnly = true)
    public TipoServico buscar(Long id) {
        return tipoServicoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Tipo de serviço não encontrado."));
    }

    @Transactional
    public TipoServico criar(TipoServicoForm form) {
        String nome = form.getNome().trim();
        if (tipoServicoRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("nome", "Já existe um tipo de serviço com este nome.");
        }
        TipoServico tipo = new TipoServico(nome, textoOpcional(form.getDescricao()));
        tipoServicoRepository.save(tipo);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, tipo.getId(),
                "Tipo de serviço '" + nome + "' cadastrado.");
        return tipo;
    }

    @Transactional
    public TipoServico atualizar(Long id, TipoServicoForm form) {
        TipoServico tipo = buscar(id);
        String nome = form.getNome().trim();
        if (tipoServicoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("nome", "Já existe um tipo de serviço com este nome.");
        }
        String descricao = textoOpcional(form.getDescricao());
        if (tipo.getNome().equals(nome) && Objects.equals(tipo.getDescricao(), descricao)) {
            return tipo;
        }

        String nomeAnterior = tipo.getNome();
        tipo.setNome(nome);
        tipo.setDescricao(descricao);
        String detalhe = nomeAnterior.equals(nome)
                ? "Tipo de serviço '" + nome + "' alterado."
                : "Tipo de serviço renomeado de '" + nomeAnterior + "' para '" + nome + "'.";
        auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, tipo.getId(), detalhe);
        return tipo;
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo) {
        TipoServico tipo = buscar(id);
        if (tipo.isAtivo() == ativo) {
            return;
        }
        tipo.setAtivo(ativo);
        auditoriaService.registrar(ativo ? AcaoAuditoria.ATIVACAO : AcaoAuditoria.DESATIVACAO, ENTIDADE,
                tipo.getId(), "Tipo de serviço '" + tipo.getNome() + "' " + (ativo ? "ativado." : "desativado."));
    }

    private static String textoOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
