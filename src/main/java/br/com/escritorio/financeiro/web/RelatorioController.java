package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import br.com.escritorio.financeiro.dto.AgendaHonorarios;
import br.com.escritorio.financeiro.dto.AgendaPagamentos;
import br.com.escritorio.financeiro.dto.FiltroRelatorio;
import br.com.escritorio.financeiro.dto.FiltroSituacao;
import br.com.escritorio.financeiro.dto.LinhaHonorario;
import br.com.escritorio.financeiro.service.ClienteService;
import br.com.escritorio.financeiro.service.RelatorioService;
import br.com.escritorio.financeiro.service.ResponsavelService;
import br.com.escritorio.financeiro.service.TipoServicoService;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;
    private final ResponsavelService responsavelService;
    private final TipoServicoService tipoServicoService;
    private final ClienteService clienteService;
    private final Clock relogio;

    public RelatorioController(RelatorioService relatorioService, ResponsavelService responsavelService,
                               TipoServicoService tipoServicoService, ClienteService clienteService, Clock relogio) {
        this.relatorioService = relatorioService;
        this.responsavelService = responsavelService;
        this.tipoServicoService = tipoServicoService;
        this.clienteService = clienteService;
        this.relogio = relogio;
    }

    @GetMapping
    public String inicio() {
        return "redirect:/relatorios/pagamentos";
    }

    @GetMapping("/pagamentos")
    public String pagamentos(@ModelAttribute("filtro") FiltroRelatorio filtro, BindingResult parametrosInvalidos,
                             HttpServletRequest request, Model model) {
        preparar(filtro, request, model, Arrays.asList(StatusPagamento.values()));
        model.addAttribute("relatorio", relatorioService.pagamentos(filtro));
        return "relatorios/pagamentos";
    }

    @GetMapping("/pagamentos.csv")
    public ResponseEntity<byte[]> pagamentosCsv(@ModelAttribute("filtro") FiltroRelatorio filtro,
                                                BindingResult parametrosInvalidos) {
        completar(filtro);
        AgendaPagamentos relatorio = relatorioService.pagamentos(filtro);
        Csv csv = new Csv("Dia de recebimento", "Cliente", "Benefício", "Responsável", "Banco", "Status",
                "Valor (R$)", "Honorário (R$)", "Situação do honorário", "Repasse (R$)", "Situação do repasse");
        for (PagamentoAgendado p : relatorio.pagamentos()) {
            linhaPagamento(csv, p);
        }
        return exportar(csv, "pagamentos", "pagamentos", filtro);
    }

    @GetMapping("/honorarios")
    public String honorarios(@ModelAttribute("filtro") FiltroRelatorio filtro, BindingResult parametrosInvalidos,
                             HttpServletRequest request, Model model) {
        preparar(filtro, request, model, Arrays.asList(SituacaoHonorarioPagamento.values()));
        model.addAttribute("relatorio", relatorioService.honorarios(filtro));
        return "relatorios/honorarios";
    }

    @GetMapping("/honorarios.csv")
    public ResponseEntity<byte[]> honorariosCsv(@ModelAttribute("filtro") FiltroRelatorio filtro,
                                                BindingResult parametrosInvalidos) {
        completar(filtro);
        AgendaHonorarios relatorio = relatorioService.honorarios(filtro);
        Csv csv = new Csv("Dia de recebimento", "Cliente", "Benefício", "Responsável", "Original (R$)",
                "Desconto (R$)", "Negociado (R$)", "Recebido (R$)", "Saldo (R$)", "Situação");
        for (LinhaHonorario linha : relatorio.linhas()) {
            PagamentoAgendado p = linha.pagamento();
            csv.data(p.getDataRecebimento())
                    .texto(p.getCliente().getNome())
                    .texto(p.getTipoServico().getNome())
                    .texto(p.getResponsavel().getNome())
                    .valor(linha.valores().original())
                    .valor(linha.valores().desconto())
                    .valor(linha.valores().temNegociacao() ? linha.valores().negociado() : null)
                    .valor(linha.valores().recebido())
                    .valor(linha.valores().saldo())
                    .texto(p.getSituacaoHonorario().getRotulo())
                    .fimDaLinha();
        }
        return exportar(csv, "honorários", "honorarios", filtro);
    }

    @GetMapping("/repasses")
    public String repasses(@ModelAttribute("filtro") FiltroRelatorio filtro, BindingResult parametrosInvalidos,
                           HttpServletRequest request, Model model) {
        preparar(filtro, request, model, Arrays.asList(SituacaoRepassePagamento.values()));
        model.addAttribute("relatorio", relatorioService.repasses(filtro));
        return "relatorios/repasses";
    }

    @GetMapping("/repasses.csv")
    public ResponseEntity<byte[]> repassesCsv(@ModelAttribute("filtro") FiltroRelatorio filtro,
                                              BindingResult parametrosInvalidos) {
        completar(filtro);
        AgendaPagamentos relatorio = relatorioService.repasses(filtro);
        Csv csv = new Csv("Dia de recebimento", "Cliente", "Benefício", "Responsável", "Repasse (R$)", "Situação");
        for (PagamentoAgendado p : relatorio.pagamentos()) {
            csv.data(p.getDataRecebimento())
                    .texto(p.getCliente().getNome())
                    .texto(p.getTipoServico().getNome())
                    .texto(p.getResponsavel().getNome())
                    .valor(p.getValorRepasse())
                    .texto(p.getSituacaoRepasse().getRotulo())
                    .fimDaLinha();
        }
        return exportar(csv, "repasses", "repasses", filtro);
    }

    @GetMapping("/historico")
    public String historico(@RequestParam(required = false) Long clienteId,
                            @RequestParam(required = false) String busca, Model model) {
        if (clienteId == null) {
            model.addAttribute("busca", busca);
            model.addAttribute("clientes", clienteService.pesquisar(busca, FiltroSituacao.TODOS, 0));
            return "relatorios/historico-escolher";
        }
        model.addAttribute("historico", relatorioService.historico(clienteId));
        model.addAttribute("hoje", LocalDate.now(relogio));
        return "relatorios/historico";
    }

    private static void linhaPagamento(Csv csv, PagamentoAgendado p) {
        csv.data(p.getDataRecebimento())
                .texto(p.getCliente().getNome())
                .texto(p.getTipoServico().getNome())
                .texto(p.getResponsavel().getNome())
                .texto(p.getBanco())
                .texto(p.getStatus().getRotulo())
                .valor(p.getValor())
                .valor(p.getValorHonorario())
                .texto(p.getValorHonorario() == null ? "" : p.getSituacaoHonorario().getRotulo())
                .valor(p.getValorRepasse())
                .texto(p.getValorRepasse() == null ? "" : p.getSituacaoRepasse().getRotulo())
                .fimDaLinha();
    }

    private void preparar(FiltroRelatorio filtro, HttpServletRequest request, Model model, List<? extends Enum<?>> situacoes) {
        completar(filtro);
        LocalDate hoje = LocalDate.now(relogio);
        model.addAttribute("tituloPeriodo", tituloPeriodo(filtro));
        model.addAttribute("mesAnterior", filtro.getMes().minusMonths(1));
        model.addAttribute("mesSeguinte", filtro.getMes().plusMonths(1));
        model.addAttribute("mesAtual", YearMonth.from(hoje));
        model.addAttribute("hoje", hoje);
        model.addAttribute("responsaveis", responsavelService.listar());
        model.addAttribute("tipos", tipoServicoService.listar());
        model.addAttribute("situacoes", situacoes);
        String consulta = request.getQueryString();
        model.addAttribute("linkCsv", request.getRequestURI() + ".csv" + (consulta == null ? "" : "?" + consulta));
    }

    private void completar(FiltroRelatorio filtro) {
        if (filtro.getMes() == null) {
            filtro.setMes(YearMonth.now(relogio));
        }
    }

    private ResponseEntity<byte[]> exportar(Csv csv, String relatorio, String arquivo, FiltroRelatorio filtro) {
        relatorioService.registrarExportacao(relatorio, tituloPeriodo(filtro), csv.getLinhas());
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(arquivo + "-" + filtro.getSufixoArquivo() + ".csv").build().toString())
                .cacheControl(CacheControl.noStore())
                .body(csv.bytes());
    }

    private static String tituloPeriodo(FiltroRelatorio filtro) {
        return filtro.isPeriodoPersonalizado()
                ? TituloPeriodo.periodo(filtro.getInicioOrdenado(), filtro.getFimOrdenado())
                : TituloPeriodo.mes(filtro.getMes());
    }
}
