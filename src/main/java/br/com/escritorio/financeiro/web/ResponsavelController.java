package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Responsavel;
import br.com.escritorio.financeiro.dto.ResponsavelForm;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.ResponsavelService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/responsaveis")
public class ResponsavelController {

    private static final String VIEW_FORM = "responsaveis/form";

    private final ResponsavelService responsavelService;

    public ResponsavelController(ResponsavelService responsavelService) {
        this.responsavelService = responsavelService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("responsaveis", responsavelService.listar());
        return "responsaveis/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new ResponsavelForm());
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") ResponsavelForm form, BindingResult resultado,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            Responsavel responsavel = responsavelService.criar(form);
            redirect.addFlashAttribute("sucesso", "Responsável " + responsavel.getNome() + " cadastrado.");
            return "redirect:/responsaveis";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping(value = "/rapido", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<CadastroRapido> criarRapido(@Valid @ModelAttribute("form") ResponsavelForm form,
                                                      BindingResult resultado) {
        return CadastroRapido.executar(resultado, () -> {
            Responsavel responsavel = responsavelService.criar(form);
            return CadastroRapido.criado(responsavel.getId(), responsavel.getNome());
        });
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", ResponsavelForm.de(responsavelService.buscar(id)));
        return VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ResponsavelForm form,
                            BindingResult resultado, RedirectAttributes redirect) {
        form.setId(id);
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            Responsavel responsavel = responsavelService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Responsável " + responsavel.getNome() + " atualizado.");
            return "redirect:/responsaveis";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        responsavelService.alterarSituacao(id, true);
        redirect.addFlashAttribute("sucesso", "Responsável ativado.");
        return "redirect:/responsaveis";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        responsavelService.alterarSituacao(id, false);
        redirect.addFlashAttribute("sucesso", "Responsável desativado.");
        return "redirect:/responsaveis";
    }
}
