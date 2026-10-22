package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.TipoLancamentoFuncionario;
import br.com.escritorio.financeiro.dto.AcertoFuncionario;
import br.com.escritorio.financeiro.dto.FuncionarioForm;
import br.com.escritorio.financeiro.service.EmprestimoFuncionarioService;
import br.com.escritorio.financeiro.service.FechamentoFuncionarioService;
import br.com.escritorio.financeiro.service.FuncionarioService;
import br.com.escritorio.financeiro.service.LancamentoFuncionarioService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Cadastro dos funcionários e ficha mensal: lançamentos da competência, quadro do acerto e empréstimos.
 */
@Controller
@RequestMapping("/funcionarios")
public class FuncionarioController {

    private static final String VIEW_FORM = "funcionarios/form";

    private final FuncionarioService funcionarioService;
    private final LancamentoFuncionarioService lancamentoService;
    private final EmprestimoFuncionarioService emprestimoService;
    private final FechamentoFuncionarioService fechamentoService;

    public FuncionarioController(FuncionarioService funcionarioService, LancamentoFuncionarioService lancamentoService,
                                 EmprestimoFuncionarioService emprestimoService,
                                 FechamentoFuncionarioService fechamentoService) {
        this.funcionarioService = funcionarioService;
        this.lancamentoService = lancamentoService;
        this.emprestimoService = emprestimoService;
        this.fechamentoService = fechamentoService;
    }

    @GetMapping
    public String listar(Model model) {
        YearMonth mesAtual = YearMonth.from(fechamentoService.hoje());
        model.addAttribute("funcionarios", funcionarioService.listar());
        model.addAttribute("mesAtual", mesAtual);
        return "funcionarios/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        FuncionarioForm form = new FuncionarioForm();
        form.setDataAdmissao(fechamentoService.hoje());
        form.setDiaPagamento(5);
        model.addAttribute("form", form);
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") FuncionarioForm form, BindingResult resultado,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            Funcionario funcionario = funcionarioService.criar(form);
            redirect.addFlashAttribute("sucesso", "Funcionário " + funcionario.getNome() + " cadastrado.");
            return "redirect:/funcionarios/" + funcionario.getId();
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @GetMapping("/{id}")
    public String ficha(@PathVariable Long id,
                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                        Model model) {
        Funcionario funcionario = funcionarioService.buscar(id);
        LocalDate hoje = fechamentoService.hoje();
        YearMonth mesAtual = YearMonth.from(hoje);
        YearMonth competencia = mes != null ? mes : mesAtual;
        AcertoFuncionario acerto = fechamentoService.acerto(funcionario, competencia);

        model.addAttribute("funcionario", funcionario);
        model.addAttribute("mes", competencia);
        model.addAttribute("tituloPeriodo", TituloPeriodo.mes(competencia));
        model.addAttribute("mesAnterior", competencia.minusMonths(1));
        model.addAttribute("mesSeguinte", competencia.plusMonths(1));
        model.addAttribute("mesAtual", mesAtual);
        model.addAttribute("lancamentos", lancamentoService.daCompetencia(id, competencia));
        model.addAttribute("acerto", acerto);
        model.addAttribute("trabalhouNoMes", funcionario.trabalhouEm(competencia));
        model.addAttribute("podeLancar", funcionario.isAtivo() && !acerto.isPago()
                && funcionario.trabalhouEm(competencia));
        model.addAttribute("podePagar", !acerto.isPago() && !competencia.isAfter(mesAtual));
        model.addAttribute("emprestimos", emprestimoService.doFuncionario(id));
        model.addAttribute("tipos", TipoLancamentoFuncionario.manuais());
        model.addAttribute("formas", FormaPagamento.values());
        model.addAttribute("hoje", hoje);
        model.addAttribute("historico", funcionarioService.historico(id));
        return "funcionarios/ficha";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", FuncionarioForm.de(funcionarioService.buscar(id)));
        return VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") FuncionarioForm form,
                            BindingResult resultado, RedirectAttributes redirect) {
        funcionarioService.buscar(id);
        form.setId(id);
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            funcionarioService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Cadastro atualizado.");
            return "redirect:/funcionarios/" + id;
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        funcionarioService.alterarSituacao(id, false);
        redirect.addFlashAttribute("sucesso", "Funcionário inativado. Ele não recebe novos lançamentos.");
        return "redirect:/funcionarios/" + id;
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        funcionarioService.alterarSituacao(id, true);
        redirect.addFlashAttribute("sucesso", "Funcionário reativado.");
        return "redirect:/funcionarios/" + id;
    }
}
