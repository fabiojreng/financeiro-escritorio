package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.TipoPericia;
import br.com.escritorio.financeiro.repository.TipoPericiaRepository;
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
class TipoPericiaIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TipoPericiaRepository tipoPericiaRepository;

    @Test
    void migracaoCriaOsTresTiposDoCaderno() throws Exception {
        mvc.perform(get("/tipos-pericia").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Perícia Médica Inicial"),
                        containsString("Avaliação Médica Pericial"),
                        containsString("Avaliação Médica Pericial BPC"))));
    }

    @Test
    void operadorCadastraEDesativaTipo() throws Exception {
        mvc.perform(post("/tipos-pericia").with(operador()).with(csrf())
                        .param("nome", "Perícia de prorrogação").param("descricao", "Pedido de prorrogação"))
                .andExpect(redirectedUrl("/tipos-pericia"));

        TipoPericia criado = tipoPericiaRepository.findAll().stream()
                .filter(t -> t.getNome().equals("Perícia de prorrogação")).findFirst().orElseThrow();

        mvc.perform(post("/tipos-pericia/{id}/desativar", criado.getId()).with(operador()).with(csrf()))
                .andExpect(redirectedUrl("/tipos-pericia"));

        assertThat(tipoPericiaRepository.findById(criado.getId())).get()
                .extracting(TipoPericia::isAtivo).isEqualTo(false);
    }

    @Test
    void nomeRepetidoComOutraCaixaERejeitado() throws Exception {
        mvc.perform(post("/tipos-pericia").with(operador()).with(csrf())
                        .param("nome", "PERÍCIA MÉDICA INICIAL"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nome"))
                .andExpect(content().string(containsString("Já existe um tipo de perícia com este nome.")));
    }

    @Test
    void cadastroRapidoDevolveOTipoCriadoParaSerSelecionado() throws Exception {
        mvc.perform(post("/tipos-pericia/rapido").with(operador()).with(csrf())
                        .param("nome", "  Reabilitação profissional "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Reabilitação profissional"));
    }

    @Test
    void cadastroRapidoSemNomeDevolveErro() throws Exception {
        mvc.perform(post("/tipos-pericia/rapido").with(operador()).with(csrf())
                        .param("nome", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe o nome do tipo de perícia."));
    }
}
