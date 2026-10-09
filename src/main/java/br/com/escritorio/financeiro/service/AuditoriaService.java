package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.repository.AuditoriaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Trilha de operações relevantes. As descrições não devem conter senhas nem documentos pessoais completos.
 */
@Service
public class AuditoriaService {

    public static final String USUARIO_SISTEMA = "sistema";

    private static final int TAMANHO_PAGINA = 50;
    private static final int TAMANHO_MAXIMO_DESCRICAO = 500;

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional
    public void registrar(AcaoAuditoria acao, String entidade, Long entidadeId, String descricao) {
        String texto = descricao.length() > TAMANHO_MAXIMO_DESCRICAO
                ? descricao.substring(0, TAMANHO_MAXIMO_DESCRICAO)
                : descricao;
        auditoriaRepository.save(new Auditoria(usuarioAtual(), acao, entidade, entidadeId, texto));
    }

    @Transactional(readOnly = true)
    public Page<Auditoria> listar(String entidade, int pagina) {
        PageRequest pageRequest = PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA);
        if (StringUtils.hasText(entidade)) {
            return auditoriaRepository.findByEntidadeOrderByDataHoraDescIdDesc(entidade, pageRequest);
        }
        return auditoriaRepository.findAllByOrderByDataHoraDescIdDesc(pageRequest);
    }

    @Transactional(readOnly = true)
    public List<Auditoria> historico(String entidade, Long entidadeId) {
        return auditoriaRepository.findByEntidadeAndEntidadeIdOrderByDataHoraDescIdDesc(entidade, entidadeId);
    }

    private String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            return USUARIO_SISTEMA;
        }
        return auth.getName();
    }
}
