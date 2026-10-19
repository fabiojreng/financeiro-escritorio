package br.com.escritorio.financeiro.security;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class SegurancaIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void visitanteAnonimoERedirecionadoParaLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void paginaDeLoginEPublica() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Entrar")));
    }

    @Test
    void recursosEstaticosSaoPublicos() throws Exception {
        mvc.perform(get("/webjars/bootstrap/css/bootstrap.min.css")).andExpect(status().isOk());
        mvc.perform(get("/css/app.css")).andExpect(status().isOk());
    }

    @Test
    void loginComCredenciaisValidasLevaAoInicio() throws Exception {
        criarUsuario("maria", "senha-segura-123", Perfil.OPERADOR);

        mvc.perform(formLogin("/login").user("maria").password("senha-segura-123"))
                .andExpect(authenticated().withUsername("maria").withRoles("OPERADOR"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginComSenhaErradaVoltaComErro() throws Exception {
        criarUsuario("joao", "senha-segura-123", Perfil.OPERADOR);

        mvc.perform(formLogin("/login").user("joao").password("errada"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    void usuarioAutenticadoVaiDoInicioParaAAgendaDePagamentos() throws Exception {
        mvc.perform(get("/").with(user("ana").roles("OPERADOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/agenda-pagamentos"));

        mvc.perform(get("/agenda-pagamentos").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Agenda de pagamentos"),
                        containsString("href=\"/honorarios\""), containsString("href=\"/repasses\""),
                        not(containsString("Agenda mensal")), not(containsString("Repasses internos")),
                        not(containsString("href=\"/atendimentos\"")))));

        mvc.perform(get("/agenda").with(user("ana").roles("OPERADOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/agenda-pagamentos"));
        mvc.perform(get("/painel").with(user("ana").roles("OPERADOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/agenda-pagamentos"));
    }

    @Test
    void logoutExigeTokenCsrf() throws Exception {
        mvc.perform(post("/logout").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isForbidden());

        mvc.perform(post("/logout").with(user("ana").roles("OPERADOR")).with(csrf()))
                .andExpect(redirectedUrl("/login?saiu"));
    }

    @Test
    void senhaEArmazenadaComoHash() {
        Usuario usuario = criarUsuario("carlos", "senha-segura-123", Perfil.ADMIN);

        Usuario salvo = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(salvo.getSenhaHash())
                .isNotEqualTo("senha-segura-123")
                .startsWith("$2");
    }

    private Usuario criarUsuario(String username, String senha, Perfil perfil) {
        return usuarioRepository.save(new Usuario(username, "Nome " + username, passwordEncoder.encode(senha), perfil));
    }
}
