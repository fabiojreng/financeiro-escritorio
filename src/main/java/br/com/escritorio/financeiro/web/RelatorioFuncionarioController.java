package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.AcertoMensal;
import br.com.escritorio.financeiro.dto.FiltroRelatorioFuncionarios;
import br.com.escritorio.financeiro.dto.ResumoAcertosMes;
import br.com.escritorio.financeiro.dto.SaldoEmprestimo;
import br.com.escritorio.financeiro.dto.ValesFuncionario;
import br.com.escritorio.financeiro.service.FechamentoFuncionarioService;
import br.com.escritorio.financeiro.service.FuncionarioService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.RelatorioFuncionarioService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Relatórios do módulo de funcionários, com permissão própria (FUNCIONARIOS_RELATORIOS): salário e descontos
 * são dados sensíveis e ficam fora dos relatórios gerais.
 */
@Controller
public class RelatorioFuncionarioController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final int MESES_PADRAO_TOTAIS = 12;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RelatorioFuncionarioService relatorioService;
    private final FuncionarioService funcionarioService;
    private final FechamentoFuncionarioService fechamentoService;

    public RelatorioFuncionarioController(RelatorioFuncionarioService relatorioService,
                                          FuncionarioService funcionarioService,
                                          FechamentoFuncionarioService fechamentoService) {
        this.relatorioService = relatorioService;
        this.funcionarioService = funcionarioService;
        this.fechamentoService = fechamentoService;
    }

    @GetMapping("/funcionarios/relatorios")
    public String inicio() {
        return "redirect:/funcionarios/relatorios/extrato";
    }

    @GetMapping("/funcionarios/relatorios/extrato")
    public String extrato(@ModelAttribute("filtro") FiltroRelatorioFuncionarios filtro,
                          BindingResult parametrosInvalidos, Model model) {
        YearMonth mesAtual = completar(filtro);
        LocalDate inicio = filtro.getInicioEfetivo();
        LocalDate fim = filtro.getFimEfetivo();
        model.addAttribute("extrato", relatorioService.extrato(filtro.getFuncionarioId(), inicio, fim));
        preparar("extrato", filtro, mesAtual, model);
        model.addAttribute("tituloPeriodo", filtro.isPeriodoPersonalizado()
                ? TituloPeriodo.periodo(filtro.getInicioOrdenado(), filtro.getFimOrdenado())
                : TituloPeriodo.mes(filtro.getMes()));
        return "funcionarios/relatorio";
    }

    @GetMapping("/funcionarios/relatorios/vales")
    public String vales(@ModelAttribute("filtro") FiltroRelatorioFuncionarios filtro,
                        BindingResult parametrosInvalidos, Model model) {
        YearMonth mesAtual = YearMonth.from(relatorioHoje());
        filtro.setInicio(null);
        filtro.setFim(null);
        if (filtro.getMes() == null) {
            filtro.setMes(mesAtual);
        }
        List<ValesFuncionario> vales = relatorioService.valesDoMes(filtro.getMes());
        model.addAttribute("vales", vales);
        model.addAttribute("totalVales", vales.stream()
                .map(ValesFuncionario::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        preparar("vales", filtro, mesAtual, model);
        model.addAttribute("tituloPeriodo", TituloPeriodo.mes(filtro.getMes()));
        return "funcionarios/relatorio";
    }

    @GetMapping("/funcionarios/relatorios/emprestimos")
    public String emprestimos(@ModelAttribute("filtro") FiltroRelatorioFuncionarios filtro,
                              BindingResult parametrosInvalidos, Model model) {
        YearMonth mesAtual = YearMonth.from(relatorioHoje());
        List<SaldoEmprestimo> emprestimos = relatorioService.emprestimosComSaldo();
        model.addAttribute("emprestimos", emprestimos);
        model.addAttribute("totalSaldo", emprestimos.stream()
                .map(SaldoEmprestimo::saldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        preparar("emprestimos", filtro, mesAtual, model);
        model.addAttribute("tituloPeriodo", "Saldo em " + DATA.format(relatorioHoje()));
        return "funcionarios/relatorio";
    }

    @GetMapping("/funcionarios/relatorios/totais")
    public String totais(@ModelAttribute("filtro") FiltroRelatorioFuncionarios filtro,
                         BindingResult parametrosInvalidos, Model model, RedirectAttributes redirect) {
        YearMonth mesAtual = completarTotais(filtro);
        YearMonth inicio = filtro.getMesInicial(mesAtual, MESES_PADRAO_TOTAIS);
        YearMonth fim = filtro.getMesFinal(mesAtual);
        try {
            List<ResumoAcertosMes> meses = relatorioService.totaisPorMes(inicio, fim);
            model.addAttribute("meses", meses);
            model.addAttribute("totalGeral", meses.stream()
                    .map(ResumoAcertosMes::getTotal)
                    .reduce(AcertoMensal.ZERADO, AcertoMensal::somar));
            model.addAttribute("custoGeral", meses.stream()
                    .map(ResumoAcertosMes::getCusto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/funcionarios/relatorios/totais";
        }
        preparar("totais", filtro, mesAtual, model);
        model.addAttribute("tituloPeriodo", tituloMeses(inicio, fim));
        model.addAttribute("linkExcel", "/funcionarios/relatorios/totais.xlsx?inicio=" + inicio.atDay(1)
                + "&fim=" + fim.atEndOfMonth());
        return "funcionarios/relatorio";
    }

    @GetMapping("/funcionarios/relatorios/totais.xlsx")
    public ResponseEntity<byte[]> totaisExcel(@ModelAttribute("filtro") FiltroRelatorioFuncionarios filtro,
                                              BindingResult parametrosInvalidos) {
        YearMonth mesAtual = completarTotais(filtro);
        YearMonth inicio = filtro.getMesInicial(mesAtual, MESES_PADRAO_TOTAIS);
        YearMonth fim = filtro.getMesFinal(mesAtual);
        List<ResumoAcertosMes> meses = relatorioService.totaisPorMes(inicio, fim);
        String periodo = tituloMeses(inicio, fim);
        byte[] arquivo = FuncionariosExcel.gerar(periodo, meses);
        relatorioService.registrarExportacao("Total por mês", periodo,
                meses.stream().mapToInt(m -> m.acertos().size()).sum());
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("funcionarios-" + filtro.sufixoArquivo(inicio, fim) + ".xlsx").build().toString())
                .cacheControl(CacheControl.noStore())
                .body(arquivo);
    }

    private YearMonth completar(FiltroRelatorioFuncionarios filtro) {
        YearMonth mesAtual = YearMonth.from(relatorioHoje());
        if (filtro.getMes() == null) {
            filtro.setMes(mesAtual);
        }
        return mesAtual;
    }

    /** Sem filtro, o total por mês mostra os últimos {@value #MESES_PADRAO_TOTAIS} meses. */
    private YearMonth completarTotais(FiltroRelatorioFuncionarios filtro) {
        YearMonth mesAtual = YearMonth.from(relatorioHoje());
        if (filtro.getMes() == null && !filtro.isPeriodoPersonalizado()) {
            filtro.setInicio(mesAtual.minusMonths(MESES_PADRAO_TOTAIS - 1L).atDay(1));
            filtro.setFim(mesAtual.atEndOfMonth());
        }
        if (filtro.getMes() == null) {
            filtro.setMes(mesAtual);
        }
        return mesAtual;
    }

    private void preparar(String aba, FiltroRelatorioFuncionarios filtro, YearMonth mesAtual, Model model) {
        model.addAttribute("aba", aba);
        model.addAttribute("funcionarios", funcionarioService.listarPorNome());
        model.addAttribute("mesAtual", mesAtual);
        if (filtro.getMes() != null) {
            model.addAttribute("mesAnterior", filtro.getMes().minusMonths(1));
            model.addAttribute("mesSeguinte", filtro.getMes().plusMonths(1));
        }
    }

    private LocalDate relatorioHoje() {
        return fechamentoService.hoje();
    }

    private static String tituloMeses(YearMonth inicio, YearMonth fim) {
        return inicio.equals(fim) ? TituloPeriodo.mes(inicio)
                : "De " + TituloPeriodo.mes(inicio).toLowerCase() + " a " + TituloPeriodo.mes(fim).toLowerCase();
    }
}
