package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.LocalPericia;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.Pericia;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.SituacaoPericia;
import br.com.escritorio.financeiro.domain.TipoPericia;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.LocalPericiaRepository;
import br.com.escritorio.financeiro.repository.PericiaRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoPericiaRepository;
import br.com.escritorio.financeiro.security.UsuariosDeTeste;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;

import static br.com.escritorio.financeiro.security.UsuariosDeTeste.operador;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PericiaIntegracaoTest extends IntegracaoTest {

    private static final String CPF = "529.982.247-25";
    private static final String CPF_DIGITOS = "52998224725";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Clock relogio;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ResponsavelRepository responsavelRepository;
    @Autowired
    private TipoPericiaRepository tipoPericiaRepository;
    @Autowired
    private LocalPericiaRepository localPericiaRepository;
    @Autowired
    private PericiaRepository periciaRepository;

    private LocalDate hoje;
    private Responsavel shirley;
    private TipoPericia tipo;
    private LocalPericia local;

    @BeforeEach
    void setUp() {
        hoje = LocalDate.now(relogio);
        shirley = responsavelRepository.save(new Responsavel("Shirley Perícias", null));
        tipo = tipoPericiaRepository.findAll().stream()
                .filter(t -> t.getNome().equals("Avaliação Médica Pericial BPC")).findFirst().orElseThrow();
        local = localPericiaRepository.save(new LocalPericia("APS Teste Perícias", "Quadra 1"));
    }

    @Test
    void cadastraPericiaComClienteNovoENomeDaMaeEMostraNoDiaCerto() throws Exception {
        LocalDate dia = hoje.plusDays(3);

        mvc.perform(novaPericia("Alleanor Alves Martins Santos", CPF, dia, "09:30")
                        .param("nomeMae", "Raimunda Rodrigues dos Santos"))
                .andExpect(redirectedUrl("/pericias?data=" + dia))
                .andExpect(flash().attribute("sucesso", "Perícia de 'ALLEANOR ALVES MARTINS SANTOS' cadastrada."));

        Cliente cliente = clienteRepository.findByCpf(CPF_DIGITOS).orElseThrow();
        assertThat(cliente.getNome()).isEqualTo("ALLEANOR ALVES MARTINS SANTOS");
        assertThat(cliente.getNomeMae()).isEqualTo("Raimunda Rodrigues dos Santos");

        mvc.perform(get("/pericias").param("periodo", "DIA").param("data", dia.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase(DATA.format(dia)),
                        containsStringIgnoringCase("09:30"),
                        containsStringIgnoringCase("Alleanor Alves Martins Santos"),
                        containsStringIgnoringCase(CPF),
                        containsStringIgnoringCase("Raimunda Rodrigues dos Santos"),
                        containsStringIgnoringCase("Avaliação Médica Pericial BPC"),
                        containsStringIgnoringCase("Shirley Perícias"),
                        containsStringIgnoringCase("APS Teste Perícias"),
                        containsStringIgnoringCase("Agendada"))));

        mvc.perform(get("/pericias").param("periodo", "DIA").param("data", dia.plusDays(1).toString())
                        .with(operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Nenhuma perícia com data neste período."),
                        not(containsStringIgnoringCase("Alleanor Alves Martins Santos")))));
    }

    @Test
    void clienteJaCadastradoSemNomeDaMaeGanhaONomeInformadoNaPericia() throws Exception {
        Cliente cliente = new Cliente("Maria Aparecida Souza", hoje.minusYears(1));
        cliente.setCpf(CPF_DIGITOS);
        clienteRepository.save(cliente);

        mvc.perform(novaPericia("Maria Aparecida Souza", CPF, hoje.plusDays(1), "")
                        .param("clienteId", cliente.getId().toString())
                        .param("nomeMae", "Joana Souza"))
                .andExpect(redirectedUrl("/pericias?data=" + hoje.plusDays(1)));

        assertThat(clienteRepository.findById(cliente.getId()).orElseThrow().getNomeMae()).isEqualTo("Joana Souza");
        assertThat(periciaRepository.findByClienteIdOrderByDataDescIdDesc(cliente.getId())).hasSize(1);

        mvc.perform(get("/clientes/{id}", cliente.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Nome da mãe"),
                        containsStringIgnoringCase("Joana Souza"),
                        containsStringIgnoringCase("Perícias médicas"),
                        containsStringIgnoringCase("Avaliação Médica Pericial BPC"))));
    }

    @Test
    void periciaSemDataFicaNaListaAAgendar() throws Exception {
        mvc.perform(novaPericia("Cliente Sem Data", CPF, null, "", tipo.getId().toString(), ""))
                .andExpect(redirectedUrl("/pericias#a-agendar"));

        mvc.perform(get("/pericias").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("A agendar"),
                        containsStringIgnoringCase("Cliente Sem Data"),
                        containsStringIgnoringCase("Informar data"))));

        mvc.perform(get("/pericias").param("situacao", "CANCELADA").with(operador()))
                .andExpect(content().string(not(containsStringIgnoringCase("Cliente Sem Data"))));
    }

    @Test
    void horarioSemDataETipoEmBrancoSaoRejeitados() throws Exception {
        mvc.perform(novaPericia("Cliente Horário", CPF, null, "10:20"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "horario"))
                .andExpect(content().string(containsStringIgnoringCase("Informe a data para poder informar o horário.")));

        mvc.perform(novaPericia("Cliente Horário", CPF, hoje, "10:20", "", local.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "tipoPericiaId"));

        assertThat(clienteRepository.findByCpf(CPF_DIGITOS)).isEmpty();
    }

    @Test
    void salvarECadastrarOutraVoltaAoFormularioNoMesmoDia() throws Exception {
        LocalDate dia = hoje.plusDays(2);
        mvc.perform(novaPericia("Cliente Em Série", CPF, dia, "08:00").param("acao", "nova"))
                .andExpect(redirectedUrl("/pericias/nova?data=" + dia));

        mvc.perform(get("/pericias/nova").param("data", dia.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Avaliação Médica Pericial BPC"),
                        containsStringIgnoringCase("APS Teste Perícias"))));
    }

    @Test
    void remarcarCriaNovaPericiaELigaAAnterior() throws Exception {
        Pericia anterior = cadastrar(hoje.plusDays(1), LocalTime.of(9, 0));
        LocalDate novaData = hoje.plusDays(8);

        mvc.perform(get("/pericias/{id}/remarcar", anterior.getId()).with(operador()))
                .andExpect(status().isOk());
        mvc.perform(post("/pericias/{id}/remarcar", anterior.getId()).with(operador()).with(csrf())
                        .param("data", novaData.toString())
                        .param("horario", "14:30")
                        .param("localId", local.getId().toString())
                        .param("motivo", "INSS remarcou por falta de perito"))
                .andExpect(status().is3xxRedirection());

        Pericia antiga = periciaRepository.findComDetalhesById(anterior.getId()).orElseThrow();
        assertThat(antiga.getSituacao()).isEqualTo(SituacaoPericia.REMARCADA);
        assertThat(antiga.getMotivoSituacao()).isEqualTo("INSS remarcou por falta de perito");
        Pericia nova = antiga.getRemarcadaPara();
        assertThat(nova.getData()).isEqualTo(novaData);
        assertThat(nova.getHorario()).isEqualTo(LocalTime.of(14, 30));
        assertThat(nova.getSituacao()).isEqualTo(SituacaoPericia.AGENDADA);

        mvc.perform(get("/pericias/{id}", anterior.getId()).with(operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Esta perícia foi remarcada para"),
                        containsStringIgnoringCase(DATA.format(novaData) + " às 14:30"))));
        mvc.perform(get("/pericias/{id}", nova.getId()).with(operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Remarcação da perícia de"),
                        containsStringIgnoringCase(DATA.format(anterior.getData())))));

        mvc.perform(get("/pericias/{id}/editar", anterior.getId()).with(operador()))
                .andExpect(redirectedUrl("/pericias/" + anterior.getId()))
                .andExpect(flash().attribute("erro", "Esta perícia está remarcada e não pode mais ser editada."));
    }

    @Test
    void periciaDeHojeEMarcadaComoRealizada() throws Exception {
        Pericia pericia = cadastrar(hoje, LocalTime.of(7, 30));

        mvc.perform(get("/pericias/{id}", pericia.getId()).with(operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("O que aconteceu?"),
                        containsStringIgnoringCase("Perícia realizada"))));

        mvc.perform(post("/pericias/{id}/realizada", pericia.getId()).with(operador()).with(csrf()))
                .andExpect(redirectedUrl("/pericias/" + pericia.getId()))
                .andExpect(flash().attributeExists("sucesso"));

        assertThat(periciaRepository.findById(pericia.getId()).orElseThrow().getSituacao())
                .isEqualTo(SituacaoPericia.REALIZADA);
    }

    @Test
    void periciaFuturaNaoPodeSerRealizadaMasPodeSerCancelada() throws Exception {
        Pericia pericia = cadastrar(hoje.plusDays(5), null);

        mvc.perform(post("/pericias/{id}/realizada", pericia.getId()).with(operador()).with(csrf()))
                .andExpect(redirectedUrl("/pericias/" + pericia.getId()))
                .andExpect(flash().attribute("erro", "A perícia está marcada para " + DATA.format(pericia.getData())
                        + " e ainda não pode ser realizada."));

        mvc.perform(post("/pericias/{id}/cancelar", pericia.getId()).with(operador()).with(csrf())
                        .param("motivo", "Ok"))
                .andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/pericias/{id}/cancelar", pericia.getId()).with(operador()).with(csrf())
                        .param("motivo", "Cliente desistiu do pedido"))
                .andExpect(flash().attributeExists("sucesso"));

        Pericia cancelada = periciaRepository.findById(pericia.getId()).orElseThrow();
        assertThat(cancelada.getSituacao()).isEqualTo(SituacaoPericia.CANCELADA);
        assertThat(cancelada.getMotivoSituacao()).isEqualTo("Cliente desistiu do pedido");

        mvc.perform(get("/pericias/{id}", pericia.getId()).with(operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Cliente desistiu do pedido"),
                        not(containsStringIgnoringCase("O que aconteceu?")))));
    }

    @Test
    void quemSoEditaNaoMudaASituacao() throws Exception {
        Pericia pericia = cadastrar(hoje, null);
        RequestPostProcessor recepcao = UsuariosDeTeste.comPermissoes("recepcao",
                EnumSet.of(Permissao.PERICIAS_VISUALIZAR, Permissao.PERICIAS_EDITAR));

        mvc.perform(get("/pericias/{id}", pericia.getId()).with(recepcao))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsStringIgnoringCase("O que aconteceu?"))));
        mvc.perform(get("/pericias/{id}/editar", pericia.getId()).with(recepcao)).andExpect(status().isOk());

        for (String acao : List.of("realizada", "falta", "cancelar")) {
            mvc.perform(post("/pericias/{id}/" + acao, pericia.getId()).with(recepcao).with(csrf())
                            .param("motivo", "Tentativa sem permissão"))
                    .andExpect(status().isForbidden());
        }
        assertThat(periciaRepository.findById(pericia.getId()).orElseThrow().isAgendada()).isTrue();
    }

    @Test
    void excluiPericiaAnotadaEmDuplicidade() throws Exception {
        Pericia pericia = cadastrar(hoje.plusDays(2), LocalTime.of(8, 0));
        Long id = pericia.getId();

        mvc.perform(get("/pericias/{id}", id).with(operador()))
                .andExpect(content().string(containsStringIgnoringCase("/pericias/" + id + "/excluir")));

        mvc.perform(post("/pericias/{id}/excluir", id).with(operador()).with(csrf())
                        .param("motivo", "Anotada em duplicidade"))
                .andExpect(redirectedUrl("/pericias?data=" + hoje.plusDays(2)))
                .andExpect(flash().attribute("sucesso",
                        "Perícia de 'CLIENTE PERÍCIA SALVA' excluída. O registro fica na auditoria."));

        assertThat(periciaRepository.findById(id)).isEmpty();
        mvc.perform(get("/pericias").param("periodo", "DIA").param("data", hoje.plusDays(2).toString())
                        .with(operador()))
                .andExpect(content().string(not(containsStringIgnoringCase("Cliente Perícia Salva"))));
    }

    @Test
    void excluirARemarcacaoVoltaAPericiaAnteriorParaAgendada() throws Exception {
        Pericia anterior = cadastrar(hoje.plusDays(1), LocalTime.of(9, 0));
        mvc.perform(post("/pericias/{id}/remarcar", anterior.getId()).with(operador()).with(csrf())
                        .param("data", hoje.plusDays(8).toString()).param("localId", local.getId().toString()))
                .andExpect(status().is3xxRedirection());
        Pericia nova = periciaRepository.findComDetalhesById(anterior.getId()).orElseThrow().getRemarcadaPara();

        mvc.perform(get("/pericias/{id}", anterior.getId()).with(operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Esta perícia já foi remarcada"),
                        not(containsStringIgnoringCase("/pericias/" + anterior.getId() + "/excluir")))));
        mvc.perform(post("/pericias/{id}/excluir", anterior.getId()).with(operador()).with(csrf())
                        .param("motivo", "Tentando excluir a antiga"))
                .andExpect(redirectedUrl("/pericias/" + anterior.getId()))
                .andExpect(flash().attributeExists("erro"));

        mvc.perform(post("/pericias/{id}/excluir", nova.getId()).with(operador()).with(csrf())
                        .param("motivo", "Remarcação lançada por engano"))
                .andExpect(redirectedUrl("/pericias?data=" + hoje.plusDays(8)));

        Pericia restaurada = periciaRepository.findComDetalhesById(anterior.getId()).orElseThrow();
        assertThat(restaurada.getSituacao()).isEqualTo(SituacaoPericia.AGENDADA);
        assertThat(restaurada.getRemarcadaPara()).isNull();
        assertThat(periciaRepository.findById(nova.getId())).isEmpty();
    }

    @Test
    void quemNaoTemPermissaoNaoExcluiPericia() throws Exception {
        Pericia pericia = cadastrar(hoje, null);
        RequestPostProcessor recepcao = UsuariosDeTeste.comPermissoes("recepcao",
                EnumSet.of(Permissao.PERICIAS_VISUALIZAR, Permissao.PERICIAS_EDITAR));

        mvc.perform(get("/pericias/{id}", pericia.getId()).with(recepcao))
                .andExpect(content().string(not(containsStringIgnoringCase("Excluir perícia"))));
        mvc.perform(post("/pericias/{id}/excluir", pericia.getId()).with(recepcao).with(csrf())
                        .param("motivo", "Anotada em duplicidade"))
                .andExpect(status().isForbidden());
        assertThat(periciaRepository.findById(pericia.getId())).isPresent();
    }

    @Test
    void exportaAAgendaDoDiaEmExcelSemAsPericiasAAgendar() throws Exception {
        LocalDate dia = hoje.plusDays(2);
        Pericia doDia = cadastrar(dia, LocalTime.of(14, 30));
        doDia.setObservacoes("Levar documento original.");
        Cliente semData = clienteRepository.save(new Cliente("Cliente Ainda Sem Data", hoje));
        periciaRepository.save(new Pericia(semData, tipo, shirley));

        mvc.perform(get("/pericias?periodo=DIA&data=" + dia).with(operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("/pericias/planilha.xlsx?periodo=DIA&amp;data=" + dia),
                        containsStringIgnoringCase("Exportar Excel"))));

        byte[] arquivo = mvc.perform(get("/pericias/planilha.xlsx").param("periodo", "DIA")
                        .param("data", dia.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsStringIgnoringCase("agenda-pericias-")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook planilha = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            XSSFSheet aba = planilha.getSheet(AgendaPericiasExcel.ABA);
            assertThat(aba.getRow(1).getCell(0).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(dia);
            assertThat(aba.getRow(1).getCell(1).getStringCellValue()).isEqualTo("14:30");
            assertThat(aba.getRow(1).getCell(2).getStringCellValue()).isEqualTo("CLIENTE PERÍCIA SALVA");
            assertThat(aba.getRow(1).getCell(7).getStringCellValue()).isEqualTo("APS Teste Perícias");
            assertThat(aba.getRow(1).getCell(AgendaPericiasExcel.CABECALHO.indexOf("OBSERVAÇÕES")).getStringCellValue())
                    .isEqualTo("Levar documento original.");
            assertThat(aba.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Total (1 perícia)");
            assertThat(aba.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void filtroDeObservacaoValeParaAsPericiasComDataEAsAAgendar() throws Exception {
        Pericia comData = cadastrar(hoje, LocalTime.of(9, 0));
        comData.setObservacoes("Levar laudo do ortopedista.");
        Pericia semData = new Pericia(clienteRepository.save(new Cliente("Cliente Sem Data Observação", hoje)),
                tipo, shirley);
        semData.setObservacoes("Aguardando LAUDO novo.");
        periciaRepository.save(semData);
        Pericia outra = new Pericia(clienteRepository.save(new Cliente("Cliente Outra Observação", hoje)),
                tipo, shirley);
        outra.setData(hoje);
        periciaRepository.save(outra);

        mvc.perform(get("/pericias").param("periodo", "DIA").param("data", hoje.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Levar laudo do ortopedista."),
                        containsStringIgnoringCase("Aguardando LAUDO novo."),
                        containsStringIgnoringCase("Cliente Outra Observação"))));

        mvc.perform(get("/pericias").param("periodo", "DIA").param("data", hoje.toString())
                        .param("observacao", "laudo").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Cliente Perícia Salva"),
                        containsStringIgnoringCase("Levar laudo do ortopedista."),
                        containsStringIgnoringCase("Cliente Sem Data Observação"),
                        not(containsStringIgnoringCase("Cliente Outra Observação")))));

        mvc.perform(get("/pericias").param("periodo", "DIA").param("data", hoje.toString())
                        .param("observacao", "ortopedista").with(operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Cliente Perícia Salva"),
                        not(containsStringIgnoringCase("Cliente Sem Data Observação")))));
    }

    @Test
    void periciaInexistenteRetorna404() throws Exception {
        mvc.perform(get("/pericias/{id}", 999999).with(operador())).andExpect(status().isNotFound());
    }

    private Pericia cadastrar(LocalDate data, LocalTime horario) {
        Cliente cliente = new Cliente("Cliente Perícia Salva", hoje);
        cliente.setCpf(CPF_DIGITOS);
        clienteRepository.save(cliente);
        Pericia pericia = new Pericia(cliente, tipo, shirley);
        pericia.setData(data);
        pericia.setHorario(horario);
        pericia.setLocal(local);
        return periciaRepository.save(pericia);
    }

    private MockHttpServletRequestBuilder novaPericia(String nome, String cpf, LocalDate data, String horario) {
        return novaPericia(nome, cpf, data, horario, tipo.getId().toString(), local.getId().toString());
    }

    private MockHttpServletRequestBuilder novaPericia(String nome, String cpf, LocalDate data, String horario,
                                                      String tipoPericiaId, String localId) {
        return post("/pericias").with(operador()).with(csrf())
                .param("nomeCliente", nome)
                .param("cpf", cpf)
                .param("tipoPericiaId", tipoPericiaId)
                .param("responsavelId", shirley.getId().toString())
                .param("localId", localId)
                .param("data", data == null ? "" : data.toString())
                .param("horario", horario);
    }
}
