package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import br.com.escritorio.financeiro.dto.FiltroPagamentos;
import br.com.escritorio.financeiro.dto.PeriodoPagamentos;
import br.com.escritorio.financeiro.service.PagamentoService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.ResponsavelService;
import br.com.escritorio.financeiro.service.TipoServicoService;
import jakarta.servlet.http.HttpServletRequest;
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
 * Repasses ao responsável de cada pagamento da agenda. Todo pagamento agendado aparece aqui; o valor e a
 * situação do repasse são lançados na própria linha.
 */
@Controller
@RequestMapping("/repasses")
public class RepasseController {

    private final PagamentoService pagamentoService;
    private final ResponsavelService responsavelService;
    private final TipoServicoService tipoServicoService;

    public RepasseController(PagamentoService pagamentoService, ResponsavelService responsavelService,
                             TipoServicoService tipoServicoService) {
        this.pagamentoService = pagamentoService;
        this.responsavelService = responsavelService;
        this.tipoServicoService = tipoServicoService;
    }

    @GetMapping
    public String listar(@ModelAttribute("filtro") FiltroPagamentos filtro, BindingResult parametrosInvalidos,
                         HttpServletRequest request, Model model) {
        LocalDate hoje = pagamentoService.hoje();
        FiltrosDaAgenda.completar(filtro, hoje, PeriodoPagamentos.MES);
        model.addAttribute("agenda", pagamentoService.repasses(filtro));
        model.addAttribute("hoje", hoje);
        if (filtro.getPeriodo().isNavegavel()) {
            model.addAttribute("dataAnterior", filtro.deslocar(-1));
            model.addAttribute("dataSeguinte", filtro.deslocar(1));
        }
        model.addAttribute("periodos", PeriodoPagamentos.values());
        model.addAttribute("responsaveis", responsavelService.listar());
        model.addAttribute("tipos", tipoServicoService.listar());
        model.addAttribute("situacoesRepasse", SituacaoRepassePagamento.values());
        model.addAttribute("urlAtual", Retorno.urlAtual(request));
        model.addAttribute("tituloPeriodo", FiltrosDaAgenda.tituloPeriodo(filtro));
        return "repasses/lista";
    }

    @PostMapping("/{id}")
    public String lancar(@PathVariable Long id, @RequestParam(required = false) String valor,
                         @RequestParam SituacaoRepassePagamento situacao,
                         @RequestParam(required = false) String voltar, RedirectAttributes redirect) {
        try {
            PagamentoAgendado pagamento = pagamentoService.lancarRepasse(id,
                    ValorDigitado.opcional(valor, "O repasse"), situacao);
            redirect.addFlashAttribute("sucesso", "Repasse de '" + pagamento.getCliente().getNome() + "' salvo.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:" + Retorno.seguro(voltar, "/repasses");
    }
}
