package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.TipoServico;
import br.com.escritorio.financeiro.dto.TipoServicoForm;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.TipoServicoService;
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
@RequestMapping("/tipos-servico")
public class TipoServicoController {

    private static final String VIEW_FORM = "tipos-servico/form";

    private final TipoServicoService tipoServicoService;

    public TipoServicoController(TipoServicoService tipoServicoService) {
        this.tipoServicoService = tipoServicoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("tipos", tipoServicoService.listar());
        return "tipos-servico/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new TipoServicoForm());
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") TipoServicoForm form, BindingResult resultado,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            TipoServico tipo = tipoServicoService.criar(form);
            redirect.addFlashAttribute("sucesso", "Tipo de serviço " + tipo.getNome() + " cadastrado.");
            return "redirect:/tipos-servico";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping(value = "/rapido", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<CadastroRapido> criarRapido(@Valid @ModelAttribute("form") TipoServicoForm form,
                                                      BindingResult resultado) {
        return CadastroRapido.executar(resultado, () -> {
            TipoServico tipo = tipoServicoService.criar(form);
            return CadastroRapido.criado(tipo.getId(), tipo.getNome());
        });
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", TipoServicoForm.de(tipoServicoService.buscar(id)));
        return VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") TipoServicoForm form,
                            BindingResult resultado, RedirectAttributes redirect) {
        form.setId(id);
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            TipoServico tipo = tipoServicoService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Tipo de serviço " + tipo.getNome() + " atualizado.");
            return "redirect:/tipos-servico";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        tipoServicoService.alterarSituacao(id, true);
        redirect.addFlashAttribute("sucesso", "Tipo de serviço ativado.");
        return "redirect:/tipos-servico";
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        tipoServicoService.alterarSituacao(id, false);
        redirect.addFlashAttribute("sucesso", "Tipo de serviço desativado.");
        return "redirect:/tipos-servico";
    }
}
