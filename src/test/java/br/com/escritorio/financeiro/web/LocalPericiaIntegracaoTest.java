package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.LocalPericia;
import br.com.escritorio.financeiro.repository.LocalPericiaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static br.com.escritorio.financeiro.security.UsuariosDeTeste.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class LocalPericiaIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private LocalPericiaRepository localPericiaRepository;

    @Test
    void operadorCadastraEditaEDesativaLocal() throws Exception {
        mvc.perform(post("/locais-pericia").with(operador()).with(csrf())
                        .param("nome", "APS Grajaú").param("endereco", "Rua Belém, 100"))
                .andExpect(redirectedUrl("/locais-pericia"));

        LocalPericia criado = localPericiaRepository.findAll().stream()
                .filter(l -> l.getNome().equals("APS Grajaú")).findFirst().orElseThrow();
        assertThat(criado.getEndereco()).isEqualTo("Rua Belém, 100");

        mvc.perform(get("/locais-pericia").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("APS Grajaú"), containsString("Rua Belém, 100"))));

        mvc.perform(post("/locais-pericia/{id}", criado.getId()).with(operador()).with(csrf())
                        .param("nome", "APS Grajaú - Centro").param("endereco", "Rua Belém, 100"))
                .andExpect(redirectedUrl("/locais-pericia"));
        mvc.perform(post("/locais-pericia/{id}/desativar", criado.getId()).with(operador()).with(csrf()))
                .andExpect(redirectedUrl("/locais-pericia"));

        LocalPericia atualizado = localPericiaRepository.findById(criado.getId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("APS Grajaú - Centro");
        assertThat(atualizado.isAtivo()).isFalse();
    }

    @Test
    void nomeRepetidoComOutraCaixaERejeitado() throws Exception {
        localPericiaRepository.save(new LocalPericia("APS Centro", null));

        mvc.perform(post("/locais-pericia").with(operador()).with(csrf())
                        .param("nome", "aps centro"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nome"))
                .andExpect(content().string(containsString("Já existe um local de perícia com este nome.")));
    }

    @Test
    void cadastroRapidoDevolveOLocalCriadoParaSerSelecionado() throws Exception {
        mvc.perform(post("/locais-pericia/rapido").with(operador()).with(csrf())
                        .param("nome", "  Mangueiral "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Mangueiral"));

        mvc.perform(post("/locais-pericia/rapido").with(operador()).with(csrf())
                        .param("nome", "MANGUEIRAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Já existe um local de perícia com este nome."));
    }

    @Test
    void localInexistenteRetorna404() throws Exception {
        mvc.perform(get("/locais-pericia/{id}/editar", 999999).with(operador()))
                .andExpect(status().isNotFound());
    }
}
