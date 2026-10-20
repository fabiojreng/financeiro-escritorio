package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.ModuloPermissao;
import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.dto.PerfilForm;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.PerfilService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
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
@RequestMapping("/perfis")
@PreAuthorize("hasAuthority('ADMIN_PERFIS')")
public class PerfilController {

    private static final String VIEW_FORM = "perfis/form";

    private final PerfilService perfilService;
    private final AuditoriaService auditoriaService;

    public PerfilController(PerfilService perfilService, AuditoriaService auditoriaService) {
        this.perfilService = perfilService;
        this.auditoriaService = auditoriaService;
    }

    @ModelAttribute("modulos")
    public ModuloPermissao[] modulos() {
        return ModuloPermissao.values();
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("perfis", perfilService.listar());
        model.addAttribute("usuariosPorPerfil", perfilService.usuariosPorPerfil());
        return "perfis/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new PerfilForm());
        return VIEW_FORM;
    }

    @GetMapping("/{id}/duplicar")
    public String duplicar(@PathVariable Long id, Model model) {
        model.addAttribute("form", PerfilForm.copiaDe(perfilService.buscar(id)));
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") PerfilForm form, BindingResult resultado,
                        RedirectAttributes redirect) {
        form.setSistema(false);
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            Perfil perfil = perfilService.criar(form);
            redirect.addFlashAttribute("sucesso", "Perfil " + perfil.getNome() + " cadastrado.");
            return "redirect:/perfis";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return VIEW_FORM;
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        return exibirEdicao(PerfilForm.de(perfilService.buscar(id)), model);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") PerfilForm form,
                            BindingResult resultado, Authentication auth, Model model,
                            RedirectAttributes redirect) {
        Perfil atual = perfilService.buscar(id);
        form.setId(id);
        form.setOrigemId(null);
        form.setSistema(atual.isSistema());
        if (atual.isSistema()) {
            form.setNome(atual.getNome());
            form.setPermissoes(atual.permissoesEfetivas());
        }
        if (resultado.hasErrors()) {
            return exibirEdicao(form, model);
        }
        try {
            Perfil perfil = perfilService.atualizar(id, form, auth.getName());
            redirect.addFlashAttribute("sucesso", "Perfil " + perfil.getNome() + " atualizado. "
                    + "As mudanças valem para os usuários a partir do próximo login.");
            return "redirect:/perfis";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirEdicao(form, model);
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        return alterarSituacao(id, true, redirect);
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        return alterarSituacao(id, false, redirect);
    }

    private String alterarSituacao(Long id, boolean ativo, RedirectAttributes redirect) {
        try {
            perfilService.alterarSituacao(id, ativo);
            redirect.addFlashAttribute("sucesso", ativo ? "Perfil ativado." : "Perfil inativado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/perfis";
    }

    private String exibirEdicao(PerfilForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("historico", auditoriaService.historico(PerfilService.ENTIDADE, form.getId()));
        return VIEW_FORM;
    }
}
