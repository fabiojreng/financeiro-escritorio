package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.LocalPericia;
import br.com.escritorio.financeiro.dto.LocalPericiaForm;
import br.com.escritorio.financeiro.service.LocalPericiaService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
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
@RequestMapping("/locais-pericia")
public class LocalPericiaController {

    private static final String VIEW_FORM = "locais-pericia/form";

    private final LocalPericiaService localPericiaService;

    public LocalPericiaController(LocalPericiaService localPericiaService) {
        this.localPericiaService = localPericiaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("locais", localPericiaService.listar());
        return "locais-pericia/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new LocalPericiaForm());
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") LocalPericiaForm form, BindingResult resultado,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            LocalPericia local = localPericiaService.criar(form);
            redirect.addFlashAttribute("sucesso", "Local " + local.getNome() + " cadastrado.");
            return "redirect:/locais-pericia";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping(value = "/rapido", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<CadastroRapido> criarRapido(@Valid @ModelAttribute("form") LocalPericiaForm form,
                                                      BindingResult resultado) {
        return CadastroRapido.executar(resultado, () -> {
            LocalPericia local = localPericiaService.criar(form);
            return CadastroRapido.criado(local.getId(), local.getNome());
        });
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", LocalPericiaForm.de(localPericiaService.buscar(id)));
        return VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") LocalPericiaForm form,
                            BindingResult resultado, RedirectAttributes redirect) {
        form.setId(id);
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            LocalPericia local = localPericiaService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Local " + local.getNome() + " atualizado.");
            return "redirect:/locais-pericia";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        localPericiaService.alterarSituacao(id, true);
        redirect.addFlashAttribute("sucesso", "Local de perícia ativado.");
        return "redirect:/locais-pericia";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        localPericiaService.alterarSituacao(id, false);
        redirect.addFlashAttribute("sucesso", "Local de perícia desativado.");
        return "redirect:/locais-pericia";
    }
}
