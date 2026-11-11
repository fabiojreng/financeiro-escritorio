package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.BeneficioConcedido;
import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.repository.BeneficioConcedidoRepository;
import br.com.escritorio.financeiro.repository.ClienteRepository;
import br.com.escritorio.financeiro.repository.ResponsavelRepository;
import br.com.escritorio.financeiro.repository.TipoServicoRepository;
import br.com.escritorio.financeiro.security.UsuariosDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class BeneficioConcedidoIntegracaoTest extends IntegracaoTest {

    private static final String CPF = "111.444.777-35";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Clock relogio;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private ResponsavelRepository responsavelRepository;
    @Autowired
    private TipoServicoRepository tipoServicoRepository;
    @Autowired
    private BeneficioConcedidoRepository beneficioRepository;

    private YearMonth mes;
    private Responsavel ana;
    private Responsavel bruno;
    private TipoServico maternidade;
    private TipoServico bpc;

    @BeforeEach
    void setUp() {
        mes = YearMonth.from(LocalDate.now(relogio));
        ana = responsavelRepository.save(new Responsavel("Ana Benefício Concedido", null));
        bruno = responsavelRepository.save(new Responsavel("Bruno Benefício Concedido", null));
        maternidade = tipoServicoRepository.save(new TipoServico("Maternidade Concedida Teste", null));
        bpc = tipoServicoRepository.save(new TipoServico("BPC Concedido Teste", null));
    }

    @Test
    void lancaSemCpfEComCpfESoApareceNoMesDaConcessao() throws Exception {
        mvc.perform(lancamento("Cliente Sem CPF Concedido", "", maternidade, ana, mes))
                .andExpect(redirectedUrl("/beneficios-concedidos?mes=" + mes))
                .andExpect(flash().attributeExists("sucesso"));

        Cliente semCpf = clienteRepository.findAll().stream()
                .filter(c -> c.getNome().equalsIgnoreCase("Cliente Sem CPF Concedido"))
                .findFirst().orElseThrow();
        assertThat(semCpf.getCpf()).isNull();

        mvc.perform(lancamento("Cliente Com CPF Concedido", CPF, bpc, bruno, mes))
                .andExpect(redirectedUrl("/beneficios-concedidos?mes=" + mes));
        assertThat(clienteRepository.findByCpf("11144477735")).isPresent();

        mvc.perform(lancamento("Cliente Mês Passado Concedido", "", maternidade, ana, mes.minusMonths(1)))
                .andExpect(redirectedUrl("/beneficios-concedidos?mes=" + mes.minusMonths(1)));

        mvc.perform(get("/beneficios-concedidos").param("mes", mes.toString()).with(UsuariosDeTeste.operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Cliente Sem CPF Concedido"),
                        containsStringIgnoringCase("Cliente Com CPF Concedido"),
                        containsStringIgnoringCase(CPF),
                        containsStringIgnoringCase("Maternidade Concedida Teste"),
                        containsStringIgnoringCase("BPC Concedido Teste"),
                        not(containsStringIgnoringCase("Cliente Mês Passado Concedido")))));

        mvc.perform(get("/beneficios-concedidos").param("mes", mes.minusMonths(1).toString())
                        .with(UsuariosDeTeste.operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Cliente Mês Passado Concedido"),
                        not(containsStringIgnoringCase("Cliente Sem CPF Concedido")))));
    }

    @Test
    void filtraPorNomeSemAcentoPorBeneficioEPorResponsavel() throws Exception {
        mvc.perform(lancamento("João Concessão Teste", "", maternidade, ana, mes));
        mvc.perform(lancamento("Maria Concessão Teste", "", bpc, bruno, mes));

        mvc.perform(get("/beneficios-concedidos").param("mes", mes.toString())
                        .param("busca", "joao concessao").with(UsuariosDeTeste.operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("João Concessão Teste"),
                        not(containsStringIgnoringCase("Maria Concessão Teste")))));

        mvc.perform(get("/beneficios-concedidos").param("mes", mes.toString())
                        .param("tipoServicoId", bpc.getId().toString()).with(UsuariosDeTeste.operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Maria Concessão Teste"),
                        not(containsStringIgnoringCase("João Concessão Teste")))));

        mvc.perform(get("/beneficios-concedidos").param("mes", mes.toString())
                        .param("responsavelId", ana.getId().toString()).with(UsuariosDeTeste.operador()))
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("João Concessão Teste"),
                        containsStringIgnoringCase("de 2 no mês"),
                        not(containsStringIgnoringCase("Maria Concessão Teste")))));
    }

    @Test
    void editaOResponsavelEExcluiComMotivo() throws Exception {
        mvc.perform(lancamento("Cliente Edição Concedido", "", maternidade, ana, mes));
        BeneficioConcedido beneficio = beneficioDoCliente("Cliente Edição Concedido");

        mvc.perform(post("/beneficios-concedidos/{id}", beneficio.getId()).with(UsuariosDeTeste.operador()).with(csrf())
                        .param("nomeCliente", "Cliente Edição Concedido")
                        .param("clienteId", beneficio.getCliente().getId().toString())
                        .param("competencia", mes.toString())
                        .param("tipoServicoId", maternidade.getId().toString())
                        .param("responsavelId", bruno.getId().toString()))
                .andExpect(redirectedUrl("/beneficios-concedidos/" + beneficio.getId()));
        assertThat(beneficio.getResponsavel().getNome()).isEqualTo("Bruno Benefício Concedido");

        mvc.perform(post("/beneficios-concedidos/{id}/excluir", beneficio.getId())
                        .with(UsuariosDeTeste.operador()).with(csrf()).param("motivo", "abc"))
                .andExpect(redirectedUrl("/beneficios-concedidos/" + beneficio.getId()))
                .andExpect(flash().attribute("erro",
                        "Informe o motivo da exclusão (pelo menos 5 caracteres)."));
        assertThat(beneficioRepository.findById(beneficio.getId())).isPresent();

        mvc.perform(post("/beneficios-concedidos/{id}/excluir", beneficio.getId())
                        .with(UsuariosDeTeste.operador()).with(csrf())
                        .param("motivo", "Lançado em duplicidade"))
                .andExpect(redirectedUrl("/beneficios-concedidos?mes=" + mes))
                .andExpect(flash().attributeExists("sucesso"));
        assertThat(beneficioRepository.findById(beneficio.getId())).isEmpty();
    }

    @Test
    void cpfInvalidoNaoCadastraEOAcessoSemPermissaoENegado() throws Exception {
        mvc.perform(lancamento("Cliente CPF Inválido", "111.111.111-11", maternidade, ana, mes))
                .andExpect(status().isOk())
                .andExpect(content().string(containsStringIgnoringCase("CPF inválido")));
        assertThat(clienteRepository.findAll())
                .noneMatch(c -> c.getNome().equalsIgnoreCase("Cliente CPF Inválido"));

        mvc.perform(get("/beneficios-concedidos")
                        .with(UsuariosDeTeste.comPermissoes("sem.beneficio", Permissao.CLIENTES_VISUALIZAR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/beneficios-concedidos").param("mes", mes.toString())
                        .with(UsuariosDeTeste.comPermissoes("so.ve", Permissao.BENEFICIOS_CONCEDIDOS_VISUALIZAR)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsStringIgnoringCase("Exportar Excel"))));
        mvc.perform(get("/beneficios-concedidos/planilha.xlsx").param("mes", mes.toString())
                        .with(UsuariosDeTeste.comPermissoes("so.ve", Permissao.BENEFICIOS_CONCEDIDOS_VISUALIZAR)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/beneficios-concedidos")
                        .with(UsuariosDeTeste.comPermissoes("sem.beneficio", Permissao.BENEFICIOS_CONCEDIDOS_VISUALIZAR))
                        .with(csrf())
                        .param("nomeCliente", "Invasor")
                        .param("competencia", mes.toString())
                        .param("tipoServicoId", maternidade.getId().toString())
                        .param("responsavelId", ana.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportaOMesEOsFiltrosDaTelaEmExcel() throws Exception {
        mvc.perform(lancamento("Cliente Planilha Sem CPF", "", maternidade, ana, mes));
        mvc.perform(lancamento("Cliente Planilha Com CPF", CPF, bpc, bruno, mes));
        mvc.perform(lancamento("Cliente Planilha Outro Mês", "", maternidade, ana, mes.minusMonths(1)));

        mvc.perform(get("/beneficios-concedidos?mes=" + mes + "&tipoServicoId=" + bpc.getId())
                        .with(UsuariosDeTeste.operador()))
                .andExpect(content().string(containsStringIgnoringCase(
                        "/beneficios-concedidos/planilha.xlsx?mes=" + mes + "&amp;tipoServicoId=" + bpc.getId())));

        byte[] doMes = mvc.perform(get("/beneficios-concedidos/planilha.xlsx").param("mes", mes.toString())
                        .with(UsuariosDeTeste.operador()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        containsStringIgnoringCase("beneficios-concedidos-" + mes + ".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook planilha = new XSSFWorkbook(new ByteArrayInputStream(doMes))) {
            XSSFSheet aba = planilha.getSheet(BeneficiosConcedidosExcel.ABA);
            assertThat(aba.getRow(1).getCell(0).getStringCellValue()).isEqualTo("CLIENTE PLANILHA COM CPF");
            assertThat(aba.getRow(1).getCell(1).getStringCellValue()).isEqualTo(CPF);
            assertThat(aba.getRow(1).getCell(2).getStringCellValue()).isEqualTo("BPC Concedido Teste");
            assertThat(aba.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Bruno Benefício Concedido");
            assertThat(aba.getRow(2).getCell(0).getStringCellValue()).isEqualTo("CLIENTE PLANILHA SEM CPF");
            assertThat(aba.getRow(2).getCell(1).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(aba.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Total (2 benefícios)");
            assertThat(aba.getLastRowNum()).isEqualTo(3);
        }

        byte[] filtrado = mvc.perform(get("/beneficios-concedidos/planilha.xlsx").param("mes", mes.toString())
                        .param("tipoServicoId", maternidade.getId().toString())
                        .param("responsavelId", ana.getId().toString())
                        .with(UsuariosDeTeste.operador()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook planilha = new XSSFWorkbook(new ByteArrayInputStream(filtrado))) {
            XSSFSheet aba = planilha.getSheet(BeneficiosConcedidosExcel.ABA);
            assertThat(aba.getRow(1).getCell(0).getStringCellValue()).isEqualTo("CLIENTE PLANILHA SEM CPF");
            assertThat(aba.getRow(2).getCell(0).getStringCellValue()).isEqualTo("Total (1 benefício)");
            assertThat(aba.getLastRowNum()).isEqualTo(2);
        }
    }

    @Test
    void lancaSemResponsavelEApareceNaListaENoDetalhe() throws Exception {
        mvc.perform(post("/beneficios-concedidos").with(UsuariosDeTeste.operador()).with(csrf())
                        .param("nomeCliente", "Cliente Sem Responsável Concedido")
                        .param("competencia", mes.toString())
                        .param("tipoServicoId", maternidade.getId().toString()))
                .andExpect(redirectedUrl("/beneficios-concedidos?mes=" + mes))
                .andExpect(flash().attributeExists("sucesso"));

        BeneficioConcedido beneficio = beneficioDoCliente("Cliente Sem Responsável Concedido");
        assertThat(beneficio.getResponsavel()).isNull();

        mvc.perform(get("/beneficios-concedidos").param("mes", mes.toString()).with(UsuariosDeTeste.operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsStringIgnoringCase("Cliente Sem Responsável Concedido")));
        mvc.perform(get("/beneficios-concedidos/{id}", beneficio.getId()).with(UsuariosDeTeste.operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsStringIgnoringCase("Cliente Sem Responsável Concedido"),
                        containsStringIgnoringCase("—"))));
    }

    private MockHttpServletRequestBuilder lancamento(String nome, String cpf, TipoServico tipo, Responsavel responsavel,
                                                     YearMonth competencia) {
        return post("/beneficios-concedidos").with(UsuariosDeTeste.operador()).with(csrf())
                .param("nomeCliente", nome)
                .param("cpf", cpf)
                .param("competencia", competencia.toString())
                .param("tipoServicoId", tipo.getId().toString())
                .param("responsavelId", responsavel.getId().toString());
    }

    private BeneficioConcedido beneficioDoCliente(String nome) {
        return beneficioRepository.findAll().stream()
                .filter(b -> b.getCliente().getNome().equalsIgnoreCase(nome))
                .findFirst().orElseThrow();
    }
}
