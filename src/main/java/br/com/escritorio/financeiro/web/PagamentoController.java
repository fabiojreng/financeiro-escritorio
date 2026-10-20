package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.AcaoAuditoria;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.dto.AgendaPagamentos;
import br.com.escritorio.financeiro.dto.FiltroPagamentos;
import br.com.escritorio.financeiro.dto.PagamentoForm;
import br.com.escritorio.financeiro.dto.PeriodoPagamentos;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.ClienteService;
import br.com.escritorio.financeiro.service.FechamentoDespesaService;
import br.com.escritorio.financeiro.service.HonorarioService;
import br.com.escritorio.financeiro.service.PagamentoService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.ResponsavelService;
import br.com.escritorio.financeiro.service.TipoServicoService;
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

/** Agenda de pagamentos da recepção (por dia, semana, mês ou período): a tela que substitui a folha de papel. */
@Controller
@RequestMapping("/agenda-pagamentos")
public class PagamentoController {

    private static final String VIEW_FORM = "pagamentos/form";
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PagamentoService pagamentoService;
    private final ResponsavelService responsavelService;
    private final TipoServicoService tipoServicoService;
    private final AuditoriaService auditoriaService;
    private final ClienteService clienteService;
    private final FechamentoDespesaService fechamentoDespesaService;
    private final HonorarioService honorarioService;

    public PagamentoController(PagamentoService pagamentoService, ResponsavelService responsavelService,
                               TipoServicoService tipoServicoService, AuditoriaService auditoriaService,
                               ClienteService clienteService, FechamentoDespesaService fechamentoDespesaService,
                               HonorarioService honorarioService) {
        this.pagamentoService = pagamentoService;
        this.honorarioService = honorarioService;
        this.responsavelService = responsavelService;
        this.tipoServicoService = tipoServicoService;
        this.auditoriaService = auditoriaService;
        this.clienteService = clienteService;
        this.fechamentoDespesaService = fechamentoDespesaService;
    }

    @GetMapping
    public String agenda(@ModelAttribute("filtro") FiltroPagamentos filtro, BindingResult parametrosInvalidos,
                         HttpServletRequest request, Model model) {
        LocalDate hoje = pagamentoService.hoje();
        FiltrosDaAgenda.completar(filtro, hoje);
        model.addAttribute("agenda", pagamentoService.agenda(filtro));
        model.addAttribute("pendenciasDespesas", fechamentoDespesaService.pendencias());
        model.addAttribute("hoje", hoje);
        if (filtro.getPeriodo().isNavegavel()) {
            model.addAttribute("dataAnterior", filtro.deslocar(-1));
            model.addAttribute("dataSeguinte", filtro.deslocar(1));
        }
        model.addAttribute("periodos", PeriodoPagamentos.values());
        model.addAttribute("responsaveis", responsavelService.listar());
        model.addAttribute("tipos", tipoServicoService.listar());
        model.addAttribute("statusPagamento", StatusPagamento.values());
        model.addAttribute("urlAtual", Retorno.urlAtual(request));
        model.addAttribute("tituloPeriodo", FiltrosDaAgenda.tituloPeriodo(filtro));
        String consulta = request.getQueryString();
        model.addAttribute("linkExcel", request.getRequestURI() + "/planilha.xlsx"
                + (consulta == null ? "" : "?" + consulta));
        return "pagamentos/lista";
    }

    @GetMapping("/planilha.xlsx")
    public ResponseEntity<byte[]> exportar(@ModelAttribute("filtro") FiltroPagamentos filtro,
                                           BindingResult parametrosInvalidos) {
        FiltrosDaAgenda.completar(filtro, pagamentoService.hoje());
        String periodo = FiltrosDaAgenda.tituloPeriodo(filtro);
        AgendaPagamentos agenda = pagamentoService.agenda(filtro);
        List<PagamentoAgendado> pagamentos = agenda.pagamentos();
        honorarioService.valoresPorPagamento(pagamentos);
        byte[] arquivo = AgendaPagamentosExcel.gerar(periodo, pagamentos);
        int linhas = pagamentos.size();
        auditoriaService.registrar(AcaoAuditoria.EXPORTACAO, PagamentoService.ENTIDADE, null,
                "Agenda de pagamentos exportada em Excel (" + periodo + ", " + linhas
                        + (linhas == 1 ? " linha)." : " linhas)."));
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("agenda-pagamentos-" + filtro.getSufixoArquivo() + ".xlsx").build().toString())
                .cacheControl(CacheControl.noStore())
                .body(arquivo);
    }

    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                       @RequestParam(required = false) Long clienteId, Model model) {
        return exibirFormulario(pagamentoService.novoFormulario(data, clienteId), model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") PagamentoForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return exibirFormulario(form, model);
        }
        try {
            PagamentoAgendado pagamento = pagamentoService.criar(form);
            redirect.addFlashAttribute("sucesso", "Pagamento de '" + pagamento.getCliente().getNome() + "' lançado.");
            return "redirect:/agenda-pagamentos?data=" + pagamento.getDataRecebimento();
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormulario(form, model);
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        return exibirFormulario(PagamentoForm.de(pagamentoService.buscar(id)), model);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") PagamentoForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        pagamentoService.buscar(id);
        form.setId(id);
        if (resultado.hasErrors()) {
            return exibirFormulario(form, model);
        }
        try {
            PagamentoAgendado pagamento = pagamentoService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Pagamento de '" + pagamento.getCliente().getNome() + "' atualizado.");
            return "redirect:/agenda-pagamentos?data=" + pagamento.getDataRecebimento();
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormulario(form, model);
        }
    }

    @PostMapping("/{id}/status")
    public String alterarStatus(@PathVariable Long id, @RequestParam StatusPagamento status,
                                @RequestParam(required = false) String voltar, RedirectAttributes redirect) {
        PagamentoAgendado pagamento = pagamentoService.alterarStatus(id, status);
        redirect.addFlashAttribute("sucesso", "Status do pagamento de '" + pagamento.getCliente().getNome()
                + "' alterado para " + status.getRotulo() + ".");
        return "redirect:" + Retorno.seguro(voltar, "/agenda-pagamentos?data=" + pagamento.getDataRecebimento());
    }

    private String exibirFormulario(PagamentoForm form, Model model) {
        Long tipoAtual = form.isNovo() ? null : form.getTipoServicoId();
        List<Long> responsaveisAtuais = form.isNovo() ? List.of() : form.getResponsavelIds();
        model.addAttribute("form", form);
        model.addAttribute("tipos", tipoServicoService.listarParaSelecao(tipoAtual));
        model.addAttribute("responsaveis", responsavelService.listarParaSelecao(responsaveisAtuais));
        model.addAttribute("statusPagamento", StatusPagamento.values());
        if (form.getClienteId() != null) {
            model.addAttribute("clienteEscolhido", clienteService.buscar(form.getClienteId()));
        }
        if (!form.isNovo()) {
            model.addAttribute("historico", auditoriaService.historico(PagamentoService.ENTIDADE, form.getId()));
        }
        return VIEW_FORM;
    }

}
