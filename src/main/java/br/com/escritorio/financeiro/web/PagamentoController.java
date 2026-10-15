package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Cpf;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.dto.FiltroPagamentos;
import br.com.escritorio.financeiro.dto.PagamentoForm;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.PagamentoService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.RelatorioService;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Agenda semanal de pagamentos da recepção: a tela que substitui a folha de papel. */
@Controller
@RequestMapping("/agenda-pagamentos")
public class PagamentoController {

    private static final String VIEW_FORM = "pagamentos/form";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PagamentoService pagamentoService;
    private final ResponsavelService responsavelService;
    private final TipoServicoService tipoServicoService;
    private final RelatorioService relatorioService;
    private final AuditoriaService auditoriaService;

    public PagamentoController(PagamentoService pagamentoService, ResponsavelService responsavelService,
                               TipoServicoService tipoServicoService, RelatorioService relatorioService,
                               AuditoriaService auditoriaService) {
        this.pagamentoService = pagamentoService;
        this.responsavelService = responsavelService;
        this.tipoServicoService = tipoServicoService;
        this.relatorioService = relatorioService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String agenda(@ModelAttribute("filtro") FiltroPagamentos filtro, BindingResult parametrosInvalidos,
                         HttpServletRequest request, Model model) {
        LocalDate hoje = pagamentoService.hoje();
        completar(filtro, hoje);
        model.addAttribute("agenda", pagamentoService.agenda(filtro));
        model.addAttribute("hoje", hoje);
        model.addAttribute("semanaAnterior", filtro.getInicio().minusWeeks(1));
        model.addAttribute("semanaSeguinte", filtro.getInicio().plusWeeks(1));
        model.addAttribute("responsaveis", responsavelService.listar());
        model.addAttribute("tipos", tipoServicoService.listar());
        model.addAttribute("statusPagamento", StatusPagamento.values());
        model.addAttribute("urlAtual", Retorno.urlAtual(request));
        model.addAttribute("tituloSemana", tituloSemana(filtro));
        String consulta = request.getQueryString();
        model.addAttribute("linkCsv", request.getRequestURI() + "/planilha.csv" + (consulta == null ? "" : "?" + consulta));
        return "pagamentos/lista";
    }

    @GetMapping("/planilha.csv")
    public ResponseEntity<byte[]> exportar(@ModelAttribute("filtro") FiltroPagamentos filtro,
                                           BindingResult parametrosInvalidos) {
        completar(filtro, pagamentoService.hoje());
        Csv csv = new Csv("Cliente", "CPF", "Responsável", "Dia de recebimento", "Benefício", "Valor", "Banco",
                "Status", "Observações");
        for (PagamentoAgendado p : pagamentoService.agenda(filtro).pagamentos()) {
            csv.texto(p.getCliente().getNome())
                    .texto(Cpf.formatar(p.getCliente().getCpf()))
                    .texto(p.getResponsavel().getNome())
                    .data(p.getDataRecebimento())
                    .texto(p.getTipoServico().getNome())
                    .valor(p.getValor())
                    .texto(p.getBanco())
                    .texto(p.getStatus().getRotulo())
                    .texto(p.getObservacoes())
                    .fimDaLinha();
        }
        relatorioService.registrarExportacao("agenda de pagamentos", tituloSemana(filtro), csv.getLinhas());
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("agenda-pagamentos-" + filtro.getInicio() + ".csv").build().toString())
                .cacheControl(CacheControl.noStore())
                .body(csv.bytes());
    }

    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                       Model model) {
        PagamentoForm form = new PagamentoForm();
        form.setDataRecebimento(data);
        return exibirFormulario(form, model);
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
            return "redirect:/agenda-pagamentos?semana=" + pagamento.getDataRecebimento();
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
            return "redirect:/agenda-pagamentos?semana=" + pagamento.getDataRecebimento();
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
        return "redirect:" + Retorno.seguro(voltar, "/agenda-pagamentos?semana=" + pagamento.getDataRecebimento());
    }

    private String exibirFormulario(PagamentoForm form, Model model) {
        Long tipoAtual = form.isNovo() ? null : form.getTipoServicoId();
        Long responsavelAtual = form.isNovo() ? null : form.getResponsavelId();
        model.addAttribute("form", form);
        model.addAttribute("tipos", tipoServicoService.listarParaSelecao(tipoAtual));
        model.addAttribute("responsaveis", responsavelService.listarParaSelecao(responsavelAtual));
        model.addAttribute("statusPagamento", StatusPagamento.values());
        if (!form.isNovo()) {
            model.addAttribute("historico", auditoriaService.historico(PagamentoService.ENTIDADE, form.getId()));
        }
        return VIEW_FORM;
    }

    private static void completar(FiltroPagamentos filtro, LocalDate hoje) {
        if (filtro.getSemana() == null) {
            filtro.setSemana(hoje);
        }
    }

    private static String tituloSemana(FiltroPagamentos filtro) {
        return "Semana de " + DATA.format(filtro.getInicio()) + " a " + DATA.format(filtro.getFim());
    }
}
