package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.SituacaoRepasse;
import br.com.escritorio.financeiro.domain.TotaisRepasses;
import br.com.escritorio.financeiro.dto.PagamentoRepasseForm;
import br.com.escritorio.financeiro.dto.RepasseForm;
import br.com.escritorio.financeiro.service.AtendimentoService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.RepasseService;
import br.com.escritorio.financeiro.service.ResponsavelService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class RepasseController {

    private final RepasseService repasseService;
    private final AtendimentoService atendimentoService;
    private final ResponsavelService responsavelService;

    public RepasseController(RepasseService repasseService, AtendimentoService atendimentoService,
                             ResponsavelService responsavelService) {
        this.repasseService = repasseService;
        this.atendimentoService = atendimentoService;
        this.responsavelService = responsavelService;
    }

    @GetMapping("/repasses")
    public String listar(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) Long destinatarioId,
                         @RequestParam(required = false) SituacaoRepasse situacao,
                         @RequestParam(defaultValue = "0") int pagina, Model model) {
        List<Repasse> repasses = repasseService.pesquisar(busca, destinatarioId, situacao);
        model.addAttribute("repasses", Paginas.de(repasses, pagina));
        model.addAttribute("totais", TotaisRepasses.de(repasses));
        model.addAttribute("busca", busca);
        model.addAttribute("destinatarioId", destinatarioId);
        model.addAttribute("situacao", situacao);
        model.addAttribute("situacoes", SituacaoRepasse.values());
        model.addAttribute("responsaveis", responsavelService.listar());
        model.addAttribute("hoje", repasseService.hoje());
        return "repasses/lista";
    }

    @GetMapping("/atendimentos/{atendimentoId}/repasses/novo")
    public String novo(@PathVariable Long atendimentoId, Model model) {
        Atendimento atendimento = atendimentoService.buscar(atendimentoId);
        RepasseForm form = new RepasseForm();
        if (atendimento.getResponsavel().isAtivo()) {
            form.setDestinatarioId(atendimento.getResponsavel().getId());
        }
        return exibirFormulario(form, atendimento, model);
    }

    @PostMapping("/atendimentos/{atendimentoId}/repasses")
    public String criar(@PathVariable Long atendimentoId, @Valid @ModelAttribute("form") RepasseForm form,
                        BindingResult resultado, Model model, RedirectAttributes redirect) {
        Atendimento atendimento = atendimentoService.buscar(atendimentoId);
        if (!resultado.hasErrors()) {
            try {
                Repasse repasse = repasseService.criar(atendimentoId, form);
                redirect.addFlashAttribute("sucesso", "Repasse lançado.");
                return "redirect:/repasses/" + repasse.getId();
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirFormulario(form, atendimento, model);
    }

    @GetMapping("/repasses/{id}")
    public String detalhar(@PathVariable Long id, Model model) {
        model.addAttribute("repasse", repasseService.buscar(id));
        model.addAttribute("historico", repasseService.historico(id));
        model.addAttribute("hoje", repasseService.hoje());
        return "repasses/detalhe";
    }

    @GetMapping("/repasses/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Repasse repasse = repasseService.buscar(id);
        if (!repasse.isPendente()) {
            redirect.addFlashAttribute("erro", "Só repasses pendentes podem ser alterados.");
            return "redirect:/repasses/" + id;
        }
        return exibirFormulario(RepasseForm.de(repasse), repasse.getAtendimento(), model);
    }

    @PostMapping("/repasses/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") RepasseForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        Repasse repasse = repasseService.buscar(id);
        form.setId(id);
        if (!resultado.hasErrors()) {
            try {
                repasseService.atualizar(id, form);
                redirect.addFlashAttribute("sucesso", "Repasse atualizado.");
                return "redirect:/repasses/" + id;
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirFormulario(form, repasse.getAtendimento(), model);
    }

    @GetMapping("/repasses/{id}/pagamento")
    public String formularioPagamento(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Repasse repasse = repasseService.buscar(id);
        if (!repasse.isPendente()) {
            redirect.addFlashAttribute("erro", "Só repasses pendentes podem receber pagamento.");
            return "redirect:/repasses/" + id;
        }
        PagamentoRepasseForm form = new PagamentoRepasseForm();
        form.setDataPagamento(repasseService.hoje());
        form.setValorPago(repasse.getValorPrevisto());
        return exibirPagamento(form, repasse, model);
    }

    @PostMapping("/repasses/{id}/pagamento")
    public String registrarPagamento(@PathVariable Long id, @Valid @ModelAttribute("form") PagamentoRepasseForm form,
                                     BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                repasseService.registrarPagamento(id, form);
                redirect.addFlashAttribute("sucesso", "Pagamento do repasse registrado.");
                return "redirect:/repasses/" + id;
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirPagamento(form, repasseService.buscar(id), model);
    }

    @PostMapping("/repasses/{id}/cancelar")
    public String cancelar(@PathVariable Long id, @RequestParam(required = false) String motivo,
                           HttpServletRequest request, RedirectAttributes redirect) {
        try {
            repasseService.cancelar(id, motivo, request.isUserInRole("ADMIN"));
            redirect.addFlashAttribute("sucesso", "Repasse cancelado. Ele continua registrado no histórico.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/repasses/" + id;
    }

    private String exibirFormulario(RepasseForm form, Atendimento atendimento, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("atendimento", atendimento);
        model.addAttribute("responsaveis", responsavelService.listarParaSelecao(form.isNovo() ? null : form.getDestinatarioId()));
        return "repasses/form";
    }

    private String exibirPagamento(PagamentoRepasseForm form, Repasse repasse, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("repasse", repasse);
        return "repasses/pagamento";
    }
}
