package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.dto.UsuarioForm;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private static final String VIEW_FORM = "usuarios/form";

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("perfis")
    public Perfil[] perfis() {
        return Perfil.values();
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return "usuarios/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new UsuarioForm());
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") UsuarioForm form, BindingResult resultado,
                        RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            Usuario usuario = usuarioService.criar(form);
            redirect.addFlashAttribute("sucesso", "Usuário " + usuario.getNome() + " cadastrado.");
            return "redirect:/usuarios";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", UsuarioForm.de(usuarioService.buscar(id)));
        return VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") UsuarioForm form,
                            BindingResult resultado, RedirectAttributes redirect) {
        form.setId(id);
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            Usuario usuario = usuarioService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Usuário " + usuario.getNome() + " atualizado.");
            return "redirect:/usuarios";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        return alterarSituacao(id, true, auth, redirect);
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        return alterarSituacao(id, false, auth, redirect);
    }

    private String alterarSituacao(Long id, boolean ativo, Authentication auth, RedirectAttributes redirect) {
        try {
            usuarioService.alterarSituacao(id, ativo, auth.getName());
            redirect.addFlashAttribute("sucesso", ativo ? "Usuário ativado." : "Usuário desativado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/usuarios";
    }
}
