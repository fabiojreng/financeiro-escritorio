package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static br.com.escritorio.financeiro.security.UsuariosDeTeste.operador;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ClienteIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void cadastraClienteComCpfFormatadoEMostraAFicha() throws Exception {
        mvc.perform(post("/clientes").with(operador()).with(csrf())
                        .param("nome", "Maria Aparecida Teste").param("cpf", "529.982.247-25")
                        .param("telefone", "(11) 98888-7777").param("dataCadastro", "2026-10-01"))
                .andExpect(redirectedUrlPattern("/clientes/*"));

        Cliente salvo = clienteRepository.findByCpf("52998224725").orElseThrow();
        assertThat(salvo.getNome()).isEqualTo("Maria Aparecida Teste");

        mvc.perform(get("/clientes/{id}", salvo.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("529.982.247-25"),
                        containsString("01/10/2026"),
                        containsString("Cliente &#39;Maria Aparecida Teste&#39; cadastrado."))));
    }

    @Test
    void nomeVazioERejeitado() throws Exception {
        mvc.perform(post("/clientes").with(operador()).with(csrf())
                        .param("nome", " ").param("dataCadastro", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nome"))
                .andExpect(content().string(containsString("Informe o nome do cliente.")));
    }

    @Test
    void cpfDuplicadoEBloqueadoComLinkParaOClienteExistente() throws Exception {
        Cliente existente = salvar("Cliente Original Teste", "11144477735");

        mvc.perform(post("/clientes").with(operador()).with(csrf())
                        .param("nome", "Outro Nome").param("cpf", "111.444.777-35").param("dataCadastro", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "cpf"))
                .andExpect(content().string(allOf(
                        containsString("Já existe um cliente cadastrado com este CPF."),
                        containsString("/clientes/" + existente.getId()),
                        containsString("Cliente Original Teste"))));
    }

    @Test
    void clientesSemCpfPodemSerCadastradosVariasVezes() {
        salvar("Sem CPF Um", null);
        salvar("Sem CPF Dois", null);
        clienteRepository.flush();
    }

    @Test
    void bancoImpedeCpfDuplicadoMesmoSemPassarPeloServico() {
        salvar("Primeiro Teste", "12345678909");
        clienteRepository.flush();

        assertThatThrownBy(() -> {
            salvar("Segundo Teste", "12345678909");
            clienteRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void pesquisaPorNomeSemAcentoEPorParteDoCpf() throws Exception {
        salvar("João Acentuação Teste", "52998224725");
        salvar("Pedro Outro Teste", null);

        mvc.perform(get("/clientes").param("busca", "joao acentuacao").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("João Acentuação Teste"),
                        not(containsString("Pedro Outro Teste")))));

        mvc.perform(get("/clientes").param("busca", "982.247").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("João Acentuação Teste")));
    }

    @Test
    void filtroDeSituacaoSeparaAtivosEInativos() throws Exception {
        Cliente inativo = salvar("Inativo Filtro Teste", null);
        inativo.setAtivo(false);

        mvc.perform(get("/clientes").param("busca", "Filtro Teste").with(operador()))
                .andExpect(content().string(not(containsString("Inativo Filtro Teste"))));

        mvc.perform(get("/clientes").param("busca", "Filtro Teste").param("situacao", "INATIVOS")
                        .with(operador()))
                .andExpect(content().string(containsString("Inativo Filtro Teste")));
    }

    @Test
    void dataDeCadastroNoFuturoERejeitada() throws Exception {
        mvc.perform(post("/clientes").with(operador()).with(csrf())
                        .param("nome", "Futuro").param("dataCadastro", LocalDate.now().plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "dataCadastro"));
    }

    private Cliente salvar(String nome, String cpf) {
        Cliente cliente = new Cliente(nome, LocalDate.of(2026, 10, 1));
        cliente.setCpf(cpf);
        return clienteRepository.save(cliente);
    }
}
