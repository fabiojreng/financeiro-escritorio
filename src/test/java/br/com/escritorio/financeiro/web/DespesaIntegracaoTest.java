package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import br.com.escritorio.financeiro.domain.Despesa;
import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import br.com.escritorio.financeiro.repository.CategoriaDespesaRepository;
import br.com.escritorio.financeiro.repository.DespesaRepository;
import br.com.escritorio.financeiro.repository.FormaPagamentoDespesaRepository;
import br.com.escritorio.financeiro.service.FechamentoDespesaService;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class DespesaIntegracaoTest extends IntegracaoTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Clock relogio;
    @Autowired
    private CategoriaDespesaRepository categoriaRepository;
    @Autowired
    private FormaPagamentoDespesaRepository formaPagamentoRepository;
    @Autowired
    private DespesaRepository despesaRepository;
    @Autowired
    private FechamentoDespesaService fechamentoService;

    private LocalDate hoje;
    private YearMonth mesPassado;
    private CategoriaDespesa categoria;
    private FormaPagamentoDespesa forma;

    @BeforeEach
    void setUp() {
        hoje = LocalDate.now(relogio);
        mesPassado = YearMonth.from(hoje).minusMonths(1);
        categoria = categoriaRepository.save(new CategoriaDespesa("Categoria Despesa Teste", "Histórico padrão teste, ref."));
        forma = formaPagamentoRepository.save(new FormaPagamentoDespesa("PAG TESTE", "Conta de teste"));
    }

    @Test
    void migracaoCriaAsCategoriasEFormasDaPlanilhaDaContabilidade() throws Exception {
        mvc.perform(get("/categorias-despesa").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Energia Eletrica"),
                        containsString("Despesa c/ Energia, ref."), containsString("Imposto Simples Federal"),
                        containsString("Honorários Contábil"))));
        mvc.perform(get("/formas-pagamento-despesa").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("CX ECO"), containsString("DEB CX ECO"),
                        containsString("TRF BB"))));
    }

    @Test
    void lancamentoPelaTelaAparecaNaListaENaPlanilha() throws Exception {
        mvc.perform(get("/despesas/nova").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-texto=\"Histórico padrão teste, ref.\"")));

        mvc.perform(post("/despesas").with(operador()).with(csrf())
                        .param("dataPagamento", mesPassado.atDay(5).toString())
                        .param("categoriaId", categoria.getId().toString())
                        .param("formaPagamentoId", forma.getId().toString())
                        .param("historico", "Histórico padrão teste, ref. 08")
                        .param("numeroDocumento", "NF 123")
                        .param("valor", "1.234,56")
                        .param("fornecedor", "Fornecedor Integração Teste"))
                .andExpect(redirectedUrl("/despesas?mes=" + mesPassado))
                .andExpect(flash().attributeExists("sucesso"));

        Despesa despesa = despesasDoTeste().getFirst();
        assertThat(despesa.getValor()).isEqualByComparingTo("1234.56");

        mvc.perform(get("/despesas").param("mes", mesPassado.toString())
                        .param("fornecedor", "fornecedor integracao").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Fornecedor Integração Teste"),
                        containsString("NF 123"), containsString("1.234,56"))));

        byte[] arquivo = mvc.perform(get("/despesas/planilha.xlsx").param("mes", mesPassado.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition", containsString("contas-pagas-" + mesPassado + ".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook planilha = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            XSSFSheet aba = planilha.getSheet("Contas Pagas");
            boolean encontrou = false;
            for (int i = 1; i <= aba.getLastRowNum(); i++) {
                if ("Fornecedor Integração Teste".equals(aba.getRow(i).getCell(6).getStringCellValue())) {
                    encontrou = true;
                    assertThat(aba.getRow(i).getCell(1).getStringCellValue()).isEqualTo("Categoria Despesa Teste");
                    assertThat(aba.getRow(i).getCell(2).getStringCellValue()).isEqualTo("PAG TESTE");
                    assertThat(aba.getRow(i).getCell(5).getNumericCellValue()).isEqualTo(1234.56);
                }
            }
            assertThat(encontrou).isTrue();
        }
    }

    @Test
    void camposObrigatoriosEDataFuturaSaoValidados() throws Exception {
        mvc.perform(post("/despesas").with(operador()).with(csrf())
                        .param("dataPagamento", hoje.plusDays(1).toString())
                        .param("categoriaId", categoria.getId().toString())
                        .param("formaPagamentoId", forma.getId().toString())
                        .param("historico", "Histórico")
                        .param("valor", "10,00")
                        .param("fornecedor", "Fornecedor Integração Teste"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "dataPagamento"));

        mvc.perform(post("/despesas").with(operador()).with(csrf())
                        .param("dataPagamento", hoje.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "categoriaId", "formaPagamentoId", "historico",
                        "valor", "fornecedor"));
        assertThat(despesasDoTeste()).isEmpty();
    }

    @Test
    void canceladaSaiDaPlanilhaMasContinuaRegistrada() throws Exception {
        Despesa despesa = salvar(mesPassado.atDay(3), "50.00");

        mvc.perform(post("/despesas/{id}/cancelar", despesa.getId()).with(operador()).with(csrf())
                        .param("motivo", "Lançada em duplicidade"))
                .andExpect(redirectedUrl("/despesas/" + despesa.getId()))
                .andExpect(flash().attributeExists("sucesso"));

        assertThat(despesa.isCancelada()).isTrue();
        assertThat(despesaRepository.findById(despesa.getId())).isPresent();
        assertThat(despesaRepository.ativasEntre(mesPassado.atDay(1), mesPassado.atEndOfMonth()))
                .noneMatch(d -> d.getId().equals(despesa.getId()));
        mvc.perform(get("/despesas/{id}", despesa.getId()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Lançada em duplicidade"), containsString("Cancelada"))));
    }

    @Test
    void mesEnviadoFicaTravadoESoAdministradorReabre() throws Exception {
        Despesa despesa = salvar(mesPassado.atDay(10), "80.00");

        mvc.perform(post("/despesas/fechamento/enviado").with(operador()).with(csrf())
                        .param("mes", mesPassado.toString()))
                .andExpect(redirectedUrl("/despesas?mes=" + mesPassado))
                .andExpect(flash().attributeExists("sucesso"));
        assertThat(fechamentoService.isEnviado(mesPassado)).isTrue();

        mvc.perform(get("/despesas").param("mes", mesPassado.toString()).with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString("Enviado à contabilidade"),
                        not(containsString("/despesas/" + despesa.getId() + "/editar")),
                        not(containsString("/despesas/fechamento/reabrir")))));

        mvc.perform(get("/despesas/{id}/editar", despesa.getId()).with(operador()))
                .andExpect(redirectedUrl("/despesas/" + despesa.getId()))
                .andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/despesas/{id}/cancelar", despesa.getId()).with(operador()).with(csrf())
                        .param("motivo", "Tentativa no mês enviado"))
                .andExpect(flash().attributeExists("erro"));
        assertThat(despesa.isAtiva()).isTrue();

        mvc.perform(post("/despesas").with(operador()).with(csrf())
                        .param("dataPagamento", mesPassado.atDay(12).toString())
                        .param("categoriaId", categoria.getId().toString())
                        .param("formaPagamentoId", forma.getId().toString())
                        .param("historico", "Histórico")
                        .param("valor", "10,00")
                        .param("fornecedor", "Fornecedor Integração Teste"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "dataPagamento"));

        mvc.perform(post("/despesas/fechamento/reabrir").with(operador()).with(csrf())
                        .param("mes", mesPassado.toString()).param("motivo", "Faltou uma nota"))
                .andExpect(status().isForbidden());
        assertThat(fechamentoService.isEnviado(mesPassado)).isTrue();

        mvc.perform(get("/despesas").param("mes", mesPassado.toString()).with(admin()))
                .andExpect(content().string(containsString("/despesas/fechamento/reabrir")));
        mvc.perform(post("/despesas/fechamento/reabrir").with(admin()).with(csrf())
                        .param("mes", mesPassado.toString()).param("motivo", "Faltou uma nota"))
                .andExpect(redirectedUrl("/despesas?mes=" + mesPassado))
                .andExpect(flash().attributeExists("sucesso"));
        assertThat(fechamentoService.isEnviado(mesPassado)).isFalse();

        mvc.perform(get("/despesas/{id}/editar", despesa.getId()).with(operador()))
                .andExpect(status().isOk());
    }

    @Test
    void mesAtualAindaNaoPodeSerMarcadoComoEnviado() throws Exception {
        YearMonth mesAtual = YearMonth.from(hoje);

        mvc.perform(post("/despesas/fechamento/enviado").with(operador()).with(csrf())
                        .param("mes", mesAtual.toString()))
                .andExpect(redirectedUrl("/despesas?mes=" + mesAtual))
                .andExpect(flash().attributeExists("erro"));
        assertThat(fechamentoService.isEnviado(mesAtual)).isFalse();
    }

    @Test
    void telasExigemLogin() throws Exception {
        for (String tela : List.of("/despesas", "/despesas/nova", "/categorias-despesa", "/formas-pagamento-despesa",
                "/despesas/planilha.xlsx?mes=" + mesPassado)) {
            mvc.perform(get(tela)).andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void cadastroDeCategoriaRejeitaNomeRepetido() throws Exception {
        mvc.perform(post("/categorias-despesa").with(operador()).with(csrf())
                        .param("nome", "ENERGIA ELETRICA"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nome"));

        mvc.perform(post("/categorias-despesa").with(operador()).with(csrf())
                        .param("nome", "Aluguel Teste").param("historicoPadrao", "Despesa c/ Aluguel, ref."))
                .andExpect(redirectedUrl("/categorias-despesa"));
        assertThat(categoriaRepository.findAll())
                .anyMatch(c -> c.getNome().equals("Aluguel Teste") && "Despesa c/ Aluguel, ref.".equals(c.getHistoricoPadrao()));
    }

    private Despesa salvar(LocalDate data, String valor) {
        return despesaRepository.save(new Despesa(data, categoria, forma, "Histórico teste", new BigDecimal(valor),
                "Fornecedor Integração Teste"));
    }

    private List<Despesa> despesasDoTeste() {
        return despesaRepository.findAll().stream()
                .filter(d -> d.getFornecedor().equals("Fornecedor Integração Teste"))
                .toList();
    }

    private static RequestPostProcessor operador() {
        return user("ana").roles("OPERADOR");
    }

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }
}
