package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.security.Permissoes;
import br.com.escritorio.financeiro.service.FechamentoFuncionarioService;
import br.com.escritorio.financeiro.service.FuncionarioService;
import br.com.escritorio.financeiro.service.LancamentoFuncionarioService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;

/**
 * Acerto mensal dos funcionários: resumo do mês, pagamento, reabertura e demonstrativo para impressão.
 * Reabrir exige a permissão FUNCIONARIOS_REABRIR (regra também no {@code SecurityConfig}).
 */
@Controller
public class AcertoFuncionarioController {

    private final FechamentoFuncionarioService fechamentoService;
    private final FuncionarioService funcionarioService;
    private final LancamentoFuncionarioService lancamentoService;
    private final String nomeEscritorio;

    public AcertoFuncionarioController(FechamentoFuncionarioService fechamentoService,
                                       FuncionarioService funcionarioService,
                                       LancamentoFuncionarioService lancamentoService,
                                       @Value("${app.escritorio.nome}") String nomeEscritorio) {
        this.fechamentoService = fechamentoService;
        this.funcionarioService = funcionarioService;
        this.lancamentoService = lancamentoService;
        this.nomeEscritorio = nomeEscritorio;
    }

    @GetMapping("/funcionarios/acertos")
    public String resumo(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                         Model model) {
        YearMonth mesAtual = YearMonth.from(fechamentoService.hoje());
        YearMonth competencia = mes != null ? mes : mesAtual;
        model.addAttribute("resumo", fechamentoService.resumoDoMes(competencia));
        model.addAttribute("mes", competencia);
        model.addAttribute("tituloPeriodo", TituloPeriodo.mes(competencia));
        model.addAttribute("mesAnterior", competencia.minusMonths(1));
        model.addAttribute("mesSeguinte", competencia.plusMonths(1));
        model.addAttribute("mesAtual", mesAtual);
        return "funcionarios/acertos";
    }

    @PostMapping("/funcionarios/{id}/acerto/pago")
    public String marcarPago(@PathVariable Long id, @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                             LocalDate dataPagamento,
                             @RequestParam(required = false) FormaPagamento formaPagamento,
                             RedirectAttributes redirect) {
        try {
            fechamentoService.marcarPago(id, mes, dataPagamento, formaPagamento);
            redirect.addFlashAttribute("sucesso", "Acerto de " + TituloPeriodo.mes(mes).toLowerCase(Locale.ROOT)
                    + " marcado como pago. Os lançamentos do mês ficaram travados.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/funcionarios/" + id + "?mes=" + mes;
    }

    @PreAuthorize("hasAuthority('FUNCIONARIOS_REABRIR')")
    @PostMapping("/funcionarios/{id}/acerto/reabrir")
    public String reabrir(@PathVariable Long id, @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                          @RequestParam(required = false) String motivo, Authentication auth,
                          RedirectAttributes redirect) {
        try {
            fechamentoService.reabrir(id, mes, motivo, Permissoes.possui(auth, Permissao.FUNCIONARIOS_REABRIR));
            redirect.addFlashAttribute("sucesso", "Acerto reaberto. Corrija os lançamentos e marque como pago de novo.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/funcionarios/" + id + "?mes=" + mes;
    }

    @GetMapping("/funcionarios/{id}/acerto/demonstrativo")
    public String demonstrativo(@PathVariable Long id,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                                Model model) {
        Funcionario funcionario = funcionarioService.buscar(id);
        YearMonth competencia = mes != null ? mes : YearMonth.from(fechamentoService.hoje());
        model.addAttribute("funcionario", funcionario);
        model.addAttribute("acerto", fechamentoService.acerto(funcionario, competencia));
        model.addAttribute("lancamentos", lancamentoService.daCompetencia(id, competencia).stream()
                .filter(l -> l.isAtivo())
                .toList());
        model.addAttribute("mes", competencia);
        model.addAttribute("tituloPeriodo", TituloPeriodo.mes(competencia));
        model.addAttribute("escritorio", nomeEscritorio);
        model.addAttribute("emitidoEm", fechamentoService.hoje());
        return "funcionarios/demonstrativo";
    }
}
