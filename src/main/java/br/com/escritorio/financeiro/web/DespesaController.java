package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Despesa;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.SituacaoEnvioDespesas;
import br.com.escritorio.financeiro.dto.DespesaForm;
import br.com.escritorio.financeiro.dto.FiltroDespesas;
import br.com.escritorio.financeiro.dto.ResumoDespesas;
import br.com.escritorio.financeiro.security.Permissoes;
import br.com.escritorio.financeiro.service.CategoriaDespesaService;
import br.com.escritorio.financeiro.service.DespesaService;
import br.com.escritorio.financeiro.service.FechamentoDespesaService;
import br.com.escritorio.financeiro.service.FormaPagamentoDespesaService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/**
 * Despesas avulsas do escritório e envio mensal da planilha "Contas Pagas" à contabilidade.
 * Reabrir um mês já enviado exige a permissão DESPESAS_REABRIR_FECHAMENTO (regra também no {@code SecurityConfig}).
 */
@Controller
@RequestMapping("/despesas")
public class DespesaController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final DespesaService despesaService;
    private final FechamentoDespesaService fechamentoService;
    private final CategoriaDespesaService categoriaService;
    private final FormaPagamentoDespesaService formaPagamentoService;

    public DespesaController(DespesaService despesaService, FechamentoDespesaService fechamentoService,
                             CategoriaDespesaService categoriaService,
                             FormaPagamentoDespesaService formaPagamentoService) {
        this.despesaService = despesaService;
        this.fechamentoService = fechamentoService;
        this.categoriaService = categoriaService;
        this.formaPagamentoService = formaPagamentoService;
    }

    @GetMapping
    public String listar(@ModelAttribute("filtro") FiltroDespesas filtro, BindingResult parametrosInvalidos,
                         @RequestParam(defaultValue = "0") int pagina, Model model) {
        LocalDate hoje = despesaService.hoje();
        YearMonth mesAtual = YearMonth.from(hoje);
        if (filtro.getMes() == null) {
            filtro.setMes(mesAtual);
        }
        YearMonth mes = filtro.getMes();
        List<Despesa> despesas = despesaService.pesquisar(mes, filtro.getCategoriaId(), filtro.getFornecedor());
        SituacaoEnvioDespesas situacao = fechamentoService.situacao(mes);

        model.addAttribute("despesas", Paginas.de(despesas, pagina));
        model.addAttribute("resumo", ResumoDespesas.de(despesas));
        model.addAttribute("resumoMes", filtro.isFiltrado() ? ResumoDespesas.de(despesaService.ativasDoMes(mes))
                : ResumoDespesas.de(despesas));
        model.addAttribute("fechamento", fechamentoService.buscar(mes).orElse(null));
        model.addAttribute("situacaoEnvio", situacao);
        model.addAttribute("situacoesEnvio", SituacaoEnvioDespesas.values());
        model.addAttribute("prazo", SituacaoEnvioDespesas.prazo(mes));
        model.addAttribute("podeMarcarEnviado", situacao != SituacaoEnvioDespesas.ENVIADO && mes.isBefore(mesAtual));
        model.addAttribute("categorias", categoriaService.listar());
        model.addAttribute("tituloPeriodo", TituloPeriodo.mes(mes));
        model.addAttribute("mesAnterior", mes.minusMonths(1));
        model.addAttribute("mesSeguinte", mes.plusMonths(1));
        model.addAttribute("mesAtual", mesAtual);
        return "despesas/lista";
    }

    @GetMapping("/nova")
    public String nova(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                       Model model) {
        LocalDate hoje = despesaService.hoje();
        DespesaForm form = new DespesaForm();
        form.setDataPagamento(mes != null && mes.isBefore(YearMonth.from(hoje)) ? mes.atEndOfMonth() : hoje);
        return exibirFormulario(form, model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") DespesaForm form, BindingResult resultado,
                        @RequestParam(required = false) String outra, Model model, RedirectAttributes redirect) {
        if (!resultado.hasErrors()) {
            try {
                Despesa despesa = despesaService.criar(form);
                redirect.addFlashAttribute("sucesso", "Despesa de " + despesa.getFornecedor() + " lançada.");
                return outra != null
                        ? "redirect:/despesas/nova?mes=" + despesa.getCompetencia()
                        : "redirect:/despesas?mes=" + despesa.getCompetencia();
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirFormulario(form, model);
    }

    @GetMapping("/{id}")
    public String detalhar(@PathVariable Long id, Model model) {
        Despesa despesa = despesaService.buscar(id);
        model.addAttribute("despesa", despesa);
        model.addAttribute("historico", despesaService.historico(id));
        model.addAttribute("mesEnviado", fechamentoService.isEnviado(despesa.getCompetencia()));
        return "despesas/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Despesa despesa = despesaService.buscar(id);
        if (despesa.isCancelada()) {
            redirect.addFlashAttribute("erro", "Despesas canceladas não podem ser alteradas.");
            return "redirect:/despesas/" + id;
        }
        if (fechamentoService.isEnviado(despesa.getCompetencia())) {
            redirect.addFlashAttribute("erro", "O mês desta despesa já foi enviado à contabilidade. "
                    + "Peça a quem pode reabrir o fechamento para reabrir o mês antes de alterar.");
            return "redirect:/despesas/" + id;
        }
        return exibirFormulario(DespesaForm.de(despesa), model);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") DespesaForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        despesaService.buscar(id);
        form.setId(id);
        if (!resultado.hasErrors()) {
            try {
                despesaService.atualizar(id, form);
                redirect.addFlashAttribute("sucesso", "Despesa atualizada.");
                return "redirect:/despesas/" + id;
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirFormulario(form, model);
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, @RequestParam(required = false) String motivo,
                           RedirectAttributes redirect) {
        try {
            despesaService.cancelar(id, motivo);
            redirect.addFlashAttribute("sucesso",
                    "Despesa cancelada. Ela sai da planilha da contabilidade, mas continua registrada no histórico.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/despesas/" + id;
    }

    @GetMapping("/planilha.xlsx")
    public ResponseEntity<byte[]> planilha(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        List<Despesa> despesas = despesaService.ativasDoMes(mes);
        byte[] conteudo = PlanilhaContasPagas.gerar(despesas);
        fechamentoService.registrarExportacao(mes, despesas.size());
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("contas-pagas-" + mes + ".xlsx").build().toString())
                .cacheControl(CacheControl.noStore())
                .body(conteudo);
    }

    @PostMapping("/fechamento/enviado")
    public String marcarEnviado(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                                RedirectAttributes redirect) {
        try {
            fechamentoService.marcarEnviado(mes);
            redirect.addFlashAttribute("sucesso", "Planilha de " + TituloPeriodo.mes(mes).toLowerCase(Locale.ROOT)
                    + " marcada como enviada à contabilidade. As despesas do mês ficaram travadas.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/despesas?mes=" + mes;
    }

    @PreAuthorize("hasAuthority('DESPESAS_REABRIR_FECHAMENTO')")
    @PostMapping("/fechamento/reabrir")
    public String reabrir(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                          @RequestParam(required = false) String motivo, Authentication auth,
                          RedirectAttributes redirect) {
        try {
            fechamentoService.reabrir(mes, motivo, Permissoes.possui(auth, Permissao.DESPESAS_REABRIR_FECHAMENTO));
            redirect.addFlashAttribute("sucesso", "Mês reaberto. Corrija as despesas e envie a planilha de novo.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/despesas?mes=" + mes;
    }

    private String exibirFormulario(DespesaForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("categorias", categoriaService.listarParaSelecao(form.isNovo() ? null : form.getCategoriaId()));
        model.addAttribute("formas", formaPagamentoService.listarParaSelecao(
                form.isNovo() ? null : form.getFormaPagamentoId()));
        model.addAttribute("fornecedores", despesaService.fornecedores());
        model.addAttribute("hoje", despesaService.hoje());
        return "despesas/form";
    }
}
