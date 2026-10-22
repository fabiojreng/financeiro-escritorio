package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.IntegracaoTest;
import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Auditoria;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.TipoLancamentoFuncionario;
import br.com.escritorio.financeiro.repository.FuncionarioRepository;
import br.com.escritorio.financeiro.repository.LancamentoFuncionarioRepository;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.FechamentoFuncionarioService;
import br.com.escritorio.financeiro.service.FuncionarioService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static br.com.escritorio.financeiro.security.UsuariosDeTeste.administrador;
import static br.com.escritorio.financeiro.security.UsuariosDeTeste.operador;
import static br.com.escritorio.financeiro.security.UsuariosDeTeste.operadorCom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class FuncionarioIntegracaoTest extends IntegracaoTest {

    private static final String NOME = "Funcionária Integração Teste";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Clock relogio;
    @Autowired
    private FuncionarioRepository funcionarioRepository;
    @Autowired
    private LancamentoFuncionarioRepository lancamentoRepository;
    @Autowired
    private FechamentoFuncionarioService fechamentoService;
    @Autowired
    private AuditoriaService auditoriaService;

    private LocalDate hoje;
    private YearMonth mesPassado;

    @BeforeEach
    void setUp() {
        hoje = LocalDate.now(relogio);
        mesPassado = YearMonth.from(hoje).minusMonths(1);
    }

    @Test
    void fluxoCompletoDoCadastroAoAcertoPagoEReaberto() throws Exception {
        Long id = cadastrar();

        lancar(id, TipoLancamentoFuncionario.VALE, "300,00").andExpect(redirectedUrl(ficha(id)));
        lancar(id, TipoLancamentoFuncionario.VALE, "600,00").andExpect(redirectedUrl(ficha(id)));
        lancar(id, TipoLancamentoFuncionario.BONIFICACAO, "200,00").andExpect(redirectedUrl(ficha(id)));
        lancar(id, TipoLancamentoFuncionario.REEMBOLSO, "80,00").andExpect(redirectedUrl(ficha(id)));
        mvc.perform(lancamento(id, TipoLancamentoFuncionario.DESCONTO, "50,00").param("descricao", "Falta não justificada"))
                .andExpect(redirectedUrl(ficha(id)));

        mvc.perform(get("/funcionarios/{id}", id).param("mes", mesPassado.toString()).with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString(NOME), containsString("1.830,00"),
                        containsString("Falta não justificada"), not(containsString("mpréstimo")))));

        mvc.perform(post("/funcionarios/{id}/acerto/pago", id).with(administrador()).with(csrf())
                        .param("mes", mesPassado.toString())
                        .param("dataPagamento", hoje.toString())
                        .param("formaPagamento", "PIX"))
                .andExpect(redirectedUrl(ficha(id)))
                .andExpect(flash().attributeExists("sucesso"));
        assertThat(fechamentoService.isPago(id, mesPassado)).isTrue();
        assertThat(fechamentoService.buscar(id, mesPassado).orElseThrow().getLiquido()).isEqualByComparingTo("1830.00");

        lancar(id, TipoLancamentoFuncionario.VALE, "10,00")
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("já foi pago")));

        mvc.perform(get("/funcionarios/{id}/acerto/demonstrativo", id).param("mes", mesPassado.toString())
                        .with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString(NOME), containsString("1.830,00"),
                        containsString("2.500,00"))));

        mvc.perform(post("/funcionarios/{id}/acerto/reabrir", id).with(administrador()).with(csrf())
                        .param("mes", mesPassado.toString())
                        .param("motivo", "Faltou lançar a hora extra."))
                .andExpect(redirectedUrl(ficha(id)))
                .andExpect(flash().attributeExists("sucesso"));
        assertThat(fechamentoService.isPago(id, mesPassado)).isFalse();
        lancar(id, TipoLancamentoFuncionario.HORA_EXTRA, "100,00").andExpect(redirectedUrl(ficha(id)));

        List<Auditoria> historico = auditoriaService.historico(FuncionarioService.ENTIDADE, id);
        assertThat(historico).extracting(Auditoria::getAcao).contains(AcaoAuditoria.CRIACAO);
    }

    @Test
    void telasDoModuloAbrem() throws Exception {
        Long id = cadastrar();
        mvc.perform(lancamento(id, TipoLancamentoFuncionario.DESCONTO, "50,00").param("descricao", "Falta não justificada"))
                .andExpect(redirectedUrl(ficha(id)));
        Long descontoId = lancamentoRepository.daCompetencia(id, mesPassado.atDay(1)).getFirst().getId();

        List<String> telas = List.of(
                "/funcionarios/novo",
                "/funcionarios/" + id + "/editar",
                "/funcionarios/" + id,
                "/funcionarios/" + id + "/lancamentos/novo?tipo=VALE",
                "/funcionarios/lancamentos/" + descontoId,
                "/funcionarios/lancamentos/" + descontoId + "/editar",
                "/funcionarios/acertos?mes=" + mesPassado,
                "/funcionarios/relatorios/extrato?funcionarioId=" + id + "&inicio=" + mesPassado.atDay(1));
        for (String tela : telas) {
            mvc.perform(get(tela).with(administrador())).andExpect(status().isOk());
        }
    }

    @Test
    void reciboDoValeTrazOValorPorExtenso() throws Exception {
        Long id = cadastrar();
        lancar(id, TipoLancamentoFuncionario.VALE, "1.500,20").andExpect(redirectedUrl(ficha(id)));
        LancamentoFuncionario vale = lancamentoRepository.daCompetencia(id, mesPassado.atDay(1)).getFirst();

        mvc.perform(get("/funcionarios/lancamentos/{id}/recibo", vale.getId()).with(administrador()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(containsString(NOME), containsString("1.500,20"),
                        containsString("mil e quinhentos reais e vinte centavos"))));
    }

    @Test
    void cancelarLancamentoExigeMotivoESoTiraDoAcerto() throws Exception {
        Long id = cadastrar();
        lancar(id, TipoLancamentoFuncionario.VALE, "300,00").andExpect(redirectedUrl(ficha(id)));
        LancamentoFuncionario vale = lancamentoRepository.daCompetencia(id, mesPassado.atDay(1)).getFirst();

        mvc.perform(post("/funcionarios/lancamentos/{id}/cancelar", vale.getId()).with(administrador()).with(csrf())
                        .param("motivo", ""))
                .andExpect(flash().attributeExists("erro"));
        mvc.perform(post("/funcionarios/lancamentos/{id}/cancelar", vale.getId()).with(administrador()).with(csrf())
                        .param("motivo", "Lançado em duplicidade."))
                .andExpect(flash().attributeExists("sucesso"));

        LancamentoFuncionario cancelado = lancamentoRepository.findById(vale.getId()).orElseThrow();
        assertThat(cancelado.isCancelado()).isTrue();
        assertThat(cancelado.getMotivoCancelamento()).isEqualTo("Lançado em duplicidade.");
    }

    @Test
    void relatoriosEExportacaoParaExcel() throws Exception {
        Long id = cadastrar();
        lancar(id, TipoLancamentoFuncionario.VALE, "300,00").andExpect(redirectedUrl(ficha(id)));

        for (String relatorio : List.of("extrato", "vales", "totais")) {
            mvc.perform(get("/funcionarios/relatorios/" + relatorio).param("mes", mesPassado.toString())
                            .with(administrador()))
                    .andExpect(status().isOk());
        }
        mvc.perform(get("/funcionarios/relatorios/vales").param("mes", mesPassado.toString()).with(administrador()))
                .andExpect(content().string(allOf(containsString(NOME), containsString("300,00"))));

        byte[] arquivo = mvc.perform(get("/funcionarios/relatorios/totais.xlsx")
                        .param("inicio", mesPassado.atDay(1).toString())
                        .param("fim", mesPassado.atEndOfMonth().toString())
                        .with(administrador()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        containsString("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition", containsString("funcionarios-" + mesPassado + ".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook planilha = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            XSSFSheet aba = planilha.getSheet(FuncionariosExcel.ABA);
            Row linha = null;
            for (int i = 1; i <= aba.getLastRowNum(); i++) {
                if (NOME.equals(aba.getRow(i).getCell(1).getStringCellValue())) {
                    linha = aba.getRow(i);
                }
            }
            assertThat(linha).isNotNull();
            assertThat(linha.getCell(2).getNumericCellValue()).isEqualTo(2500.0);
            assertThat(linha.getCell(5).getNumericCellValue()).isEqualTo(300.0);
            assertThat(linha.getCell(7).getNumericCellValue()).isEqualTo(2200.0);
            assertThat(linha.getCell(9).getStringCellValue()).isEqualTo("Em aberto");
        }
    }

    @Test
    void operadorNaoAcessaOModuloPorPadrao() throws Exception {
        Long id = cadastrar();

        mvc.perform(get("/funcionarios").with(operador())).andExpect(status().isForbidden());
        mvc.perform(get("/funcionarios/{id}", id).with(operador())).andExpect(status().isForbidden());
        mvc.perform(get("/funcionarios/relatorios/totais.xlsx").with(operador())).andExpect(status().isForbidden());
        mvc.perform(post("/funcionarios").with(operador()).with(csrf()).param("nome", "X"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/clientes").with(operador()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/funcionarios\""))));
    }

    @Test
    void permissoesDoModuloSaoSeparadas() throws Exception {
        Long id = cadastrar();

        mvc.perform(get("/funcionarios").with(operadorCom(Permissao.FUNCIONARIOS_VISUALIZAR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(NOME)));
        mvc.perform(get("/funcionarios/novo").with(operadorCom(Permissao.FUNCIONARIOS_VISUALIZAR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/funcionarios/relatorios/totais").with(operadorCom(Permissao.FUNCIONARIOS_VISUALIZAR)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/funcionarios/{id}/acerto/pago", id).with(csrf())
                        .with(operadorCom(Permissao.FUNCIONARIOS_VISUALIZAR, Permissao.FUNCIONARIOS_EDITAR))
                        .param("mes", mesPassado.toString()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/funcionarios/{id}/acerto/reabrir", id).with(csrf())
                        .with(operadorCom(Permissao.FUNCIONARIOS_VISUALIZAR, Permissao.FUNCIONARIOS_FECHAR))
                        .param("mes", mesPassado.toString()).param("motivo", "Correção de valores."))
                .andExpect(status().isForbidden());
    }

    @Test
    void cpfDuplicadoVoltaParaOFormularioComErro() throws Exception {
        cadastrar("529.982.247-25");

        mvc.perform(post("/funcionarios").with(administrador()).with(csrf())
                        .param("nome", "Outra Pessoa")
                        .param("cpf", "52998224725")
                        .param("dataAdmissao", hoje.minusYears(1).toString())
                        .param("salarioBase", "1.800,00")
                        .param("diaPagamento", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Este CPF já está no cadastro")));
    }

    private Long cadastrar() throws Exception {
        return cadastrar(null);
    }

    private Long cadastrar(String cpf) throws Exception {
        MockHttpServletRequestBuilder requisicao = post("/funcionarios").with(administrador()).with(csrf())
                .param("nome", NOME)
                .param("cargo", "Secretária")
                .param("dataAdmissao", hoje.minusYears(1).toString())
                .param("salarioBase", "2.500,00")
                .param("diaPagamento", "5");
        if (cpf != null) {
            requisicao.param("cpf", cpf);
        }
        mvc.perform(requisicao)
                .andExpect(redirectedUrlPattern("/funcionarios/*"))
                .andExpect(flash().attributeExists("sucesso"));
        Funcionario funcionario = funcionarioRepository.findAllByOrderByNomeAsc().stream()
                .filter(f -> NOME.equals(f.getNome()))
                .findFirst()
                .orElseThrow();
        return funcionario.getId();
    }

    private ResultActions lancar(Long id, TipoLancamentoFuncionario tipo, String valor) throws Exception {
        return mvc.perform(lancamento(id, tipo, valor));
    }

    private MockHttpServletRequestBuilder lancamento(Long id, TipoLancamentoFuncionario tipo, String valor) {
        return post("/funcionarios/{id}/lancamentos", id).with(administrador()).with(csrf())
                .param("tipo", tipo.name())
                .param("data", mesPassado.atDay(5).toString())
                .param("valor", valor)
                .param("formaPagamento", "PIX");
    }

    private String ficha(Long id) {
        return "/funcionarios/" + id + "?mes=" + mesPassado;
    }
}
