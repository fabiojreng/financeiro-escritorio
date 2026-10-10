package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.dto.FiltroAgenda;
import br.com.escritorio.financeiro.service.PrevisaoService;
import br.com.escritorio.financeiro.service.ResponsavelService;
import br.com.escritorio.financeiro.service.TipoServicoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.YearMonth;

/** Agenda mensal das previsões de benefício: a tela que substitui o caderno. */
@Controller
@RequestMapping("/agenda")
public class AgendaController {

    private final PrevisaoService previsaoService;
    private final ResponsavelService responsavelService;
    private final TipoServicoService tipoServicoService;

    public AgendaController(PrevisaoService previsaoService, ResponsavelService responsavelService,
                            TipoServicoService tipoServicoService) {
        this.previsaoService = previsaoService;
        this.responsavelService = responsavelService;
        this.tipoServicoService = tipoServicoService;
    }

    @GetMapping
    public String agenda(@ModelAttribute("filtro") FiltroAgenda filtro, BindingResult parametrosInvalidos,
                         HttpServletRequest request, Model model) {
        LocalDate hoje = previsaoService.hoje();
        if (filtro.getMes() == null) {
            filtro.setMes(YearMonth.from(hoje));
        }
        model.addAttribute("agenda", previsaoService.agenda(filtro));
        model.addAttribute("hoje", hoje);
        model.addAttribute("mesAnterior", filtro.getMes().minusMonths(1));
        model.addAttribute("mesSeguinte", filtro.getMes().plusMonths(1));
        model.addAttribute("mesAtual", YearMonth.from(hoje));
        model.addAttribute("totalAtrasadas", previsaoService.contarAtrasadas());
        model.addAttribute("responsaveis", responsavelService.listar());
        model.addAttribute("tipos", tipoServicoService.listar());
        model.addAttribute("situacoes", SituacaoPrevisao.values());
        model.addAttribute("situacoesEditaveis", SituacaoPrevisao.editaveis());
        model.addAttribute("urlAtual", Retorno.urlAtual(request));
        model.addAttribute("tituloPeriodo", tituloPeriodo(filtro));
        return "agenda/lista";
    }

    private static String tituloPeriodo(FiltroAgenda filtro) {
        if (filtro.isAtrasadas()) {
            return "Previsões atrasadas sem confirmação";
        }
        if (!filtro.isPeriodoPersonalizado()) {
            return TituloPeriodo.mes(filtro.getMes());
        }
        return TituloPeriodo.periodo(filtro.getInicioEfetivo(), filtro.getFimEfetivo());
    }
}
