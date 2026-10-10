package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.domain.TipoPagamento;
import br.com.escritorio.financeiro.dto.ConfirmacaoLiberacaoForm;
import br.com.escritorio.financeiro.dto.PrevisaoForm;
import br.com.escritorio.financeiro.service.AtendimentoService;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.PrevisaoService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

@Controller
public class PrevisaoController {

    private static final String VIEW_FORM = "previsoes/form";
    private static final String VIEW_CONFIRMAR = "previsoes/confirmar";

    private final PrevisaoService previsaoService;
    private final AtendimentoService atendimentoService;
    private final AuditoriaService auditoriaService;
    private final MessageSource mensagens;

    public PrevisaoController(PrevisaoService previsaoService, AtendimentoService atendimentoService,
                              AuditoriaService auditoriaService, MessageSource mensagens) {
        this.previsaoService = previsaoService;
        this.atendimentoService = atendimentoService;
        this.auditoriaService = auditoriaService;
        this.mensagens = mensagens;
    }

    @GetMapping("/atendimentos/{atendimentoId}/previsoes/nova")
    public String nova(@PathVariable Long atendimentoId, Model model) {
        return exibirFormulario(new PrevisaoForm(), atendimentoService.buscar(atendimentoId), model);
    }

    @PostMapping("/atendimentos/{atendimentoId}/previsoes")
    public String criar(@PathVariable Long atendimentoId, @Valid @ModelAttribute("form") PrevisaoForm form,
                        BindingResult resultado, Model model, RedirectAttributes redirect) {
        Atendimento atendimento = atendimentoService.buscar(atendimentoId);
        if (resultado.hasErrors()) {
            return exibirFormulario(form, atendimento, model);
        }
        try {
            previsaoService.criar(atendimentoId, form);
            redirect.addFlashAttribute("sucesso", "Previsão cadastrada.");
            return "redirect:/atendimentos/" + atendimentoId + "#previsoes";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormulario(form, atendimento, model);
        }
    }

    @GetMapping("/previsoes/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        PrevisaoBeneficio previsao = previsaoService.buscar(id);
        model.addAttribute("previsao", previsao);
        return exibirFormulario(PrevisaoForm.de(previsao), previsao.getAtendimento(), model);
    }

    @PostMapping("/previsoes/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") PrevisaoForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        PrevisaoBeneficio previsao = previsaoService.buscar(id);
        form.setId(id);
        if (previsao.isLiberada()) {
            form.setSituacao(SituacaoPrevisao.LIBERADO);
        }
        if (resultado.hasErrors()) {
            model.addAttribute("previsao", previsao);
            return exibirFormulario(form, previsao.getAtendimento(), model);
        }
        try {
            previsaoService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Previsão atualizada.");
            return "redirect:/atendimentos/" + previsao.getAtendimento().getId() + "#previsoes";
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            model.addAttribute("previsao", previsao);
            return exibirFormulario(form, previsao.getAtendimento(), model);
        }
    }

    @GetMapping("/previsoes/{id}/confirmar")
    public String confirmar(@PathVariable Long id, @RequestParam(required = false) String voltar, Model model) {
        PrevisaoBeneficio previsao = previsaoService.buscar(id);
        ConfirmacaoLiberacaoForm form = previsao.isLiberada()
                ? new ConfirmacaoLiberacaoForm(previsao.getDataEfetiva(), previsao.getValorConfirmado())
                : new ConfirmacaoLiberacaoForm(previsaoService.hoje(), previsao.getValorEstimado());
        return exibirConfirmacao(previsao, form, voltar, model);
    }

    @PostMapping("/previsoes/{id}/confirmar")
    public String confirmarLiberacao(@PathVariable Long id, @Valid @ModelAttribute("form") ConfirmacaoLiberacaoForm form,
                                     BindingResult resultado, @RequestParam(required = false) String voltar,
                                     @RequestParam(defaultValue = "false") boolean rapido,
                                     Model model, RedirectAttributes redirect) {
        PrevisaoBeneficio previsao = previsaoService.buscar(id);
        String destino = Retorno.seguro(voltar, "/atendimentos/" + previsao.getAtendimento().getId());
        if (!resultado.hasErrors()) {
            try {
                previsaoService.confirmarLiberacao(id, form);
                redirect.addFlashAttribute("sucesso", "Liberação de '"
                        + previsao.getAtendimento().getCliente().getNome() + "' confirmada.");
                return "redirect:" + destino;
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        if (rapido) {
            redirect.addFlashAttribute("erro", "Liberação de '" + previsao.getAtendimento().getCliente().getNome()
                    + "' não confirmada: " + textoDosErros(resultado));
            return "redirect:" + destino;
        }
        return exibirConfirmacao(previsao, form, destino, model);
    }

    @PostMapping("/previsoes/{id}/situacao")
    public String alterarSituacao(@PathVariable Long id, @RequestParam SituacaoPrevisao situacao,
                                  @RequestParam(required = false) String voltar, RedirectAttributes redirect) {
        PrevisaoBeneficio previsao = previsaoService.buscar(id);
        String destino = Retorno.seguro(voltar, "/atendimentos/" + previsao.getAtendimento().getId());
        try {
            previsaoService.alterarSituacao(id, situacao);
            redirect.addFlashAttribute("sucesso", "Situação da previsão de '"
                    + previsao.getAtendimento().getCliente().getNome() + "' alterada para " + situacao.getRotulo() + ".");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:" + destino;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/previsoes/{id}/desfazer-liberacao")
    public String desfazerLiberacao(@PathVariable Long id, @RequestParam(required = false) String voltar,
                                    RedirectAttributes redirect) {
        PrevisaoBeneficio previsao = previsaoService.buscar(id);
        String destino = Retorno.seguro(voltar, "/atendimentos/" + previsao.getAtendimento().getId());
        try {
            previsaoService.desfazerLiberacao(id);
            redirect.addFlashAttribute("sucesso", "Liberação desfeita. A previsão voltou para "
                    + SituacaoPrevisao.AGUARDANDO_CONFIRMACAO.getRotulo() + ".");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:" + destino;
    }

    private String exibirFormulario(PrevisaoForm form, Atendimento atendimento, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("atendimento", atendimento);
        model.addAttribute("tiposPagamento", TipoPagamento.values());
        model.addAttribute("situacoes", SituacaoPrevisao.editaveis());
        if (!form.isNovo()) {
            model.addAttribute("historico", auditoriaService.historico(PrevisaoService.ENTIDADE, form.getId()));
        }
        return VIEW_FORM;
    }

    private String exibirConfirmacao(PrevisaoBeneficio previsao, ConfirmacaoLiberacaoForm form, String voltar,
                                     Model model) {
        model.addAttribute("previsao", previsao);
        model.addAttribute("form", form);
        model.addAttribute("voltar", Retorno.seguro(voltar, "/atendimentos/" + previsao.getAtendimento().getId()));
        return VIEW_CONFIRMAR;
    }

    private String textoDosErros(BindingResult resultado) {
        return resultado.getAllErrors().stream()
                .map(erro -> mensagens.getMessage(erro, LocaleContextHolder.getLocale()))
                .collect(Collectors.joining(" "));
    }
}
