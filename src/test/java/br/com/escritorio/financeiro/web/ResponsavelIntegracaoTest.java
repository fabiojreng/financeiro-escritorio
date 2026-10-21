package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static br.com.escritorio.financeiro.security.UsuariosDeTeste.operador;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ResponsavelIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ResponsavelRepository responsavelRepository;

    @Test
    void operadorCadastraEditaEDesativaResponsavel() throws Exception {
        mvc.perform(post("/responsaveis").with(operador()).with(csrf())
                        .param("nome", "Carla Mendes").param("observacoes", "Atende às terças"))
                .andExpect(redirectedUrl("/responsaveis"));

        Responsavel criado = responsavelRepository.findAll().stream()
                .filter(r -> r.getNome().equals("Carla Mendes")).findFirst().orElseThrow();

        mvc.perform(get("/responsaveis").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Carla Mendes")));

        mvc.perform(post("/responsaveis/{id}", criado.getId()).with(operador()).with(csrf())
                        .param("nome", "Carla Mendes Souza"))
                .andExpect(redirectedUrl("/responsaveis"));

        mvc.perform(post("/responsaveis/{id}/desativar", criado.getId()).with(operador()).with(csrf()))
                .andExpect(redirectedUrl("/responsaveis"));

        Responsavel salvo = responsavelRepository.findById(criado.getId()).orElseThrow();
        assertThat(salvo.getNome()).isEqualTo("Carla Mendes Souza");
        assertThat(salvo.getObservacoes()).isNull();
        assertThat(salvo.isAtivo()).isFalse();
    }

    @Test
    void nomeVazioERejeitado() throws Exception {
        mvc.perform(post("/responsaveis").with(operador()).with(csrf()).param("nome", "  "))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nome"))
                .andExpect(content().string(containsString("Informe o nome do responsável.")));
    }

    @Test
    void nomeDuplicadoERejeitado() throws Exception {
        responsavelRepository.save(new Responsavel("Roberto Lima", null));

        mvc.perform(post("/responsaveis").with(operador()).with(csrf()).param("nome", "roberto lima"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Já existe um responsável com este nome.")));
    }

    @Test
    void cadastroRapidoDevolveOResponsavelCriadoParaSerSelecionado() throws Exception {
        mvc.perform(post("/responsaveis/rapido").with(operador()).with(csrf())
                        .param("nome", "Júlia Prado").param("observacoes", "Parceira externa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Júlia Prado"));

        assertThat(responsavelRepository.findAll())
                .anyMatch(r -> r.getNome().equals("Júlia Prado") && r.isAtivo()
                        && "Parceira externa".equals(r.getObservacoes()));
    }

    @Test
    void cadastroRapidoComNomeRepetidoDevolveErro() throws Exception {
        responsavelRepository.save(new Responsavel("Roberto Lima", null));

        mvc.perform(post("/responsaveis/rapido").with(operador()).with(csrf())
                        .param("nome", "ROBERTO LIMA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Já existe um responsável com este nome."));
    }

    @Test
    void cadastroRapidoExigeLoginECsrf() throws Exception {
        mvc.perform(post("/responsaveis/rapido").with(csrf()).param("nome", "Sem Login"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/responsaveis/rapido").with(operador()).param("nome", "Sem Token"))
                .andExpect(status().isForbidden());

        assertThat(responsavelRepository.findAll())
                .noneMatch(r -> r.getNome().equals("Sem Login") || r.getNome().equals("Sem Token"));
    }
}
