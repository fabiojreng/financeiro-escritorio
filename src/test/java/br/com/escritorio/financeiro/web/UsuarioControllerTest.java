package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.PerfisDeTeste;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.dto.UsuarioForm;
import br.com.escritorio.financeiro.security.SecurityConfig;
import br.com.escritorio.financeiro.service.PerfilService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static br.com.escritorio.financeiro.security.UsuariosDeTeste.administrador;
import static br.com.escritorio.financeiro.security.UsuariosDeTeste.operador;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    private final Perfil perfilAdministrador = PerfisDeTeste.administrador();
    private final Perfil perfilOperador = PerfisDeTeste.operador();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private PerfilService perfilService;

    @BeforeEach
    void setUp() {
        when(perfilService.listarParaSelecao(any())).thenReturn(List.of(perfilAdministrador, perfilOperador));
    }

    @Test
    void operadorNaoAcessaGestaoDeUsuarios() throws Exception {
        mvc.perform(get("/usuarios").with(operador()))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acesso-negado"));

        mvc.perform(post("/usuarios").with(operador()).with(csrf())
                        .param("username", "invasor").param("nome", "Invasor").param("perfilId", "1")
                        .param("senha", "senha-segura-123").param("confirmacaoSenha", "senha-segura-123"))
                .andExpect(status().isForbidden());
        verify(usuarioService, never()).criar(any());
    }

    @Test
    void administradorVeListaComMenuDeAdministracao() throws Exception {
        Usuario maria = new Usuario("maria", "Maria Souza", "hash", perfilOperador);
        when(usuarioService.listar()).thenReturn(List.of(maria));

        mvc.perform(get("/usuarios").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Maria Souza"),
                        containsString("Operador"),
                        containsString("Ativo"),
                        containsString("Histórico de operações"),
                        containsString("/perfis"))));
    }

    @Test
    void formularioDeNovoUsuarioListaOsPerfisDoBanco() throws Exception {
        mvc.perform(get("/usuarios/novo").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Novo usuário"),
                        containsString("Administrador"),
                        containsString("Operador"),
                        containsString("Selecione"))));
    }

    @Test
    void edicaoMostraResumoDasPermissoesDoPerfil() throws Exception {
        Usuario maria = new Usuario("maria", "Maria Souza", "hash", perfilOperador);
        ReflectionTestUtils.setField(maria, "id", 5L);
        when(usuarioService.buscar(5L)).thenReturn(maria);

        mvc.perform(get("/usuarios/5/editar").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Permissões pelo perfil"),
                        containsString("Visualizar a agenda"))));
    }

    @Test
    void cadastroValidoRedirecionaComMensagem() throws Exception {
        when(usuarioService.criar(any(UsuarioForm.class)))
                .thenReturn(new Usuario("maria", "Maria Souza", "hash", perfilOperador));

        mvc.perform(post("/usuarios").with(administrador()).with(csrf())
                        .param("username", "maria").param("nome", "Maria Souza").param("perfilId", "2")
                        .param("senha", "senha-segura-123").param("confirmacaoSenha", "senha-segura-123"))
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("sucesso", "Usuário Maria Souza cadastrado."));
    }

    @Test
    void camposObrigatoriosVaziosVoltamAoFormulario() throws Exception {
        mvc.perform(post("/usuarios").with(administrador()).with(csrf())
                        .param("username", "").param("nome", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("usuarios/form"))
                .andExpect(model().attributeHasFieldErrors("form", "username", "nome", "perfilId"))
                .andExpect(content().string(allOf(
                        containsString("Informe o login."),
                        containsString("Informe o nome."),
                        containsString("Selecione o perfil."))));
        verify(usuarioService, never()).criar(any());
    }

    @Test
    void regraDeNegocioApareceNoCampo() throws Exception {
        when(usuarioService.criar(any(UsuarioForm.class)))
                .thenThrow(new RegraNegocioException("username", "Já existe um usuário com este login."));

        mvc.perform(post("/usuarios").with(administrador()).with(csrf())
                        .param("username", "maria").param("nome", "Maria").param("perfilId", "2")
                        .param("senha", "senha-segura-123").param("confirmacaoSenha", "senha-segura-123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "username"))
                .andExpect(content().string(allOf(
                        containsString("Já existe um usuário com este login."),
                        not(containsString("senha-segura-123")))));
    }

    @Test
    void erroAoDesativarApareceComoMensagem() throws Exception {
        doThrow(new RegraNegocioException("Você não pode desativar o seu próprio usuário."))
                .when(usuarioService).alterarSituacao(eq(1L), eq(false), eq("chefe"));

        mvc.perform(post("/usuarios/1/desativar").with(administrador()).with(csrf()))
                .andExpect(redirectedUrl("/usuarios"))
                .andExpect(flash().attribute("erro", "Você não pode desativar o seu próprio usuário."));
    }
}
