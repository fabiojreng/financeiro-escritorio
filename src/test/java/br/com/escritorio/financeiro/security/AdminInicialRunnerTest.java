package br.com.escritorio.financeiro.security;

import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.PerfisDeTeste;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.repository.PerfilRepository;
import br.com.escritorio.financeiro.repository.UsuarioRepository;
import br.com.escritorio.financeiro.service.AuditoriaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInicialRunnerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private AuditoriaService auditoriaService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void naoCriaQuandoJaExisteUsuario() {
        when(usuarioRepository.count()).thenReturn(1L);

        runner("admin", "senha-segura-123").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoCriaQuandoVariaveisNaoForamDefinidas() {
        when(usuarioRepository.count()).thenReturn(0L);

        runner("", "").run(null);
        runner(null, null).run(null);
        runner("admin", "").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void naoCriaComSenhaCurta() {
        when(usuarioRepository.count()).thenReturn(0L);

        runner("admin", "1234567").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void criaAdministradorComOPerfilDoSistemaESenhaCriptografada() {
        Perfil administrador = PerfisDeTeste.administrador();
        when(usuarioRepository.count()).thenReturn(0L);
        when(perfilRepository.findFirstBySistemaTrue()).thenReturn(Optional.of(administrador));

        runner(" admin ", "senha-segura-123").run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario criado = captor.getValue();
        assertThat(criado.getUsername()).isEqualTo("admin");
        assertThat(criado.getPerfil()).isSameAs(administrador);
        assertThat(criado.isAtivo()).isTrue();
        assertThat(criado.getSenhaHash()).isNotEqualTo("senha-segura-123");
        assertThat(passwordEncoder.matches("senha-segura-123", criado.getSenhaHash())).isTrue();
    }

    private AdminInicialRunner runner(String username, String senha) {
        return new AdminInicialRunner(usuarioRepository, perfilRepository, passwordEncoder, auditoriaService,
                new AdminInicialProperties(username, senha, "Administrador"));
    }
}
