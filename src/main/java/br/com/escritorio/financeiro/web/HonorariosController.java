package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.dto.FiltroPagamentos;
import br.com.escritorio.financeiro.dto.NegociacaoForm;
import br.com.escritorio.financeiro.dto.PeriodoPagamentos;
import br.com.escritorio.financeiro.dto.RecebimentoForm;
import br.com.escritorio.financeiro.service.HonorarioService;
import br.com.escritorio.financeiro.service.PagamentoService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.ResponsavelService;
import br.com.escritorio.financeiro.service.TipoServicoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Honorários dos pagamentos da agenda. O lançamento simples fica na lista; negociação, parcelas e
 * recebimentos ficam no detalhe.
 */
@Controller
@RequestMapping("/honorarios")
public class HonorariosController {

    private final PagamentoService pagamentoService;
    private final HonorarioService honorarioService;
    private final ResponsavelService responsavelService;
    private final TipoServicoService tipoServicoService;

    public HonorariosController(PagamentoService pagamentoService, HonorarioService honorarioService,
                                ResponsavelService responsavelService, TipoServicoService tipoServicoService) {
        this.pagamentoService = pagamentoService;
        this.honorarioService = honorarioService;
        this.responsavelService = responsavelService;
        this.tipoServicoService = tipoServicoService;
    }

    @GetMapping
    public String listar(@ModelAttribute("filtro") FiltroPagamentos filtro, BindingResult parametrosInvalidos,
                         HttpServletRequest request, Model model) {
        LocalDate hoje = pagamentoService.hoje();
        FiltrosDaAgenda.completar(filtro, hoje, PeriodoPagamentos.MES);
        model.addAttribute("agenda", honorarioService.listar(filtro));
        model.addAttribute("hoje", hoje);
        if (filtro.getPeriodo().isNavegavel()) {
            model.addAttribute("dataAnterior", filtro.deslocar(-1));
            model.addAttribute("dataSeguinte", filtro.deslocar(1));
        }
        model.addAttribute("periodos", PeriodoPagamentos.values());
        model.addAttribute("responsaveis", responsavelService.listar());
        model.addAttribute("tipos", tipoServicoService.listar());
        model.addAttribute("situacoesHonorario", SituacaoHonorarioPagamento.values());
        model.addAttribute("situacoesLancamento", SituacaoHonorarioPagamento.lancamentoSimples());
        model.addAttribute("urlAtual", Retorno.urlAtual(request));
        model.addAttribute("tituloPeriodo", FiltrosDaAgenda.tituloPeriodo(filtro));
        return "honorarios/lista";
    }

    @PostMapping("/{id}")
    public String lancar(@PathVariable Long id, @RequestParam(required = false) String valor,
                         @RequestParam SituacaoHonorarioPagamento situacao,
                         @RequestParam(required = false) String voltar, RedirectAttributes redirect) {
        try {
            PagamentoAgendado pagamento = pagamentoService.lancarHonorario(id,
                    ValorDigitado.opcional(valor, "O honorário"), situacao);
            redirect.addFlashAttribute("sucesso", "Honorário de '" + pagamento.getCliente().getNome() + "' salvo.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:" + Retorno.seguro(voltar, "/honorarios");
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("detalhe", honorarioService.detalhe(id));
        model.addAttribute("hoje", honorarioService.hoje());
        model.addAttribute("recebimentoForm", recebimentoPadrao());
        model.addAttribute("formasPagamento", FormaPagamento.values());
        return "honorarios/detalhe";
    }

    @GetMapping("/{id}/negociar")
    public String negociarFormulario(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            return exibirNegociacao(id, honorarioService.formularioNegociacao(id), model);
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/honorarios/" + id;
        }
    }

    @PostMapping("/{id}/negociar")
    public String negociar(@PathVariable Long id, @Valid @ModelAttribute("form") NegociacaoForm form,
                           BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return exibirNegociacao(id, form, model);
        }
        try {
            honorarioService.negociar(id, form);
            redirect.addFlashAttribute("sucesso", "Negociação de honorário registrada.");
            return "redirect:/honorarios/" + id;
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirNegociacao(id, form, model);
        }
    }

    @PostMapping("/{id}/parcelas/{parcelaId}/receber")
    public String receber(@PathVariable Long id, @PathVariable Long parcelaId,
                          @Valid @ModelAttribute("recebimentoForm") RecebimentoForm form, BindingResult resultado,
                          Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("detalhe", honorarioService.detalhe(id));
            model.addAttribute("hoje", honorarioService.hoje());
            model.addAttribute("formasPagamento", FormaPagamento.values());
            model.addAttribute("parcelaRecebimentoId", parcelaId);
            return "honorarios/detalhe";
        }
        try {
            honorarioService.registrarRecebimento(id, parcelaId, form);
            redirect.addFlashAttribute("sucesso", "Recebimento registrado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/honorarios/" + id;
    }

    @PostMapping("/{id}/recebimentos/{recebimentoId}/estornar")
    public String estornar(@PathVariable Long id, @PathVariable Long recebimentoId, @RequestParam String motivo,
                           RedirectAttributes redirect) {
        try {
            honorarioService.estornar(id, recebimentoId, motivo);
            redirect.addFlashAttribute("sucesso", "Recebimento estornado.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/honorarios/" + id;
    }

    private String exibirNegociacao(Long id, NegociacaoForm form, Model model) {
        PagamentoAgendado pagamento = pagamentoService.buscar(id);
        model.addAttribute("form", form);
        model.addAttribute("pagamento", pagamento);
        model.addAttribute("negociacaoAtiva", honorarioService.temNegociacaoAtiva(id));
        model.addAttribute("jaRecebido", honorarioService.totalRecebido(id));
        model.addAttribute("hoje", honorarioService.hoje());
        return "honorarios/negociar";
    }

    private RecebimentoForm recebimentoPadrao() {
        RecebimentoForm form = new RecebimentoForm();
        form.setDataRecebimento(honorarioService.hoje());
        form.setFormaPagamento(FormaPagamento.PIX);
        return form;
    }
}
