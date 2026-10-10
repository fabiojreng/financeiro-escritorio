package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import br.com.escritorio.financeiro.domain.SituacaoRepasse;
import br.com.escritorio.financeiro.domain.TotaisRepasses;
import br.com.escritorio.financeiro.dto.AcordoComTotais;
import br.com.escritorio.financeiro.dto.AgendaMensal;
import br.com.escritorio.financeiro.dto.FiltroRecebimento;
import br.com.escritorio.financeiro.dto.FiltroRelatorio;
import br.com.escritorio.financeiro.dto.FiltroSaldo;
import br.com.escritorio.financeiro.dto.FiltroSituacao;
import br.com.escritorio.financeiro.dto.RelatorioAcordos;
import br.com.escritorio.financeiro.dto.RelatorioRecebimentos;
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

/**
 * Relatórios por período e exportação CSV com os mesmos filtros. Todas as rotas exigem usuário
 * autenticado (regra geral do {@code SecurityConfig}); o CSV não inclui CPF nem telefone.
 */
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
        return "redirect:/relatorios/previsoes";
    }

    @GetMapping("/previsoes")
    public String previsoes(@ModelAttribute("filtro") FiltroRelatorio filtro, BindingResult parametrosInvalidos,
                            HttpServletRequest request, Model model) {
        preparar(filtro, request, model, Arrays.asList(SituacaoPrevisao.values()));
        model.addAttribute("relatorio", relatorioService.previsoes(filtro));
        return "relatorios/previsoes";
    }

    @GetMapping("/previsoes.csv")
    public ResponseEntity<byte[]> previsoesCsv(@ModelAttribute("filtro") FiltroRelatorio filtro,
                                               BindingResult parametrosInvalidos) {
        completar(filtro);
        AgendaMensal relatorio = relatorioService.previsoes(filtro);
        Csv csv = new Csv("Data prevista", "Cliente", "Tipo de serviço", "Responsável", "Tipo de pagamento",
                "Situação", "Valor estimado (R$)", "Data da liberação", "Valor confirmado (R$)");
        for (PrevisaoBeneficio p : relatorio.previsoes()) {
            csv.data(p.getDataPrevista())
                    .texto(p.getAtendimento().getCliente().getNome())
                    .texto(p.getAtendimento().getTipoServico().getNome())
                    .texto(p.getAtendimento().getResponsavel().getNome())
                    .texto(p.getTipoPagamento().getRotulo())
                    .texto(p.getSituacao().getRotulo())
                    .valor(p.getValorEstimado())
                    .data(p.getDataEfetiva())
                    .valor(p.getValorConfirmado())
                    .fimDaLinha();
        }
        return exportar(csv, "previsões de benefício", "previsoes-beneficio", filtro);
    }

    @GetMapping("/recebimentos")
    public String recebimentos(@ModelAttribute("filtro") FiltroRelatorio filtro, BindingResult parametrosInvalidos,
                               HttpServletRequest request, Model model) {
        preparar(filtro, request, model, Arrays.asList(FiltroRecebimento.values()));
        model.addAttribute("relatorio", relatorioService.recebimentos(filtro));
        return "relatorios/recebimentos";
    }

    @GetMapping("/recebimentos.csv")
    public ResponseEntity<byte[]> recebimentosCsv(@ModelAttribute("filtro") FiltroRelatorio filtro,
                                                  BindingResult parametrosInvalidos) {
        completar(filtro);
        RelatorioRecebimentos relatorio = relatorioService.recebimentos(filtro);
        Csv csv = new Csv("Data do recebimento", "Cliente", "Tipo de serviço", "Responsável", "Parcela",
                "Forma de pagamento", "Valor (R$)", "Situação", "Motivo do cancelamento");
        for (RecebimentoHonorario r : relatorio.recebimentos()) {
            csv.data(r.getDataRecebimento())
                    .texto(r.getAcordo().getAtendimento().getCliente().getNome())
                    .texto(r.getAcordo().getAtendimento().getTipoServico().getNome())
                    .texto(r.getAcordo().getAtendimento().getResponsavel().getNome())
                    .texto(r.getParcela() == null ? "" : Integer.toString(r.getParcela().getNumero()))
                    .texto(r.getFormaPagamento().getRotulo())
                    .valor(r.getValor())
                    .texto(r.isCancelado() ? "Cancelado" : "Confirmado")
                    .texto(r.getMotivoCancelamento())
                    .fimDaLinha();
        }
        return exportar(csv, "recebimentos de honorários", "recebimentos-honorarios", filtro);
    }

    @GetMapping("/acordos")
    public String acordos(@ModelAttribute("filtro") FiltroRelatorio filtro, BindingResult parametrosInvalidos,
                          HttpServletRequest request, Model model) {
        preparar(filtro, request, model, opcoesDeSaldo());
        model.addAttribute("relatorio", relatorioService.acordos(filtro));
        return "relatorios/acordos";
    }

    @GetMapping("/acordos.csv")
    public ResponseEntity<byte[]> acordosCsv(@ModelAttribute("filtro") FiltroRelatorio filtro,
                                             BindingResult parametrosInvalidos) {
        completar(filtro);
        RelatorioAcordos relatorio = relatorioService.acordos(filtro);
        Csv csv = new Csv("Data do acordo", "Cliente", "Tipo de serviço", "Responsável", "Forma de cobrança",
                "Percentual (%)", "Base de cálculo", "Valor contratado (R$)", "Recebido (R$)", "Saldo pendente (R$)");
        for (AcordoComTotais linha : relatorio.acordos()) {
            AcordoHonorarios a = linha.acordo();
            csv.data(a.getDataAcordo())
                    .texto(a.getAtendimento().getCliente().getNome())
                    .texto(a.getAtendimento().getTipoServico().getNome())
                    .texto(a.getAtendimento().getResponsavel().getNome())
                    .texto(a.getFormaCobranca().getRotulo())
                    .valor(a.getPercentual())
                    .texto(a.getBaseCalculo())
                    .valor(a.getValorContratado())
                    .valor(linha.recebido())
                    .valor(linha.saldo())
                    .fimDaLinha();
        }
        return exportar(csv, "acordos de honorários", "acordos-honorarios", filtro);
    }

    @GetMapping("/repasses")
    public String repasses(@ModelAttribute("filtro") FiltroRelatorio filtro, BindingResult parametrosInvalidos,
                           HttpServletRequest request, Model model) {
        preparar(filtro, request, model, Arrays.asList(SituacaoRepasse.values()));
        List<Repasse> repasses = relatorioService.repasses(filtro);
        model.addAttribute("repasses", repasses);
        model.addAttribute("totais", TotaisRepasses.de(repasses));
        return "relatorios/repasses";
    }

    @GetMapping("/repasses.csv")
    public ResponseEntity<byte[]> repassesCsv(@ModelAttribute("filtro") FiltroRelatorio filtro,
                                              BindingResult parametrosInvalidos) {
        completar(filtro);
        Csv csv = new Csv("Data prevista", "Cliente", "Tipo de serviço", "Destinatário", "Motivo",
                "Valor previsto (R$)", "Data do pagamento", "Valor pago (R$)", "Situação", "Motivo do cancelamento");
        for (Repasse r : relatorioService.repasses(filtro)) {
            csv.data(r.getDataPrevista())
                    .texto(r.getAtendimento().getCliente().getNome())
                    .texto(r.getAtendimento().getTipoServico().getNome())
                    .texto(r.getDestinatario().getNome())
                    .texto(r.getDescricao())
                    .valor(r.getValorPrevisto())
                    .data(r.getDataPagamento())
                    .valor(r.getValorPago())
                    .texto(r.getSituacao().getRotulo())
                    .texto(r.getMotivoCancelamento())
                    .fimDaLinha();
        }
        return exportar(csv, "repasses internos", "repasses", filtro);
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

    private static List<FiltroSaldo> opcoesDeSaldo() {
        return Arrays.stream(FiltroSaldo.values()).filter(f -> f != FiltroSaldo.TODOS).toList();
    }
}
