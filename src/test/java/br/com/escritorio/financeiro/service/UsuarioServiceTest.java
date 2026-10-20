package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.PerfisDeTeste;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.dto.UsuarioForm;
import br.com.escritorio.financeiro.repository.PerfilRepository;
import br.com.escritorio.financeiro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String SENHA = "senha-segura-123";

    private final Perfil administrador = PerfisDeTeste.administrador();
    private final Perfil operador = PerfisDeTeste.operador();

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, perfilRepository, passwordEncoder, auditoriaService);
        lenient().when(perfilRepository.findById(administrador.getId())).thenReturn(Optional.of(administrador));
        lenient().when(perfilRepository.findById(operador.getId())).thenReturn(Optional.of(operador));
    }

    @Test
    void criaUsuarioComSenhaCriptografadaERegistraAuditoriaSemSenha() {
        UsuarioForm form = form("maria", operador, SENHA, SENHA);

        Usuario criado = service.criar(form);

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertThat(usuario.getValue().getSenhaHash()).isNotEqualTo(SENHA);
        assertThat(usuario.getValue().getPerfil()).isSameAs(operador);
        assertThat(passwordEncoder.matches(SENHA, criado.getSenhaHash())).isTrue();

        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(UsuarioService.ENTIDADE), any(),
                descricao.capture());
        assertThat(descricao.getValue()).contains("maria", "Operador").doesNotContain(SENHA);
    }

    @Test
    void naoAtribuiPerfilInativo() {
        Perfil inativo = PerfisDeTeste.perfil(9L, "Antigo", List.of(Permissao.AGENDA_VISUALIZAR));
        inativo.setAtivo(false);
        when(perfilRepository.findById(9L)).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> service.criar(form("maria", inativo, SENHA, SENHA)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("perfilId");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rejeitaLoginDuplicadoIgnorandoMaiusculas() {
        when(usuarioRepository.existsByUsernameIgnoreCase("Maria")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(form("Maria", operador, SENHA, SENHA)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("username");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void exigeSenhaNoCadastro() {
        assertThatThrownBy(() -> service.criar(form("maria", operador, "", "")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("senha");
    }

    @Test
    void rejeitaSenhaCurta() {
        assertThatThrownBy(() -> service.criar(form("maria", operador, "1234567", "1234567")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("senha");
    }

    @Test
    void rejeitaConfirmacaoDiferente() {
        assertThatThrownBy(() -> service.criar(form("maria", operador, SENHA, "outra-senha-123")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("confirmacaoSenha");
    }

    @Test
    void edicaoSemSenhaMantemSenhaAtual() {
        Usuario existente = usuario(1L, "maria", operador, true);
        String hashOriginal = existente.getSenhaHash();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.atualizar(1L, form("maria", operador, "", ""));

        assertThat(existente.getSenhaHash()).isEqualTo(hashOriginal);
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void edicaoComNovaSenhaRegistraRedefinicaoSemExporSenha() {
        Usuario existente = usuario(1L, "maria", operador, true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.atualizar(1L, form("maria", operador, "nova-senha-456", "nova-senha-456"));

        assertThat(passwordEncoder.matches("nova-senha-456", existente.getSenhaHash())).isTrue();
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), anyString(), any(), descricao.capture());
        assertThat(descricao.getValue()).contains("senha redefinida").doesNotContain("nova-senha-456");
    }

    @Test
    void trocaDePerfilFicaNaAuditoriaComOsNomes() {
        Usuario existente = usuario(1L, "maria", operador, true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.atualizar(1L, form("maria", administrador, "", ""));

        assertThat(existente.getPerfil()).isSameAs(administrador);
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), anyString(), eq(1L), descricao.capture());
        assertThat(descricao.getValue()).contains("perfil de Operador para Administrador");
    }

    @Test
    void naoPermiteTirarOUltimoUsuarioQueGerenciaUsuarios() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "admin", administrador, true)));
        when(usuarioRepository.contarAtivosComPermissaoExcetoUsuario(Permissao.ADMIN_USUARIOS, 1L)).thenReturn(0L);

        assertThatThrownBy(() -> service.atualizar(1L, form("admin", operador, "", "")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("gerenciar usuários")
                .extracting("campo").isEqualTo("perfilId");
    }

    @Test
    void permiteTrocarPerfilQuandoOutroUsuarioAindaGerenciaUsuarios() {
        Usuario admin = usuario(1L, "admin", administrador, true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.contarAtivosComPermissaoExcetoUsuario(Permissao.ADMIN_USUARIOS, 1L)).thenReturn(1L);

        service.atualizar(1L, form("admin", operador, "", ""));

        assertThat(admin.getPerfil()).isSameAs(operador);
    }

    @Test
    void naoPermiteDesativarOProprioUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "maria", administrador, true)));

        assertThatThrownBy(() -> service.alterarSituacao(1L, false, "MARIA"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("próprio usuário");
    }

    @Test
    void naoPermiteDesativarOUltimoUsuarioQueGerenciaUsuarios() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "admin", administrador, true)));
        when(usuarioRepository.contarAtivosComPermissaoExcetoUsuario(Permissao.ADMIN_USUARIOS, 1L)).thenReturn(0L);

        assertThatThrownBy(() -> service.alterarSituacao(1L, false, "outro"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("gerenciar usuários");
    }

    @Test
    void desativaOperadorERegistraAuditoria() {
        Usuario usuario = usuario(2L, "joao", operador, true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));

        service.alterarSituacao(2L, false, "admin");

        assertThat(usuario.isAtivo()).isFalse();
        verify(auditoriaService).registrar(eq(AcaoAuditoria.DESATIVACAO), eq(UsuarioService.ENTIDADE), any(),
                anyString());
    }

    @Test
    void usuarioInexistenteGeraRegistroNaoEncontrado() {
        when(usuarioRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(RegistroNaoEncontradoException.class);
    }

    private UsuarioForm form(String username, Perfil perfil, String senha, String confirmacao) {
        UsuarioForm form = new UsuarioForm();
        form.setUsername(username);
        form.setNome("Nome de " + username);
        form.setPerfilId(perfil.getId());
        form.setSenha(senha);
        form.setConfirmacaoSenha(confirmacao);
        return form;
    }

    private Usuario usuario(Long id, String username, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario(username, "Nome de " + username, passwordEncoder.encode(SENHA), perfil);
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
