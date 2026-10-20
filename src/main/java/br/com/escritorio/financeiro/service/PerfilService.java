package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.dto.PerfilForm;
import br.com.escritorio.financeiro.repository.PerfilRepository;
import br.com.escritorio.financeiro.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Perfis de acesso. As alterações valem para os usuários a partir do próximo login.
 */
@Service
public class PerfilService {

    public static final String ENTIDADE = "Perfil";

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public PerfilService(PerfilRepository perfilRepository, UsuarioRepository usuarioRepository,
                         AuditoriaService auditoriaService) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Perfil> listar() {
        return perfilRepository.findAllByOrderByAtivoDescNomeAsc();
    }

    /** Perfis ativos, mais o perfil atual do usuário em edição (mesmo que tenha sido inativado). */
    @Transactional(readOnly = true)
    public List<Perfil> listarParaSelecao(Long idAtual) {
        return perfilRepository.findByAtivoTrueOrIdOrderByNomeAsc(idAtual);
    }

    /** Quantidade de usuários vinculados a cada perfil, pelo id do perfil. */
    @Transactional(readOnly = true)
    public Map<Long, Long> usuariosPorPerfil() {
        Map<Long, Long> quantidades = new HashMap<>();
        for (Object[] linha : usuarioRepository.contarPorPerfil()) {
            quantidades.put((Long) linha[0], (Long) linha[1]);
        }
        return quantidades;
    }

    @Transactional(readOnly = true)
    public Perfil buscar(Long id) {
        return perfilRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Perfil não encontrado."));
    }

    @Transactional
    public Perfil criar(PerfilForm form) {
        String nome = form.getNome().trim();
        if (perfilRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("nome", "Já existe um perfil com este nome.");
        }
        Perfil perfil = new Perfil(nome, textoOpcional(form.getDescricao()), form.getPermissoes());
        perfilRepository.save(perfil);

        String permissoes = " Permissões: " + rotulos(perfil.getPermissoes()) + ".";
        if (form.isDuplicacao()) {
            String origem = perfilRepository.findById(form.getOrigemId()).map(Perfil::getNome).orElse("?");
            auditoriaService.registrar(AcaoAuditoria.DUPLICACAO, ENTIDADE, perfil.getId(),
                    "Perfil '" + nome + "' criado a partir do perfil '" + origem + "'." + permissoes);
        } else {
            auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, perfil.getId(),
                    "Perfil '" + nome + "' cadastrado." + permissoes);
        }
        return perfil;
    }

    /**
     * @param usernameLogado quem está editando: não pode tirar do próprio perfil a permissão de gerenciar perfis
     */
    @Transactional
    public Perfil atualizar(Long id, PerfilForm form, String usernameLogado) {
        Perfil perfil = buscar(id);
        String descricao = textoOpcional(form.getDescricao());
        if (perfil.isSistema()) {
            return atualizarDescricaoDoSistema(perfil, descricao);
        }

        String nome = form.getNome().trim();
        if (perfilRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraNegocioException("nome", "Já existe um perfil com este nome.");
        }
        Set<Permissao> novas = form.getPermissoes().isEmpty()
                ? EnumSet.noneOf(Permissao.class)
                : EnumSet.copyOf(form.getPermissoes());
        Set<Permissao> adicionadas = EnumSet.copyOf(novas);
        adicionadas.removeAll(perfil.getPermissoes());
        Set<Permissao> removidas = perfil.permissoesEfetivas();
        removidas.removeAll(novas);

        if (removidas.contains(Permissao.ADMIN_PERFIS) && ehPerfilDe(perfil, usernameLogado)) {
            throw new RegraNegocioException("permissoes",
                    "Você não pode remover do seu próprio perfil a permissão de gerenciar perfis.");
        }
        if (removidas.contains(Permissao.ADMIN_USUARIOS) && usuarioRepository.countByPerfilAndAtivoTrue(perfil) > 0
                && usuarioRepository.contarAtivosComPermissaoForaDoPerfil(Permissao.ADMIN_USUARIOS, id) == 0) {
            throw new RegraNegocioException("permissoes", UsuarioService.MENSAGEM_ULTIMO_GESTOR);
        }

        List<String> alteracoes = new ArrayList<>();
        if (!perfil.getNome().equals(nome)) {
            alteracoes.add("nome de '" + perfil.getNome() + "' para '" + nome + "'");
            perfil.setNome(nome);
        }
        if (!Objects.equals(perfil.getDescricao(), descricao)) {
            alteracoes.add("descrição");
            perfil.setDescricao(descricao);
        }
        if (!adicionadas.isEmpty()) {
            alteracoes.add("permissões adicionadas: " + rotulos(adicionadas));
        }
        if (!removidas.isEmpty()) {
            alteracoes.add("permissões removidas: " + rotulos(removidas));
        }
        if (!adicionadas.isEmpty() || !removidas.isEmpty()) {
            perfil.setPermissoes(novas);
        }

        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, perfil.getId(),
                    "Perfil '" + perfil.getNome() + "' alterado: " + String.join("; ", alteracoes) + ".");
        }
        return perfil;
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo) {
        Perfil perfil = buscar(id);
        if (perfil.isAtivo() == ativo) {
            return;
        }
        if (!ativo) {
            if (perfil.isSistema()) {
                throw new RegraNegocioException("O perfil " + perfil.getNome() + " é do sistema e não pode ser inativado.");
            }
            long usuariosAtivos = usuarioRepository.countByPerfilAndAtivoTrue(perfil);
            if (usuariosAtivos > 0) {
                throw new RegraNegocioException("O perfil " + perfil.getNome() + " tem " + usuariosAtivos
                        + (usuariosAtivos == 1 ? " usuário ativo" : " usuários ativos")
                        + ". Mude o perfil desses usuários ou desative-os antes de inativar o perfil.");
            }
        }
        perfil.setAtivo(ativo);
        auditoriaService.registrar(ativo ? AcaoAuditoria.ATIVACAO : AcaoAuditoria.DESATIVACAO, ENTIDADE,
                perfil.getId(), "Perfil '" + perfil.getNome() + "' " + (ativo ? "ativado." : "inativado."));
    }

    private Perfil atualizarDescricaoDoSistema(Perfil perfil, String descricao) {
        if (!Objects.equals(perfil.getDescricao(), descricao)) {
            perfil.setDescricao(descricao);
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, perfil.getId(),
                    "Perfil '" + perfil.getNome() + "' alterado: descrição.");
        }
        return perfil;
    }

    private boolean ehPerfilDe(Perfil perfil, String username) {
        return username != null && usuarioRepository.findByUsernameIgnoreCase(username)
                .map(usuario -> usuario.getPerfil().getId().equals(perfil.getId()))
                .orElse(false);
    }

    static String rotulos(Collection<Permissao> permissoes) {
        if (permissoes.isEmpty()) {
            return "nenhuma";
        }
        return permissoes.stream()
                .sorted()
                .map(Permissao::getRotulo)
                .collect(Collectors.joining(", "));
    }

    private static String textoOpcional(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
