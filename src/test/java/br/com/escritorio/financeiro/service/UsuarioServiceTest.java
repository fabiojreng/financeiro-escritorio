package br.com.escritorio.financeiro.service;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.dto.UsuarioForm;
import br.com.escritorio.financeiro.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String SENHA = "senha-segura-123";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, passwordEncoder, auditoriaService);
    }

    @Test
    void criaUsuarioComSenhaCriptografadaERegistraAuditoriaSemSenha() {
        UsuarioForm form = form("maria", Perfil.OPERADOR, SENHA, SENHA);

        Usuario criado = service.criar(form);

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertThat(usuario.getValue().getSenhaHash()).isNotEqualTo(SENHA);
        assertThat(passwordEncoder.matches(SENHA, criado.getSenhaHash())).isTrue();

        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.CRIACAO), eq(UsuarioService.ENTIDADE), any(),
                descricao.capture());
        assertThat(descricao.getValue()).contains("maria").doesNotContain(SENHA);
    }

    @Test
    void rejeitaLoginDuplicadoIgnorandoMaiusculas() {
        when(usuarioRepository.existsByUsernameIgnoreCase("Maria")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(form("Maria", Perfil.OPERADOR, SENHA, SENHA)))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("username");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void exigeSenhaNoCadastro() {
        assertThatThrownBy(() -> service.criar(form("maria", Perfil.OPERADOR, "", "")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("senha");
    }

    @Test
    void rejeitaSenhaCurta() {
        assertThatThrownBy(() -> service.criar(form("maria", Perfil.OPERADOR, "1234567", "1234567")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("senha");
    }

    @Test
    void rejeitaConfirmacaoDiferente() {
        assertThatThrownBy(() -> service.criar(form("maria", Perfil.OPERADOR, SENHA, "outra-senha-123")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("confirmacaoSenha");
    }

    @Test
    void edicaoSemSenhaMantemSenhaAtual() {
        Usuario existente = usuario("maria", Perfil.OPERADOR, true);
        String hashOriginal = existente.getSenhaHash();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.atualizar(1L, form("maria", Perfil.OPERADOR, "", ""));

        assertThat(existente.getSenhaHash()).isEqualTo(hashOriginal);
        verifyNoInteractions(auditoriaService);
    }

    @Test
    void edicaoComNovaSenhaRegistraRedefinicaoSemExporSenha() {
        Usuario existente = usuario("maria", Perfil.OPERADOR, true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

        service.atualizar(1L, form("maria", Perfil.OPERADOR, "nova-senha-456", "nova-senha-456"));

        assertThat(passwordEncoder.matches("nova-senha-456", existente.getSenhaHash())).isTrue();
        ArgumentCaptor<String> descricao = ArgumentCaptor.forClass(String.class);
        verify(auditoriaService).registrar(eq(AcaoAuditoria.ALTERACAO), anyString(), any(), descricao.capture());
        assertThat(descricao.getValue()).contains("senha redefinida").doesNotContain("nova-senha-456");
    }

    @Test
    void naoPermiteRebaixarOUltimoAdministradorAtivo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario("admin", Perfil.ADMIN, true)));
        when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.atualizar(1L, form("admin", Perfil.OPERADOR, "", "")))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("campo").isEqualTo("perfil");
    }

    @Test
    void naoPermiteDesativarOProprioUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario("maria", Perfil.ADMIN, true)));

        assertThatThrownBy(() -> service.alterarSituacao(1L, false, "MARIA"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("próprio usuário");
    }

    @Test
    void naoPermiteDesativarOUltimoAdministradorAtivo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario("admin", Perfil.ADMIN, true)));
        when(usuarioRepository.countByPerfilAndAtivoTrue(Perfil.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.alterarSituacao(1L, false, "outro"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("administrador ativo");
    }

    @Test
    void desativaOperadorERegistraAuditoria() {
        Usuario operador = usuario("joao", Perfil.OPERADOR, true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(operador));

        service.alterarSituacao(2L, false, "admin");

        assertThat(operador.isAtivo()).isFalse();
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
        form.setPerfil(perfil);
        form.setSenha(senha);
        form.setConfirmacaoSenha(confirmacao);
        return form;
    }

    private Usuario usuario(String username, Perfil perfil, boolean ativo) {
        Usuario usuario = new Usuario(username, "Nome de " + username, passwordEncoder.encode(SENHA), perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
