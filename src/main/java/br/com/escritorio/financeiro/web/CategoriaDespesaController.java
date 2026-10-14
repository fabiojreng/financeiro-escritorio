package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import br.com.escritorio.financeiro.dto.CategoriaDespesaForm;
import br.com.escritorio.financeiro.service.CategoriaDespesaService;
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
@RequestMapping("/categorias-despesa")
public class CategoriaDespesaController {

    private static final String VIEW_FORM = "categorias-despesa/form";

    private final CategoriaDespesaService categoriaService;

    public CategoriaDespesaController(CategoriaDespesaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", categoriaService.listar());
        return "categorias-despesa/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("form", new CategoriaDespesaForm());
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") CategoriaDespesaForm form, BindingResult resultado,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            CategoriaDespesa categoria = categoriaService.criar(form);
            redirect.addFlashAttribute("sucesso", "Categoria " + categoria.getNome() + " cadastrada.");
            return "redirect:/categorias-despesa";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", CategoriaDespesaForm.de(categoriaService.buscar(id)));
        return VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") CategoriaDespesaForm form,
                            BindingResult resultado, RedirectAttributes redirect) {
        form.setId(id);
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            CategoriaDespesa categoria = categoriaService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Categoria " + categoria.getNome() + " atualizada.");
            return "redirect:/categorias-despesa";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        categoriaService.alterarSituacao(id, true);
        redirect.addFlashAttribute("sucesso", "Categoria ativada.");
        return "redirect:/categorias-despesa";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        categoriaService.alterarSituacao(id, false);
        redirect.addFlashAttribute("sucesso", "Categoria desativada.");
        return "redirect:/categorias-despesa";
    }
}
