package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.Pericia;
import br.com.escritorio.financeiro.domain.SituacaoPericia;
import br.com.escritorio.financeiro.dto.FiltroPericias;
import br.com.escritorio.financeiro.dto.PericiaForm;
import br.com.escritorio.financeiro.dto.PeriodoPagamentos;
import br.com.escritorio.financeiro.dto.RemarcacaoPericiaForm;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.ClienteService;
import br.com.escritorio.financeiro.service.LocalPericiaService;
import br.com.escritorio.financeiro.service.PericiaService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.ResponsavelService;
import br.com.escritorio.financeiro.service.TipoPericiaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.List;

/** Agenda de perícias médicas (por dia, semana, mês ou período): a tela que substitui o caderno. */
@Controller
@RequestMapping("/pericias")
public class PericiaController {

    private static final String VIEW_FORM = "pericias/form";
    private static final String VIEW_REMARCAR = "pericias/remarcar";
    private static final String ACAO_LANCAR_OUTRA = "nova";
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PericiaService periciaService;
    private final TipoPericiaService tipoPericiaService;
    private final LocalPericiaService localPericiaService;
    private final ResponsavelService responsavelService;
    private final ClienteService clienteService;
    private final AuditoriaService auditoriaService;

    public PericiaController(PericiaService periciaService, TipoPericiaService tipoPericiaService,
                             LocalPericiaService localPericiaService, ResponsavelService responsavelService,
                             ClienteService clienteService, AuditoriaService auditoriaService) {
        this.periciaService = periciaService;
        this.tipoPericiaService = tipoPericiaService;
        this.localPericiaService = localPericiaService;
        this.responsavelService = responsavelService;
        this.clienteService = clienteService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String agenda(@ModelAttribute("filtro") FiltroPericias filtro, BindingResult parametrosInvalidos,
                         HttpServletRequest request, Model model) {
        LocalDate hoje = periciaService.hoje();
        FiltrosDaAgenda.completar(filtro, hoje);
        model.addAttribute("agenda", periciaService.agenda(filtro));
        model.addAttribute("hoje", hoje);
        if (filtro.getPeriodo().isNavegavel()) {
            model.addAttribute("dataAnterior", filtro.deslocar(-1));
            model.addAttribute("dataSeguinte", filtro.deslocar(1));
        }
        model.addAttribute("periodos", PeriodoPagamentos.values());
        model.addAttribute("responsaveis", responsavelService.listar());
        model.addAttribute("tipos", tipoPericiaService.listar());
        model.addAttribute("locais", localPericiaService.listar());
        model.addAttribute("situacoes", SituacaoPericia.values());
        model.addAttribute("urlAtual", Retorno.urlAtual(request));
        model.addAttribute("tituloPeriodo", FiltrosDaAgenda.tituloPeriodo(filtro));
        String consulta = request.getQueryString();
        model.addAttribute("linkExcel", request.getRequestURI() + "/planilha.xlsx"
                + (consulta == null ? "" : "?" + consulta));
        return "pericias/lista";
    }

    @GetMapping("/planilha.xlsx")
    public ResponseEntity<byte[]> exportar(@ModelAttribute("filtro") FiltroPericias filtro,
                                           BindingResult parametrosInvalidos) {
        FiltrosDaAgenda.completar(filtro, periciaService.hoje());
        String periodo = FiltrosDaAgenda.tituloPeriodo(filtro);
        List<Pericia> pericias = periciaService.agenda(filtro).pericias();
        byte[] arquivo = AgendaPericiasExcel.gerar(periodo, pericias);
        int linhas = pericias.size();
        auditoriaService.registrar(AcaoAuditoria.EXPORTACAO, PericiaService.ENTIDADE, null,
                "Agenda de perícias exportada em Excel (" + periodo + ", " + linhas
                        + (linhas == 1 ? " linha)." : " linhas)."));
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("agenda-pericias-" + filtro.getSufixoArquivo() + ".xlsx").build().toString())
                .cacheControl(CacheControl.noStore())
                .body(arquivo);
    }

    @GetMapping("/nova")
    public String nova(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                       @RequestParam(required = false) Long clienteId, Model model) {
        return exibirFormulario(periciaService.novoFormulario(data, clienteId), model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") PericiaForm form, BindingResult resultado,
                        @RequestParam(required = false) String acao, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return exibirFormulario(form, model);
        }
        try {
            Pericia pericia = periciaService.criar(form);
            return depoisDeSalvar(pericia, "cadastrada", acao, redirect);
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormulario(form, model);
        }
    }

    @GetMapping("/{id}")
    public String detalhar(@PathVariable Long id, Model model) {
        Pericia pericia = periciaService.buscar(id);
        model.addAttribute("pericia", pericia);
        model.addAttribute("remarcadaDe", periciaService.remarcadaDe(id).orElse(null));
        model.addAttribute("hoje", periciaService.hoje());
        model.addAttribute("historico", auditoriaService.historico(PericiaService.ENTIDADE, id));
        return "pericias/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Pericia pericia = periciaService.buscar(id);
        if (!pericia.isAgendada()) {
            redirect.addFlashAttribute("erro", "Esta perícia está " + pericia.getSituacao().getRotulo().toLowerCase()
                    + " e não pode mais ser editada.");
            return "redirect:/pericias/" + id;
        }
        return exibirFormulario(PericiaForm.de(pericia), model);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") PericiaForm form,
                            BindingResult resultado, @RequestParam(required = false) String acao, Model model,
                            RedirectAttributes redirect) {
        periciaService.buscar(id);
        form.setId(id);
        if (resultado.hasErrors()) {
            return exibirFormulario(form, model);
        }
        try {
            Pericia pericia = periciaService.atualizar(id, form);
            return depoisDeSalvar(pericia, "atualizada", acao, redirect);
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormulario(form, model);
        }
    }

    @GetMapping("/{id}/remarcar")
    public String remarcar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Pericia pericia = periciaService.buscar(id);
        if (!pericia.isAgendada()) {
            redirect.addFlashAttribute("erro", "Esta perícia está " + pericia.getSituacao().getRotulo().toLowerCase()
                    + " e não pode mais ser remarcada.");
            return "redirect:/pericias/" + id;
        }
        return exibirRemarcacao(pericia, RemarcacaoPericiaForm.de(pericia), model);
    }

    @PostMapping("/{id}/remarcar")
    public String salvarRemarcacao(@PathVariable Long id, @Valid @ModelAttribute("form") RemarcacaoPericiaForm form,
                                   BindingResult resultado, Model model, RedirectAttributes redirect) {
        Pericia pericia = periciaService.buscar(id);
        if (resultado.hasErrors()) {
            return exibirRemarcacao(pericia, form, model);
        }
        try {
            Pericia nova = periciaService.remarcar(id, form);
            redirect.addFlashAttribute("sucesso", "Perícia de '" + nova.getCliente().getNome() + "' remarcada.");
            return "redirect:/pericias/" + nova.getId();
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirRemarcacao(pericia, form, model);
        }
    }

    @PostMapping("/{id}/realizada")
    public String marcarRealizada(@PathVariable Long id, @RequestParam(required = false) String voltar,
                                  RedirectAttributes redirect) {
        try {
            Pericia pericia = periciaService.marcarRealizada(id);
            redirect.addFlashAttribute("sucesso", "Perícia de '" + pericia.getCliente().getNome()
                    + "' marcada como realizada.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:" + Retorno.seguro(voltar, "/pericias/" + id);
    }

    @PostMapping("/{id}/falta")
    public String marcarFalta(@PathVariable Long id, @RequestParam(required = false) String motivo,
                              @RequestParam(required = false) String voltar, RedirectAttributes redirect) {
        try {
            Pericia pericia = periciaService.marcarFalta(id, motivo);
            redirect.addFlashAttribute("sucesso", "Falta do cliente '" + pericia.getCliente().getNome()
                    + "' registrada.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:" + Retorno.seguro(voltar, "/pericias/" + id);
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, @RequestParam(required = false) String motivo,
                           RedirectAttributes redirect) {
        try {
            Pericia pericia = periciaService.cancelar(id, motivo);
            redirect.addFlashAttribute("sucesso", "Perícia de '" + pericia.getCliente().getNome()
                    + "' cancelada. Ela continua registrada no histórico.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/pericias/" + id;
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, @RequestParam(required = false) String motivo,
                          RedirectAttributes redirect) {
        try {
            Pericia pericia = periciaService.excluir(id, motivo);
            redirect.addFlashAttribute("sucesso", "Perícia de '" + pericia.getCliente().getNome()
                    + "' excluída. O registro fica na auditoria.");
            return "redirect:/pericias" + (pericia.getData() == null ? "#a-agendar" : "?data=" + pericia.getData());
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/pericias/" + id;
        }
    }

    /**
     * Volta para a agenda no dia da perícia (ou na lista "a agendar", se ainda não tem data) ou, com
     * "Salvar e cadastrar outra" ({@code acao=nova}), abre um formulário em branco no mesmo dia.
     */
    private static String depoisDeSalvar(Pericia pericia, String feito, String acao, RedirectAttributes redirect) {
        String mensagem = "Perícia de '" + pericia.getCliente().getNome() + "' " + feito + ".";
        String dia = pericia.getData() == null ? "" : "?data=" + pericia.getData();
        if (ACAO_LANCAR_OUTRA.equals(acao)) {
            redirect.addFlashAttribute("sucesso", mensagem + " Preencha a próxima perícia.");
            return "redirect:/pericias/nova" + dia;
        }
        redirect.addFlashAttribute("sucesso", mensagem);
        return "redirect:/pericias" + (pericia.getData() == null ? "#a-agendar" : dia);
    }

    private String exibirFormulario(PericiaForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("tipos", tipoPericiaService.listarParaSelecao(form.isNovo() ? null : form.getTipoPericiaId()));
        model.addAttribute("responsaveis", responsavelService.listarParaSelecao(
                form.isNovo() || form.getResponsavelId() == null ? List.of() : List.of(form.getResponsavelId())));
        model.addAttribute("locais", localPericiaService.listarParaSelecao(form.isNovo() ? null : form.getLocalId()));
        if (form.getClienteId() != null) {
            model.addAttribute("clienteEscolhido", clienteService.buscar(form.getClienteId()));
        }
        if (!form.isNovo()) {
            model.addAttribute("historico", auditoriaService.historico(PericiaService.ENTIDADE, form.getId()));
        }
        return VIEW_FORM;
    }

    private String exibirRemarcacao(Pericia pericia, RemarcacaoPericiaForm form, Model model) {
        model.addAttribute("pericia", pericia);
        model.addAttribute("form", form);
        Long localAtual = pericia.getLocal() == null ? null : pericia.getLocal().getId();
        model.addAttribute("locais", localPericiaService.listarParaSelecao(localAtual));
        return VIEW_REMARCAR;
    }
}
