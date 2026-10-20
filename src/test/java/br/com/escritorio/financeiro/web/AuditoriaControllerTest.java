package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.repository.AuditoriaRepository;
import br.com.escritorio.financeiro.security.SecurityConfig;
import br.com.escritorio.financeiro.service.AuditoriaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static br.com.escritorio.financeiro.security.UsuariosDeTeste.administrador;
import static br.com.escritorio.financeiro.security.UsuariosDeTeste.operador;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AuditoriaController.class, InicioController.class})
@Import(SecurityConfig.class)
class AuditoriaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuditoriaService auditoriaService;

    @MockitoBean
    private AuditoriaRepository auditoriaRepository;

    @Test
    void operadorNaoAcessaHistorico() throws Exception {
        mvc.perform(get("/auditoria").with(operador()))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorVeHistorico() throws Exception {
        Auditoria registro = new Auditoria("chefe", AcaoAuditoria.DESATIVACAO, "Usuário", 7L,
                "Usuário 'joao' desativado.");
        when(auditoriaService.listar(any(), anyInt())).thenReturn(new PageImpl<>(List.of(registro)));
        when(auditoriaRepository.listarEntidades()).thenReturn(List.of("Usuário"));

        mvc.perform(get("/auditoria").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Usuário &#39;joao&#39; desativado."),
                        containsString("Desativação"),
                        containsString("Usuário nº 7"))));
    }

    @Test
    void menuDeAdministracaoNaoApareceParaOperador() throws Exception {
        mvc.perform(get("/acesso-negado").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Administração"))));

        mvc.perform(get("/acesso-negado").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Administração")));
    }
}
