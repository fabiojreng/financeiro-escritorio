package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.repository.AuditoriaRepository;
import br.com.escritorio.financeiro.repository.PerfilRepository;
import br.com.escritorio.financeiro.service.PerfilService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static br.com.escritorio.financeiro.security.UsuariosDeTeste.administrador;
import static br.com.escritorio.financeiro.security.UsuariosDeTeste.comPermissoes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PerfilIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PerfilRepository perfilRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Test
    void listaMostraOsPerfisPadraoEOPerfilDoSistema() throws Exception {
        mvc.perform(get("/perfis").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Administrador"), containsString("Operador"), containsString("Sistema"),
                        containsString("/perfis/novo"))));
    }

    @Test
    void formularioAgrupaAsPermissoesPorModulo() throws Exception {
        mvc.perform(get("/perfis/novo").with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("data-marcar-todos=\"AGENDA\""),
                        containsString("value=\"DESPESAS_REABRIR_FECHAMENTO\""),
                        containsString("Reabrir fechamento"),
                        containsString("Gerenciar perfis"))));
    }

    @Test
    void cadastraPerfilComAsPermissoesMarcadas() throws Exception {
        mvc.perform(post("/perfis").with(administrador()).with(csrf())
                        .param("nome", "Recepção Integração")
                        .param("descricao", "Atendimento")
                        .param("permissoes", "AGENDA_VISUALIZAR", "CLIENTES_VISUALIZAR"))
                .andExpect(redirectedUrl("/perfis"))
                .andExpect(flash().attributeExists("sucesso"));

        Perfil criado = perfilRepository.findByNomeIgnoreCase("Recepção Integração").orElseThrow();
        assertThat(criado.getPermissoes())
                .containsExactlyInAnyOrder(Permissao.AGENDA_VISUALIZAR, Permissao.CLIENTES_VISUALIZAR);
        assertThat(auditoriaRepository.findByEntidadeAndEntidadeIdOrderByDataHoraDescIdDesc(
                PerfilService.ENTIDADE, criado.getId()))
                .singleElement()
                .satisfies(registro -> assertThat(registro.getAcao()).isEqualTo(AcaoAuditoria.CRIACAO));
    }

    @Test
    void nomeRepetidoVoltaAoFormulario() throws Exception {
        mvc.perform(post("/perfis").with(administrador()).with(csrf()).param("nome", "operador"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nome"));
    }

    @Test
    void duplicarAbreFormularioComCopiaDasPermissoes() throws Exception {
        Perfil operador = perfilRepository.findByNomeIgnoreCase("Operador").orElseThrow();

        mvc.perform(get("/perfis/{id}/duplicar", operador.getId()).with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("value=\"C&oacute;pia de Operador\""),
                        containsString("name=\"origemId\""))));
    }

    @Test
    void perfilDoSistemaNaoPodeSerInativadoNemPerderPermissoes() throws Exception {
        Perfil administrador = perfilRepository.findFirstBySistemaTrue().orElseThrow();

        mvc.perform(get("/perfis/{id}/editar", administrador.getId()).with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Administrador")));

        mvc.perform(post("/perfis/{id}", administrador.getId()).with(administrador()).with(csrf())
                        .param("nome", "Admin renomeado").param("descricao", "Acesso total")
                        .param("permissoes", "AGENDA_VISUALIZAR"))
                .andExpect(redirectedUrl("/perfis"));
        assertThat(administrador.getNome()).isEqualTo("Administrador");
        assertThat(administrador.permissoesEfetivas()).containsExactlyInAnyOrder(Permissao.values());

        mvc.perform(post("/perfis/{id}/desativar", administrador.getId()).with(administrador()).with(csrf()))
                .andExpect(redirectedUrl("/perfis"))
                .andExpect(flash().attributeExists("erro"));
        assertThat(administrador.isAtivo()).isTrue();
    }

    @Test
    void quemSoGerenciaUsuariosNaoAcessaPerfis() throws Exception {
        mvc.perform(get("/perfis").with(comPermissoes("ana", Permissao.ADMIN_USUARIOS)))
                .andExpect(status().isForbidden());
    }
}
