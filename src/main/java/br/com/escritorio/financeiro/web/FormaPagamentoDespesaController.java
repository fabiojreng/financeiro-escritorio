package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import br.com.escritorio.financeiro.dto.FormaPagamentoDespesaForm;
import br.com.escritorio.financeiro.service.FormaPagamentoDespesaService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/formas-pagamento-despesa")
public class FormaPagamentoDespesaController {

    private static final String VIEW_FORM = "formas-pagamento-despesa/form";

    private final FormaPagamentoDespesaService formaService;

    public FormaPagamentoDespesaController(FormaPagamentoDespesaService formaService) {
        this.formaService = formaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("formas", formaService.listar());
        return "formas-pagamento-despesa/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("form", new FormaPagamentoDespesaForm());
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") FormaPagamentoDespesaForm form, BindingResult resultado,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            FormaPagamentoDespesa forma = formaService.criar(form);
            redirect.addFlashAttribute("sucesso", "Forma de pagamento " + forma.getNome() + " cadastrada.");
            return "redirect:/formas-pagamento-despesa";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", FormaPagamentoDespesaForm.de(formaService.buscar(id)));
        return VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") FormaPagamentoDespesaForm form,
                            BindingResult resultado, RedirectAttributes redirect) {
        form.setId(id);
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            FormaPagamentoDespesa forma = formaService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Forma de pagamento " + forma.getNome() + " atualizada.");
            return "redirect:/formas-pagamento-despesa";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        formaService.alterarSituacao(id, true);
        redirect.addFlashAttribute("sucesso", "Forma de pagamento ativada.");
        return "redirect:/formas-pagamento-despesa";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        formaService.alterarSituacao(id, false);
        redirect.addFlashAttribute("sucesso", "Forma de pagamento desativada.");
        return "redirect:/formas-pagamento-despesa";
    }
}
