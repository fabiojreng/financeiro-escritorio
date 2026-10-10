package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.AtendimentoRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AtendimentoIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private TipoServicoRepository tipoServicoRepository;
    @Autowired
    private ResponsavelRepository responsavelRepository;
    @Autowired
    private AtendimentoRepository atendimentoRepository;

    private Cliente cliente;
    private TipoServico tipo;
    private Responsavel responsavel;

    @BeforeEach
    void setUp() {
        cliente = clienteRepository.save(new Cliente("Cliente Atendimento Teste", LocalDate.of(2026, 1, 5)));
        tipo = tipoServicoRepository.save(new TipoServico("Serviço Atendimento Teste", null));
        responsavel = responsavelRepository.save(new Responsavel("Responsável Atendimento Teste", null));
    }

    @Test
    void abreAtendimentoQueApareceNaFichaDoCliente() throws Exception {
        mvc.perform(post("/atendimentos").with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("clienteId", cliente.getId().toString())
                        .param("tipoServicoId", tipo.getId().toString())
                        .param("responsavelId", responsavel.getId().toString())
                        .param("numeroProcesso", "5001234-56.2026.4.03.6100")
                        .param("dataAbertura", "2026-03-02")
                        .param("situacao", "ABERTO"))
                .andExpect(redirectedUrlPattern("/atendimentos/*"));

        assertThat(atendimentoRepository.findByClienteIdOrderByDataAberturaDescIdDesc(cliente.getId()))
                .singleElement()
                .satisfies(a -> assertThat(a.getNumeroProcesso()).isEqualTo("5001234-56.2026.4.03.6100"));

        mvc.perform(get("/clientes/{id}", cliente.getId()).with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Serviço Atendimento Teste"),
                        containsString("Responsável Atendimento Teste"),
                        containsString("02/03/2026"))));
    }

    @Test
    void tipoInativoNaoApareceEmNovoAtendimentoMasContinuaNoAntigo() throws Exception {
        Atendimento antigo = atendimentoRepository.save(
                new Atendimento(cliente, tipo, responsavel, LocalDate.of(2026, 1, 10)));
        tipo.setAtivo(false);

        mvc.perform(get("/atendimentos/novo").param("clienteId", cliente.getId().toString())
                        .with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Serviço Atendimento Teste"))));

        mvc.perform(get("/atendimentos/{id}/editar", antigo.getId()).with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Serviço Atendimento Teste (inativo)")));
    }

    @Test
    void tipoInativoEnviadoNoFormularioERejeitado() throws Exception {
        tipo.setAtivo(false);

        mvc.perform(post("/atendimentos").with(user("ana").roles("OPERADOR")).with(csrf())
                        .param("clienteId", cliente.getId().toString())
                        .param("tipoServicoId", tipo.getId().toString())
                        .param("responsavelId", responsavel.getId().toString())
                        .param("dataAbertura", "2026-03-02")
                        .param("situacao", "ABERTO"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "tipoServicoId"));
    }

    @Test
    void listaFiltraPorResponsavel() throws Exception {
        Responsavel outro = responsavelRepository.save(new Responsavel("Outro Responsável Filtro", null));
        Cliente outroCliente = clienteRepository.save(new Cliente("Outro Cliente Filtro", LocalDate.of(2026, 1, 5)));
        atendimentoRepository.save(new Atendimento(cliente, tipo, responsavel, LocalDate.of(2026, 2, 1)));
        atendimentoRepository.save(new Atendimento(outroCliente, tipo, outro, LocalDate.of(2026, 2, 2)));

        mvc.perform(get("/atendimentos").param("responsavelId", responsavel.getId().toString())
                        .with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Atendimento Teste"),
                        not(containsString("Outro Cliente Filtro")))));
    }

    @Test
    void listaPesquisaPorNomeDoClienteSemAcento() throws Exception {
        Cliente comAcento = clienteRepository.save(new Cliente("Conceição Pesquisa Atendimento", LocalDate.of(2026, 1, 5)));
        atendimentoRepository.save(new Atendimento(comAcento, tipo, responsavel, LocalDate.of(2026, 2, 1)));

        mvc.perform(get("/atendimentos").param("busca", "conceicao pesquisa").with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Conceição Pesquisa Atendimento")));
    }

    @Test
    void escolhaDeClienteListaClientesAtivos() throws Exception {
        mvc.perform(get("/atendimentos/novo").param("busca", "Cliente Atendimento Teste")
                        .with(user("ana").roles("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Cliente Atendimento Teste"),
                        containsString("clienteId=" + cliente.getId()))));
    }

    @Test
    void bancoImpedeAtendimentoComClienteInexistente() {
        Cliente inexistente = clienteRepository.getReferenceById(-1L);

        assertThatThrownBy(() -> {
            atendimentoRepository.save(new Atendimento(inexistente, tipo, responsavel, LocalDate.of(2026, 2, 1)));
            atendimentoRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
