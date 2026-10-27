package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.TipoPericia;
import br.com.escritorio.financeiro.dto.TipoPericiaForm;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.TipoPericiaService;
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
@RequestMapping("/tipos-pericia")
public class TipoPericiaController {

    private static final String VIEW_FORM = "tipos-pericia/form";

    private final TipoPericiaService tipoPericiaService;

    public TipoPericiaController(TipoPericiaService tipoPericiaService) {
        this.tipoPericiaService = tipoPericiaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tipos", tipoPericiaService.listar());
        return "tipos-pericia/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new TipoPericiaForm());
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") TipoPericiaForm form, BindingResult resultado,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            TipoPericia tipo = tipoPericiaService.criar(form);
            redirect.addFlashAttribute("sucesso", "Tipo de perícia " + tipo.getNome() + " cadastrado.");
            return "redirect:/tipos-pericia";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping(value = "/rapido", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<CadastroRapido> criarRapido(@Valid @ModelAttribute("form") TipoPericiaForm form,
                                                      BindingResult resultado) {
        return CadastroRapido.executar(resultado, () -> {
            TipoPericia tipo = tipoPericiaService.criar(form);
            return CadastroRapido.criado(tipo.getId(), tipo.getNome());
        });
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", TipoPericiaForm.de(tipoPericiaService.buscar(id)));
        return VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") TipoPericiaForm form,
                            BindingResult resultado, RedirectAttributes redirect) {
        form.setId(id);
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            TipoPericia tipo = tipoPericiaService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Tipo de perícia " + tipo.getNome() + " atualizado.");
            return "redirect:/tipos-pericia";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        tipoPericiaService.alterarSituacao(id, true);
        redirect.addFlashAttribute("sucesso", "Tipo de perícia ativado.");
        return "redirect:/tipos-pericia";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        tipoPericiaService.alterarSituacao(id, false);
        redirect.addFlashAttribute("sucesso", "Tipo de perícia desativado.");
        return "redirect:/tipos-pericia";
    }
}
