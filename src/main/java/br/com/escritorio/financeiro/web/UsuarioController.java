package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.ModuloPermissao;
import br.com.escritorio.financeiro.domain.Usuario;
import br.com.escritorio.financeiro.dto.UsuarioForm;
import br.com.escritorio.financeiro.service.PerfilService;
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
@PreAuthorize("hasAuthority('ADMIN_USUARIOS')")
public class UsuarioController {

    private static final String VIEW_FORM = "usuarios/form";

    private final UsuarioService usuarioService;
    private final PerfilService perfilService;

    public UsuarioController(UsuarioService usuarioService, PerfilService perfilService) {
        this.usuarioService = usuarioService;
        this.perfilService = perfilService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return "usuarios/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        return exibirFormulario(new UsuarioForm(), null, model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") UsuarioForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return exibirFormulario(form, null, model);
        }
        try {
            Usuario usuario = usuarioService.criar(form);
            redirect.addFlashAttribute("sucesso", "Usuário " + usuario.getNome() + " cadastrado.");
            return "redirect:/usuarios";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormulario(form, null, model);
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscar(id);
        return exibirFormulario(UsuarioForm.de(usuario), usuario, model);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") UsuarioForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        Usuario existente = usuarioService.buscar(id);
        form.setId(id);
        if (resultado.hasErrors()) {
            return exibirFormulario(form, existente, model);
        }
        try {
            Usuario usuario = usuarioService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Usuário " + usuario.getNome() + " atualizado.");
            return "redirect:/usuarios";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormulario(form, existente, model);
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

    /** {@code usuario} é o registro em edição (ou null no cadastro), usado para o resumo das permissões atuais. */
    private String exibirFormulario(UsuarioForm form, Usuario usuario, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("perfis", perfilService.listarParaSelecao(usuario == null ? null : usuario.getPerfil().getId()));
        model.addAttribute("modulos", ModuloPermissao.values());
        if (usuario != null) {
            model.addAttribute("perfilAtual", usuario.getPerfil());
            model.addAttribute("permissoesAtuais", usuario.getPerfil().permissoesEfetivas());
        }
        return VIEW_FORM;
    }
}
