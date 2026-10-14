package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.dto.FiltroPeriodo;
import br.com.escritorio.financeiro.service.FechamentoDespesaService;
import br.com.escritorio.financeiro.service.PainelService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDate;
import java.time.YearMonth;

@Controller
public class InicioController {

    private final PainelService painelService;
    private final FechamentoDespesaService fechamentoDespesaService;

    public InicioController(PainelService painelService, FechamentoDespesaService fechamentoDespesaService) {
        this.painelService = painelService;
        this.fechamentoDespesaService = fechamentoDespesaService;
    }

    @GetMapping("/")
    public String inicio(@ModelAttribute("filtro") FiltroPeriodo filtro, BindingResult parametrosInvalidos,
                         Model model) {
        LocalDate hoje = painelService.hoje();
        if (filtro.getMes() == null) {
            filtro.setMes(YearMonth.from(hoje));
        }
        LocalDate inicio = filtro.getInicioEfetivo();
        LocalDate fim = filtro.getFimEfetivo();
        model.addAttribute("painel", painelService.calcular(inicio, fim));
        model.addAttribute("pendenciasDespesas", fechamentoDespesaService.pendencias());
        model.addAttribute("hoje", hoje);
        model.addAttribute("fimDoPeriodo", fim.equals(FiltroPeriodo.DATA_MAXIMA) ? null : fim);
        model.addAttribute("mesAnterior", filtro.getMes().minusMonths(1));
        model.addAttribute("mesSeguinte", filtro.getMes().plusMonths(1));
        model.addAttribute("mesAtual", YearMonth.from(hoje));
        model.addAttribute("tituloPeriodo", filtro.isPeriodoPersonalizado()
                ? TituloPeriodo.periodo(filtro.getInicioOrdenado(), filtro.getFimOrdenado())
                : TituloPeriodo.mes(filtro.getMes()));
        return "inicio";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}
