package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.EmprestimoFuncionario;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.dto.EmprestimoFuncionarioForm;
import br.com.escritorio.financeiro.service.EmprestimoFuncionarioService;
import br.com.escritorio.financeiro.service.FechamentoFuncionarioService;
import br.com.escritorio.financeiro.service.FuncionarioService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;

/** Empréstimos aos funcionários, com as parcelas descontadas nos acertos mensais. */
@Controller
public class EmprestimoFuncionarioController {

    private static final String VIEW_FORM = "funcionarios/emprestimo-form";

    private final EmprestimoFuncionarioService emprestimoService;
    private final FuncionarioService funcionarioService;
    private final FechamentoFuncionarioService fechamentoService;

    public EmprestimoFuncionarioController(EmprestimoFuncionarioService emprestimoService,
                                           FuncionarioService funcionarioService,
                                           FechamentoFuncionarioService fechamentoService) {
        this.emprestimoService = emprestimoService;
        this.funcionarioService = funcionarioService;
        this.fechamentoService = fechamentoService;
    }

    @GetMapping("/funcionarios/{funcionarioId}/emprestimos/novo")
    public String novo(@PathVariable Long funcionarioId, Model model) {
        LocalDate hoje = emprestimoService.hoje();
        EmprestimoFuncionarioForm form = new EmprestimoFuncionarioForm();
        form.setDataConcessao(hoje);
        form.setNumeroParcelas(1);
        form.setPrimeiraCompetencia(YearMonth.from(hoje));
        return exibirFormulario(funcionarioService.buscar(funcionarioId), form, model);
    }

    @PostMapping("/funcionarios/{funcionarioId}/emprestimos")
    public String criar(@PathVariable Long funcionarioId,
                        @Valid @ModelAttribute("form") EmprestimoFuncionarioForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        Funcionario funcionario = funcionarioService.buscar(funcionarioId);
        if (!resultado.hasErrors()) {
            try {
                EmprestimoFuncionario emprestimo = emprestimoService.criar(funcionarioId, form);
                redirect.addFlashAttribute("sucesso", "Empréstimo lançado para " + funcionario.getNome()
                        + ". As parcelas já entram nos acertos de cada mês.");
                return "redirect:/funcionarios/emprestimos/" + emprestimo.getId();
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirFormulario(funcionario, form, model);
    }

    @GetMapping("/funcionarios/emprestimos/{id}")
    public String detalhar(@PathVariable Long id, Model model) {
        EmprestimoFuncionario emprestimo = emprestimoService.buscar(id);
        model.addAttribute("emprestimo", emprestimo);
        model.addAttribute("saldo", emprestimoService.saldo(emprestimo));
        model.addAttribute("parcelas", emprestimoService.parcelas(id));
        model.addAttribute("pagas", fechamentoService.competenciasPagas(emprestimo.getFuncionario().getId()));
        model.addAttribute("mesAtual", YearMonth.from(emprestimoService.hoje()));
        model.addAttribute("historico", emprestimoService.historico(id));
        return "funcionarios/emprestimo-detalhe";
    }

    @PostMapping("/funcionarios/emprestimos/{id}/quitar")
    public String quitar(@PathVariable Long id,
                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth competencia,
                         RedirectAttributes redirect) {
        try {
            emprestimoService.quitar(id, competencia);
            redirect.addFlashAttribute("sucesso", "Empréstimo quitado. O saldo será descontado numa parcela única.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/funcionarios/emprestimos/" + id;
    }

    @PostMapping("/funcionarios/emprestimos/{id}/cancelar")
    public String cancelar(@PathVariable Long id, @RequestParam(required = false) String motivo,
                           RedirectAttributes redirect) {
        try {
            emprestimoService.cancelar(id, motivo);
            redirect.addFlashAttribute("sucesso",
                    "Empréstimo cancelado. As parcelas saíram dos acertos, mas continuam registradas no histórico.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/funcionarios/emprestimos/" + id;
    }

    private String exibirFormulario(Funcionario funcionario, EmprestimoFuncionarioForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("funcionario", funcionario);
        model.addAttribute("formas", FormaPagamento.values());
        model.addAttribute("hoje", emprestimoService.hoje());
        return VIEW_FORM;
    }
}
