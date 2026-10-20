package br.com.escritorio.financeiro.security;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.repository.PerfilRepository;
import br.com.escritorio.financeiro.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static br.com.escritorio.financeiro.domain.Permissao.AGENDA_EDITAR;
import static br.com.escritorio.financeiro.domain.Permissao.AGENDA_VISUALIZAR;
import static br.com.escritorio.financeiro.domain.Permissao.CLIENTES_EDITAR;
import static br.com.escritorio.financeiro.domain.Permissao.CLIENTES_VISUALIZAR;
import static br.com.escritorio.financeiro.domain.Permissao.DESPESAS_VISUALIZAR;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class SegurancaIntegracaoTest extends IntegracaoTest {

    private static final String SENHA = "senha-segura-123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

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
    void loginCarregaAsPermissoesDoPerfilComoAuthorities() throws Exception {
        criarUsuario("maria", perfil("Operador"));

        mvc.perform(formLogin("/login").user("maria").password(SENHA))
                .andExpect(authenticated().withUsername("maria").withAuthentication(auth -> {
                    List<String> authorities = auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority).toList();
                    assertThat(authorities)
                            .contains(AGENDA_VISUALIZAR.name(), DESPESAS_VISUALIZAR.name())
                            .doesNotContain(Permissao.REPASSES_VISUALIZAR.name(), Permissao.ADMIN_USUARIOS.name())
                            .noneMatch(authority -> authority.startsWith("ROLE_"));
                }))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginComSenhaErradaVoltaComErro() throws Exception {
        criarUsuario("joao", perfil("Operador"));

        mvc.perform(formLogin("/login").user("joao").password("errada"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?erro"));
    }

    @Test
    void administradorVeTodosOsMenusEAcessaAdministracao() throws Exception {
        RequestPostProcessor admin = logado(criarUsuario("carla", perfil("Administrador")));

        mvc.perform(get("/").with(admin)).andExpect(redirectedUrl("/agenda-pagamentos"));
        mvc.perform(get("/agenda-pagamentos").with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("href=\"/honorarios\""), containsString("href=\"/repasses\""),
                        containsString("href=\"/despesas\""), containsString("href=\"/relatorios\""),
                        containsString("href=\"/clientes\""), containsString("href=\"/usuarios\""),
                        containsString("href=\"/perfis\""), containsString("href=\"/auditoria\""),
                        containsString("/agenda-pagamentos/novo"), containsString("Exportar Excel"),
                        containsString("Administrador"))));

        for (String tela : List.of("/repasses", "/relatorios/pagamentos", "/usuarios", "/perfis", "/auditoria")) {
            mvc.perform(get(tela).with(admin)).andExpect(status().isOk());
        }
    }

    @Test
    void operadorNaoVeNemAcessaRepassesRelatoriosEAdministracao() throws Exception {
        RequestPostProcessor operador = logado(criarUsuario("otavio", perfil("Operador")));

        mvc.perform(get("/agenda-pagamentos").with(operador))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Agenda de pagamentos"),
                        containsString("href=\"/honorarios\""), containsString("href=\"/despesas\""),
                        containsString("href=\"/clientes\""), containsString("/agenda-pagamentos/novo"),
                        containsString("Operador"),
                        not(containsString("href=\"/repasses\"")), not(containsString("href=\"/relatorios\"")),
                        not(containsString("Administração")), not(containsString("href=\"/perfis\"")))));

        for (String tela : List.of("/repasses", "/relatorios/pagamentos", "/relatorios/historico",
                "/usuarios", "/perfis", "/perfis/novo", "/auditoria")) {
            mvc.perform(get(tela).with(operador))
                    .andExpect(status().isForbidden())
                    .andExpect(forwardedUrl("/acesso-negado"));
        }
        mvc.perform(post("/despesas/fechamento/reabrir").with(operador).with(csrf())
                        .param("mes", "2026-01").param("motivo", "Tentativa"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/perfis").with(operador).with(csrf()).param("nome", "Invasor"))
                .andExpect(status().isForbidden());
    }

    @Test
    void perfilPersonalizadoSoVeEAcessaOQueFoiLiberado() throws Exception {
        Perfil recepcao = perfilRepository.save(new Perfil("Recepção Teste", "Atendimento no balcão",
                List.of(AGENDA_VISUALIZAR, AGENDA_EDITAR, CLIENTES_VISUALIZAR, CLIENTES_EDITAR)));
        RequestPostProcessor recepcionista = logado(criarUsuario("rita", recepcao));

        mvc.perform(get("/").with(recepcionista)).andExpect(redirectedUrl("/agenda-pagamentos"));
        mvc.perform(get("/agenda-pagamentos").with(recepcionista))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("/agenda-pagamentos/novo"), containsString("href=\"/clientes\""),
                        containsString("Recepção Teste"),
                        not(containsString("Exportar Excel")),
                        not(containsString("href=\"/honorarios\"")), not(containsString("href=\"/despesas\"")),
                        not(containsString("href=\"/responsaveis\"")), not(containsString("Administração")))));
        mvc.perform(get("/clientes").with(recepcionista)).andExpect(status().isOk());
        mvc.perform(get("/clientes/sugestoes").param("termo", "ma").with(recepcionista)).andExpect(status().isOk());

        for (String tela : List.of("/honorarios", "/despesas", "/responsaveis", "/tipos-servico",
                "/categorias-despesa", "/agenda-pagamentos/planilha.xlsx", "/usuarios")) {
            mvc.perform(get(tela).with(recepcionista)).andExpect(status().isForbidden());
        }
    }

    @Test
    void inicioLevaParaAPrimeiraTelaLiberadaPeloPerfil() throws Exception {
        Perfil financeiro = perfilRepository.save(new Perfil("Só despesas", null, List.of(DESPESAS_VISUALIZAR)));
        mvc.perform(get("/").with(logado(criarUsuario("fabio.despesas", financeiro))))
                .andExpect(redirectedUrl("/despesas"));

        Perfil vazio = perfilRepository.save(new Perfil("Sem permissões", null, List.of()));
        RequestPostProcessor semPermissao = logado(criarUsuario("sem.permissao", vazio));
        mvc.perform(get("/").with(semPermissao)).andExpect(redirectedUrl("/acesso-negado"));
        mvc.perform(get("/acesso-negado").with(semPermissao)).andExpect(status().isOk());
    }

    @Test
    void rotaSemRegraEspecificaENegadaAteParaOAdministrador() throws Exception {
        RequestPostProcessor admin = UsuariosDeTeste.administrador();

        mvc.perform(get("/rota-que-ainda-nao-existe").with(admin)).andExpect(status().isForbidden());
        mvc.perform(post("/agenda").with(admin).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void atalhosAntigosContinuamLevandoAAgenda() throws Exception {
        RequestPostProcessor operador = UsuariosDeTeste.operador();

        mvc.perform(get("/agenda").with(operador)).andExpect(redirectedUrl("/agenda-pagamentos"));
        mvc.perform(get("/painel").with(operador)).andExpect(redirectedUrl("/agenda-pagamentos"));
    }

    @Test
    void logoutExigeTokenCsrf() throws Exception {
        mvc.perform(post("/logout").with(UsuariosDeTeste.operador()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/logout").with(UsuariosDeTeste.operador()).with(csrf()))
                .andExpect(redirectedUrl("/login?saiu"));
    }

    @Test
    void senhaEArmazenadaComoHash() {
        Usuario usuario = criarUsuario("carlos", perfil("Administrador"));

        Usuario salvo = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(salvo.getSenhaHash())
                .isNotEqualTo(SENHA)
                .startsWith("$2");
    }

    private Perfil perfil(String nome) {
        return perfilRepository.findByNomeIgnoreCase(nome).orElseThrow();
    }

    private Usuario criarUsuario(String username, Perfil perfil) {
        return usuarioRepository.save(new Usuario(username, "Nome " + username, passwordEncoder.encode(SENHA), perfil));
    }

    private static RequestPostProcessor logado(Usuario usuario) {
        return user(new UsuarioAutenticado(usuario));
    }
}
