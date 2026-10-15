package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.FormaCobranca;
import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.SituacaoParcela;
import br.com.escritorio.financeiro.dto.AcordoForm;
import br.com.escritorio.financeiro.dto.FiltroSaldo;
import br.com.escritorio.financeiro.dto.GerarParcelasForm;
import br.com.escritorio.financeiro.dto.ParcelaForm;
import br.com.escritorio.financeiro.dto.RecebimentoForm;
import br.com.escritorio.financeiro.dto.ResumoHonorarios;
import br.com.escritorio.financeiro.service.AtendimentoService;
import br.com.escritorio.financeiro.service.HonorariosService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.ResponsavelService;
import jakarta.validation.Valid;
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

import java.math.BigDecimal;

@Controller
public class HonorariosController {

    private final HonorariosService honorariosService;
    private final AtendimentoService atendimentoService;
    private final ResponsavelService responsavelService;

    public HonorariosController(HonorariosService honorariosService, AtendimentoService atendimentoService,
                                ResponsavelService responsavelService) {
        this.honorariosService = honorariosService;
        this.atendimentoService = atendimentoService;
        this.responsavelService = responsavelService;
    }

    @GetMapping("/honorarios")
    public String listar(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) Long responsavelId,
                         @RequestParam(defaultValue = "TODOS") FiltroSaldo saldo,
                         @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("acordos", Paginas.de(honorariosService.listar(busca, responsavelId, saldo), pagina));
        model.addAttribute("busca", busca);
        model.addAttribute("responsavelId", responsavelId);
        model.addAttribute("saldo", saldo);
        model.addAttribute("filtrosSaldo", FiltroSaldo.values());
        model.addAttribute("responsaveis", responsavelService.listar());
        return "honorarios/lista";
    }

    @GetMapping("/atendimentos/{atendimentoId}/honorarios/novo")
    public String novoAcordo(@PathVariable Long atendimentoId, Model model) {
        ResumoHonorarios existente = honorariosService.resumoDoAtendimento(atendimentoId);
        if (existente != null) {
            return "redirect:/honorarios/" + existente.acordo().getId();
        }
        return exibirFormularioAcordo(new AcordoForm(), atendimentoService.buscar(atendimentoId), model);
    }

    @PostMapping("/atendimentos/{atendimentoId}/honorarios")
    public String criarAcordo(@PathVariable Long atendimentoId, @Valid @ModelAttribute("form") AcordoForm form,
                              BindingResult resultado, Model model, RedirectAttributes redirect) {
        Atendimento atendimento = atendimentoService.buscar(atendimentoId);
        if (resultado.hasErrors()) {
            return exibirFormularioAcordo(form, atendimento, model);
        }
        try {
            AcordoHonorarios acordo = honorariosService.criarAcordo(atendimentoId, form);
            redirect.addFlashAttribute("sucesso", "Acordo de honorários registrado.");
            return "redirect:/honorarios/" + acordo.getId();
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormularioAcordo(form, atendimento, model);
        }
    }

    @GetMapping("/honorarios/{id}")
    public String detalhar(@PathVariable Long id, Model model) {
        model.addAttribute("resumo", honorariosService.resumo(id));
        model.addAttribute("historico", honorariosService.historico(id));
        model.addAttribute("hoje", honorariosService.hoje());
        return "honorarios/detalhe";
    }

    @GetMapping("/honorarios/{id}/editar")
    public String editarAcordo(@PathVariable Long id, Model model) {
        AcordoHonorarios acordo = honorariosService.buscarAcordo(id);
        return exibirFormularioAcordo(AcordoForm.de(acordo), acordo.getAtendimento(), model);
    }

    @PostMapping("/honorarios/{id}")
    public String atualizarAcordo(@PathVariable Long id, @Valid @ModelAttribute("form") AcordoForm form,
                                  BindingResult resultado, Model model, RedirectAttributes redirect) {
        AcordoHonorarios acordo = honorariosService.buscarAcordo(id);
        form.setId(id);
        if (resultado.hasErrors()) {
            return exibirFormularioAcordo(form, acordo.getAtendimento(), model);
        }
        try {
            honorariosService.atualizarAcordo(id, form);
            redirect.addFlashAttribute("sucesso", "Acordo de honorários atualizado.");
            return "redirect:/honorarios/" + id;
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormularioAcordo(form, acordo.getAtendimento(), model);
        }
    }

    @GetMapping("/honorarios/{id}/parcelas/gerar")
    public String formularioGerarParcelas(@PathVariable Long id, Model model) {
        ResumoHonorarios resumo = honorariosService.resumo(id);
        GerarParcelasForm form = new GerarParcelasForm();
        AcordoHonorarios acordo = resumo.acordo();
        form.setQuantidade(acordo.getQuantidadeParcelas());
        form.setValorTotal(resumo.saldo());
        form.setPrimeiroVencimento(honorariosService.hoje().plusMonths(1));
        return exibirGerarParcelas(form, resumo, model);
    }

    @PostMapping("/honorarios/{id}/parcelas/gerar")
    public String gerarParcelas(@PathVariable Long id, @Valid @ModelAttribute("form") GerarParcelasForm form,
                                BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                int quantidade = honorariosService.gerarParcelas(id, form).size();
                redirect.addFlashAttribute("sucesso", quantidade == 1 ? "Parcela gerada." : quantidade + " parcelas geradas.");
                return "redirect:/honorarios/" + id + "#parcelas";
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirGerarParcelas(form, honorariosService.resumo(id), model);
    }

    @GetMapping("/honorarios/{id}/parcelas/nova")
    public String novaParcela(@PathVariable Long id, Model model) {
        return exibirFormularioParcela(new ParcelaForm(), honorariosService.buscarAcordo(id), model);
    }

    @PostMapping("/honorarios/{id}/parcelas")
    public String adicionarParcela(@PathVariable Long id, @Valid @ModelAttribute("form") ParcelaForm form,
                                   BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return exibirFormularioParcela(form, honorariosService.buscarAcordo(id), model);
        }
        ParcelaHonorario parcela = honorariosService.adicionarParcela(id, form);
        redirect.addFlashAttribute("sucesso", "Parcela " + parcela.getNumero() + " cadastrada.");
        return "redirect:/honorarios/" + id + "#parcelas";
    }

    @GetMapping("/parcelas/{id}/editar")
    public String editarParcela(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        ParcelaHonorario parcela = honorariosService.buscarParcela(id);
        if (parcela.getSituacao() != SituacaoParcela.ABERTA) {
            redirect.addFlashAttribute("erro", "Só parcelas abertas podem ser alteradas.");
            return "redirect:/honorarios/" + parcela.getAcordo().getId() + "#parcelas";
        }
        model.addAttribute("parcela", parcela);
        return exibirFormularioParcela(ParcelaForm.de(parcela), parcela.getAcordo(), model);
    }

    @PostMapping("/parcelas/{id}")
    public String atualizarParcela(@PathVariable Long id, @Valid @ModelAttribute("form") ParcelaForm form,
                                   BindingResult resultado, Model model, RedirectAttributes redirect) {
        ParcelaHonorario parcela = honorariosService.buscarParcela(id);
        form.setId(id);
        if (!resultado.hasErrors()) {
            try {
                honorariosService.atualizarParcela(id, form);
                redirect.addFlashAttribute("sucesso", "Parcela " + parcela.getNumero() + " atualizada.");
                return "redirect:/honorarios/" + parcela.getAcordo().getId() + "#parcelas";
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        model.addAttribute("parcela", parcela);
        return exibirFormularioParcela(form, parcela.getAcordo(), model);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/parcelas/{id}/cancelar")
    public String cancelarParcela(@PathVariable Long id, @RequestParam(required = false) String motivo,
                                  RedirectAttributes redirect) {
        Long acordoId = honorariosService.acordoDaParcela(id);
        try {
            ParcelaHonorario parcela = honorariosService.cancelarParcela(id, motivo);
            redirect.addFlashAttribute("sucesso", "Parcela " + parcela.getNumero() + " cancelada.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/honorarios/" + acordoId + "#parcelas";
    }

    @GetMapping("/honorarios/{id}/recebimentos/novo")
    public String novoRecebimento(@PathVariable Long id, @RequestParam(required = false) Long parcelaId, Model model) {
        ResumoHonorarios resumo = honorariosService.resumo(id);
        RecebimentoForm form = new RecebimentoForm();
        form.setDataRecebimento(honorariosService.hoje());
        form.setParcelaId(parcelaId);
        form.setValor(valorSugerido(resumo, parcelaId));
        return exibirFormularioRecebimento(form, resumo, model);
    }

    @PostMapping("/honorarios/{id}/recebimentos")
    public String registrarRecebimento(@PathVariable Long id, @Valid @ModelAttribute("form") RecebimentoForm form,
                                       BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                honorariosService.registrarRecebimento(id, form);
                redirect.addFlashAttribute("sucesso", "Recebimento registrado.");
                return "redirect:/honorarios/" + id + "#recebimentos";
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirFormularioRecebimento(form, honorariosService.resumo(id), model);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/recebimentos/{id}/cancelar")
    public String cancelarRecebimento(@PathVariable Long id, @RequestParam(required = false) String motivo,
                                      RedirectAttributes redirect) {
        Long acordoId = honorariosService.acordoDoRecebimento(id);
        try {
            honorariosService.cancelarRecebimento(id, motivo);
            redirect.addFlashAttribute("sucesso", "Recebimento cancelado. O valor voltou para o saldo pendente.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/honorarios/" + acordoId + "#recebimentos";
    }

    private String exibirFormularioAcordo(AcordoForm form, Atendimento atendimento, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("atendimento", atendimento);
        model.addAttribute("formasCobranca", FormaCobranca.values());
        return "honorarios/form";
    }

    private String exibirGerarParcelas(GerarParcelasForm form, ResumoHonorarios resumo, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("resumo", resumo);
        return "honorarios/gerar-parcelas";
    }

    private String exibirFormularioParcela(ParcelaForm form, AcordoHonorarios acordo, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("acordo", acordo);
        return "honorarios/parcela-form";
    }

    private String exibirFormularioRecebimento(RecebimentoForm form, ResumoHonorarios resumo, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("resumo", resumo);
        model.addAttribute("formasPagamento", FormaPagamento.values());
        return "honorarios/recebimento-form";
    }

    private static BigDecimal valorSugerido(ResumoHonorarios resumo, Long parcelaId) {
        if (parcelaId != null) {
            return resumo.parcelas().stream()
                    .filter(p -> p.parcela().getId().equals(parcelaId))
                    .map(ResumoHonorarios.ParcelaComRecebido::getPendente)
                    .findFirst()
                    .orElse(null);
        }
        return resumo.saldo() != null && resumo.saldo().signum() > 0 ? resumo.saldo() : null;
    }
}
