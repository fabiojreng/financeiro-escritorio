package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.repository.AuditoriaRepository;
import br.com.escritorio.financeiro.repository.PerfilRepository;
import br.com.escritorio.financeiro.repository.UsuarioRepository;
import br.com.escritorio.financeiro.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static br.com.escritorio.financeiro.security.UsuariosDeTeste.administrador;
import static br.com.escritorio.financeiro.security.UsuariosDeTeste.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class UsuarioIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void operadorRecebe403NasPaginasAdministrativas() throws Exception {
        mvc.perform(get("/usuarios").with(operador())).andExpect(status().isForbidden());
        mvc.perform(get("/perfis").with(operador())).andExpect(status().isForbidden());
        mvc.perform(get("/auditoria").with(operador())).andExpect(status().isForbidden());
    }

    @Test
    void administradorCadastraUsuarioEOperacaoFicaNaAuditoria() throws Exception {
        Perfil operador = perfilRepository.findByNomeIgnoreCase("Operador").orElseThrow();

        mvc.perform(post("/usuarios").with(administrador()).with(csrf())
                        .param("username", "novo.operador").param("nome", "Novo Operador")
                        .param("perfilId", operador.getId().toString())
                        .param("senha", "senha-segura-123").param("confirmacaoSenha", "senha-segura-123"))
                .andExpect(redirectedUrl("/usuarios"));

        Usuario criado = usuarioRepository.findByUsernameIgnoreCase("novo.operador").orElseThrow();
        assertThat(criado.getPerfil().getId()).isEqualTo(operador.getId());

        List<Auditoria> historico = auditoriaRepository
                .findByEntidadeAndEntidadeIdOrderByDataHoraDescIdDesc(UsuarioService.ENTIDADE, criado.getId());
        assertThat(historico).singleElement().satisfies(registro -> {
            assertThat(registro.getUsuario()).isEqualTo("chefe");
            assertThat(registro.getAcao()).isEqualTo(AcaoAuditoria.CRIACAO);
            assertThat(registro.getDescricao()).doesNotContain("senha-segura-123");
        });
    }

    @Test
    void usuarioInativoNaoAutentica() throws Exception {
        Usuario inativo = new Usuario("inativo", "Usuário Inativo", passwordEncoder.encode("senha-segura-123"),
                perfilRepository.findByNomeIgnoreCase("Operador").orElseThrow());
        inativo.setAtivo(false);
        usuarioRepository.save(inativo);

        mvc.perform(formLogin("/login").user("inativo").password("senha-segura-123"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?erro"));
    }
}
