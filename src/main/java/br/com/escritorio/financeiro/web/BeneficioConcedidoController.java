package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.BeneficioConcedido;
import br.com.escritorio.financeiro.dto.BeneficioConcedidoForm;
import br.com.escritorio.financeiro.dto.FiltroBeneficiosConcedidos;
import br.com.escritorio.financeiro.service.BeneficioConcedidoService;
import br.com.escritorio.financeiro.service.ClienteService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.ResponsavelService;
import br.com.escritorio.financeiro.service.TipoServicoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.YearMonth;
import java.util.List;

/** Benefícios já concedidos, lançados e consultados mês a mês. */
@Controller
@RequestMapping("/beneficios-concedidos")
public class BeneficioConcedidoController {

    private static final String VIEW_LISTA = "beneficios/lista";
    private static final String VIEW_FORM = "beneficios/form";
    private static final String VIEW_DETALHE = "beneficios/detalhe";

    private final BeneficioConcedidoService beneficioService;
    private final TipoServicoService tipoServicoService;
    private final ResponsavelService responsavelService;
    private final ClienteService clienteService;

    public BeneficioConcedidoController(BeneficioConcedidoService beneficioService,
                                        TipoServicoService tipoServicoService, ResponsavelService responsavelService,
                                        ClienteService clienteService) {
        this.beneficioService = beneficioService;
        this.tipoServicoService = tipoServicoService;
        this.responsavelService = responsavelService;
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(@ModelAttribute("filtro") FiltroBeneficiosConcedidos filtro, BindingResult parametrosInvalidos,
                         @RequestParam(defaultValue = "0") int pagina, Model model) {
        YearMonth mesAtual = YearMonth.from(beneficioService.hoje());
        if (filtro.getMes() == null) {
            filtro.setMes(mesAtual);
        }
        YearMonth mes = filtro.getMes();
        List<BeneficioConcedido> beneficios = beneficioService.pesquisar(filtro);
        model.addAttribute("beneficios", Paginas.de(beneficios, pagina));
        model.addAttribute("quantidade", beneficios.size());
        model.addAttribute("quantidadeMes", filtro.isFiltrado() ? beneficioService.contar(mes) : beneficios.size());
        model.addAttribute("tipos", tipoServicoService.listar());
        model.addAttribute("responsaveis", responsavelService.listar());
        model.addAttribute("tituloPeriodo", TituloPeriodo.mes(mes));
        model.addAttribute("mesAnterior", mes.minusMonths(1));
        model.addAttribute("mesSeguinte", mes.plusMonths(1));
        model.addAttribute("mesAtual", mesAtual);
        return VIEW_LISTA;
    }

    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes, Model model) {
        BeneficioConcedidoForm form = new BeneficioConcedidoForm();
        form.setCompetencia(mes != null ? mes : YearMonth.from(beneficioService.hoje()));
        return exibirFormulario(form, model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") BeneficioConcedidoForm form, BindingResult resultado,
                        @RequestParam(required = false) String outra, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                BeneficioConcedido beneficio = beneficioService.criar(form);
                redirect.addFlashAttribute("sucesso",
                        "Benefício de '" + beneficio.getCliente().getNome() + "' registrado.");
                return outra != null
                        ? "redirect:/beneficios-concedidos/novo?mes=" + beneficio.getMes()
                        : "redirect:/beneficios-concedidos?mes=" + beneficio.getMes();
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirFormulario(form, model);
    }

    @GetMapping("/{id}")
    public String detalhar(@PathVariable Long id, Model model) {
        BeneficioConcedido beneficio = beneficioService.buscar(id);
        model.addAttribute("beneficio", beneficio);
        model.addAttribute("tituloMes", TituloPeriodo.mes(beneficio.getMes()));
        model.addAttribute("historico", beneficioService.historico(id));
        return VIEW_DETALHE;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        return exibirFormulario(BeneficioConcedidoForm.de(beneficioService.buscar(id)), model);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") BeneficioConcedidoForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        beneficioService.buscar(id);
        form.setId(id);
        if (!resultado.hasErrors()) {
            try {
                BeneficioConcedido beneficio = beneficioService.atualizar(id, form);
                redirect.addFlashAttribute("sucesso", "Benefício concedido atualizado.");
                return "redirect:/beneficios-concedidos/" + beneficio.getId();
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirFormulario(form, model);
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, @RequestParam(required = false) String motivo,
                          RedirectAttributes redirect) {
        try {
            BeneficioConcedido beneficio = beneficioService.excluir(id, motivo);
            redirect.addFlashAttribute("sucesso", "Benefício de '" + beneficio.getCliente().getNome()
                    + "' excluído. O registro fica na auditoria.");
            return "redirect:/beneficios-concedidos?mes=" + beneficio.getMes();
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/beneficios-concedidos/" + id;
        }
    }

    private String exibirFormulario(BeneficioConcedidoForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("tipos", tipoServicoService.listarParaSelecao(form.isNovo() ? null : form.getTipoServicoId()));
        model.addAttribute("responsaveis", responsavelService.listarParaSelecao(
                form.isNovo() || form.getResponsavelId() == null ? List.of() : List.of(form.getResponsavelId())));
        if (form.getClienteId() != null) {
            model.addAttribute("clienteEscolhido", clienteService.buscar(form.getClienteId()));
        }
        return VIEW_FORM;
    }
}
