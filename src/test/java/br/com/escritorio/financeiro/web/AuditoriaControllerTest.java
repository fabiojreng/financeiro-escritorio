package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.dto.PainelIndicadores;
import br.com.escritorio.financeiro.repository.AuditoriaRepository;
import br.com.escritorio.financeiro.security.SecurityConfig;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.FechamentoDespesaService;
import br.com.escritorio.financeiro.service.PainelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

    @MockitoBean
    private PainelService painelService;

    @MockitoBean
    private FechamentoDespesaService fechamentoDespesaService;

    @Test
    void operadorNaoAcessaHistorico() throws Exception {
        mvc.perform(get("/auditoria").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorVeHistorico() throws Exception {
        Auditoria registro = new Auditoria("chefe", AcaoAuditoria.DESATIVACAO, "Usuário", 7L,
                "Usuário 'joao' desativado.");
        when(auditoriaService.listar(any(), anyInt())).thenReturn(new PageImpl<>(List.of(registro)));
        when(auditoriaRepository.listarEntidades()).thenReturn(List.of("Usuário"));

        mvc.perform(get("/auditoria").with(user("chefe").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Usuário &#39;joao&#39; desativado."),
                        containsString("Desativação"),
                        containsString("Usuário nº 7"))));
    }

    @Test
    void menuDeAdministracaoNaoApareceParaOperador() throws Exception {
        when(painelService.hoje()).thenReturn(LocalDate.of(2026, 10, 10));
        when(painelService.calcular(any(), any())).thenReturn(new PainelIndicadores(
                new PainelIndicadores.ValoresCliente(0, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0),
                new PainelIndicadores.ValoresEscritorio(BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, 0, BigDecimal.ZERO)));

        mvc.perform(get("/").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Administração"))));

        mvc.perform(get("/").with(user("chefe").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Administração")));
    }
}
