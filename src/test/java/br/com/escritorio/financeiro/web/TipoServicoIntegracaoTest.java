package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class TipoServicoIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TipoServicoRepository tipoServicoRepository;

    @Test
    void migracaoCriaOsQuatroTiposIniciais() throws Exception {
        mvc.perform(get("/tipos-servico").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Salário-maternidade"),
                        containsString("Aposentadoria por idade"),
                        containsString("Aposentadoria por incapacidade"),
                        containsString("Outros serviços"))));
    }

    @Test
    void operadorCadastraEDesativaTipo() throws Exception {
        mvc.perform(post("/tipos-servico").with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("nome", "Pensão por morte").param("descricao", "Dependentes do segurado"))
                .andExpect(redirectedUrl("/tipos-servico"));

        TipoServico criado = tipoServicoRepository.findAll().stream()
                .filter(t -> t.getNome().equals("Pensão por morte")).findFirst().orElseThrow();

        mvc.perform(post("/tipos-servico/{id}/desativar", criado.getId()).with(user("ana").roles("OPERADOR")).with(csrf()))
                .andExpect(redirectedUrl("/tipos-servico"));

        assertThat(tipoServicoRepository.findById(criado.getId())).get()
                .extracting(TipoServico::isAtivo).isEqualTo(false);
    }

    @Test
    void nomeRepetidoComOutraCaixaERejeitado() throws Exception {
        mvc.perform(post("/tipos-servico").with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("nome", "SALÁRIO-MATERNIDADE"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nome"))
                .andExpect(content().string(containsString("Já existe um tipo de serviço com este nome.")));
    }

    @Test
    void cadastroRapidoDevolveOTipoCriadoParaSerSelecionado() throws Exception {
        mvc.perform(post("/tipos-servico/rapido").with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("nome", "  BPC/LOAS ").param("descricao", "Benefício assistencial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("BPC/LOAS"));

        assertThat(tipoServicoRepository.findAll())
                .anyMatch(t -> t.getNome().equals("BPC/LOAS") && t.isAtivo()
                        && "Benefício assistencial".equals(t.getDescricao()));
    }

    @Test
    void cadastroRapidoComNomeRepetidoDevolveErro() throws Exception {
        mvc.perform(post("/tipos-servico/rapido").with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("nome", "salário-maternidade"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.id").isEmpty())
                .andExpect(jsonPath("$.erro").value("Já existe um tipo de serviço com este nome."));
    }

    @Test
    void cadastroRapidoSemNomeDevolveErro() throws Exception {
        mvc.perform(post("/tipos-servico/rapido").with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("nome", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe o nome do serviço."));
    }

    @Test
    void tipoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/tipos-servico/{id}/editar", 999999).with(user("ana").roles("OPERADOR")))
                .andExpect(status().isNotFound());
    }
}
