package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.dto.UsuarioForm;
import br.com.escritorio.financeiro.repository.PerfilRepository;
import br.com.escritorio.financeiro.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class UsuarioService {

    public static final String ENTIDADE = "Usuário";
    public static final int TAMANHO_MINIMO_SENHA = 8;

    static final String MENSAGEM_ULTIMO_GESTOR =
            "O sistema precisa ter pelo menos um usuário ativo que possa gerenciar usuários.";

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository,
                          PasswordEncoder passwordEncoder, AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAllByOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Usuário não encontrado."));
    }

    @Transactional
    public Usuario criar(UsuarioForm form) {
        String username = form.getUsername().trim();
        if (usuarioRepository.existsByUsernameIgnoreCase(username)) {
            throw new RegraNegocioException("username", "Já existe um usuário com este login.");
        }
        Perfil perfil = perfilParaAtribuir(form.getPerfilId(), null);
        validarNovaSenha(form, true);

        Usuario usuario = new Usuario(username, form.getNome().trim(),
                passwordEncoder.encode(form.getSenha()), perfil);
        usuarioRepository.save(usuario);
        auditoriaService.registrar(AcaoAuditoria.CRIACAO, ENTIDADE, usuario.getId(),
                "Usuário '" + username + "' cadastrado com perfil " + perfil.getNome() + ".");
        return usuario;
    }

    @Transactional
    public Usuario atualizar(Long id, UsuarioForm form) {
        Usuario usuario = buscar(id);
        String username = form.getUsername().trim();
        if (usuarioRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw new RegraNegocioException("username", "Já existe um usuário com este login.");
        }
        Perfil perfil = perfilParaAtribuir(form.getPerfilId(), usuario.getPerfil());
        if (usuario.isAtivo() && usuario.possui(Permissao.ADMIN_USUARIOS) && !perfil.concede(Permissao.ADMIN_USUARIOS)
                && ehUltimoGestorDeUsuarios(usuario)) {
            throw new RegraNegocioException("perfilId", MENSAGEM_ULTIMO_GESTOR);
        }

        List<String> alteracoes = new ArrayList<>();
        if (!usuario.getUsername().equals(username)) {
            alteracoes.add("login de '" + usuario.getUsername() + "' para '" + username + "'");
            usuario.setUsername(username);
        }
        String nome = form.getNome().trim();
        if (!usuario.getNome().equals(nome)) {
            alteracoes.add("nome");
            usuario.setNome(nome);
        }
        if (!usuario.getPerfil().getId().equals(perfil.getId())) {
            alteracoes.add("perfil de " + usuario.getPerfil().getNome() + " para " + perfil.getNome());
            usuario.setPerfil(perfil);
        }
        if (StringUtils.hasText(form.getSenha())) {
            validarNovaSenha(form, false);
            usuario.setSenhaHash(passwordEncoder.encode(form.getSenha()));
            alteracoes.add("senha redefinida");
        }

        if (!alteracoes.isEmpty()) {
            auditoriaService.registrar(AcaoAuditoria.ALTERACAO, ENTIDADE, usuario.getId(),
                    "Usuário '" + usuario.getUsername() + "' alterado: " + String.join(", ", alteracoes) + ".");
        }
        return usuario;
    }

    @Transactional
    public void alterarSituacao(Long id, boolean ativo, String usernameLogado) {
        Usuario usuario = buscar(id);
        if (usuario.isAtivo() == ativo) {
            return;
        }
        if (!ativo) {
            if (usuario.getUsername().equalsIgnoreCase(usernameLogado)) {
                throw new RegraNegocioException("Você não pode desativar o seu próprio usuário.");
            }
            if (usuario.possui(Permissao.ADMIN_USUARIOS) && ehUltimoGestorDeUsuarios(usuario)) {
                throw new RegraNegocioException(MENSAGEM_ULTIMO_GESTOR);
            }
        }

        usuario.setAtivo(ativo);
        auditoriaService.registrar(ativo ? AcaoAuditoria.ATIVACAO : AcaoAuditoria.DESATIVACAO, ENTIDADE,
                usuario.getId(), "Usuário '" + usuario.getUsername() + "' " + (ativo ? "ativado." : "desativado."));
    }

    private boolean ehUltimoGestorDeUsuarios(Usuario usuario) {
        return usuarioRepository.contarAtivosComPermissaoExcetoUsuario(Permissao.ADMIN_USUARIOS, usuario.getId()) == 0;
    }

    /** O perfil escolhido precisa estar ativo, a não ser que seja o perfil que o usuário já tem. */
    private Perfil perfilParaAtribuir(Long perfilId, Perfil perfilAtual) {
        if (perfilAtual != null && perfilAtual.getId().equals(perfilId)) {
            return perfilAtual;
        }
        return perfilRepository.findById(perfilId)
                .filter(Perfil::isAtivo)
                .orElseThrow(() -> new RegraNegocioException("perfilId", "Selecione um perfil ativo."));
    }

    private void validarNovaSenha(UsuarioForm form, boolean obrigatoria) {
        String senha = form.getSenha();
        if (!StringUtils.hasText(senha)) {
            if (obrigatoria) {
                throw new RegraNegocioException("senha", "Informe a senha.");
            }
            return;
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new RegraNegocioException("senha",
                    "A senha precisa ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        if (!senha.equals(form.getConfirmacaoSenha())) {
            throw new RegraNegocioException("confirmacaoSenha", "A confirmação não confere com a senha.");
        }
    }
}
