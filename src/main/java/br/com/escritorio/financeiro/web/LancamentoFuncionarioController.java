package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.FormaPagamento;
import br.com.escritorio.financeiro.domain.Funcionario;
import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import br.com.escritorio.financeiro.domain.TipoLancamentoFuncionario;
import br.com.escritorio.financeiro.domain.ValorPorExtenso;
import br.com.escritorio.financeiro.dto.LancamentoFuncionarioForm;
import br.com.escritorio.financeiro.service.FechamentoFuncionarioService;
import br.com.escritorio.financeiro.service.FuncionarioService;
import br.com.escritorio.financeiro.service.LancamentoFuncionarioService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Vales, descontos, bonificações, horas extras e reembolsos lançados na ficha do funcionário. */
@Controller
public class LancamentoFuncionarioController {

    private static final String VIEW_FORM = "funcionarios/lancamento-form";
    private static final DateTimeFormatter DATA_POR_EXTENSO =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.of("pt", "BR"));

    private final LancamentoFuncionarioService lancamentoService;
    private final FuncionarioService funcionarioService;
    private final FechamentoFuncionarioService fechamentoService;
    private final String nomeEscritorio;

    public LancamentoFuncionarioController(LancamentoFuncionarioService lancamentoService,
                                           FuncionarioService funcionarioService,
                                           FechamentoFuncionarioService fechamentoService,
                                           @Value("${app.escritorio.nome}") String nomeEscritorio) {
        this.lancamentoService = lancamentoService;
        this.funcionarioService = funcionarioService;
        this.fechamentoService = fechamentoService;
        this.nomeEscritorio = nomeEscritorio;
    }

    @GetMapping("/funcionarios/{funcionarioId}/lancamentos/novo")
    public String novo(@PathVariable Long funcionarioId,
                       @RequestParam(required = false) TipoLancamentoFuncionario tipo,
                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
                       Model model) {
        LocalDate hoje = lancamentoService.hoje();
        LancamentoFuncionarioForm form = new LancamentoFuncionarioForm();
        form.setTipo(tipo != null ? tipo : TipoLancamentoFuncionario.VALE);
        form.setData(mes != null && mes.isBefore(YearMonth.from(hoje)) ? mes.atEndOfMonth() : hoje);
        form.setCompetencia(mes);
        return exibirFormulario(funcionarioService.buscar(funcionarioId), form, model);
    }

    @PostMapping("/funcionarios/{funcionarioId}/lancamentos")
    public String criar(@PathVariable Long funcionarioId, @Valid @ModelAttribute("form") LancamentoFuncionarioForm form,
                        BindingResult resultado, @RequestParam(required = false) String outro, Model model,
                        RedirectAttributes redirect) {
        Funcionario funcionario = funcionarioService.buscar(funcionarioId);
        if (!resultado.hasErrors()) {
            try {
                LancamentoFuncionario lancamento = lancamentoService.criar(funcionarioId, form);
                redirect.addFlashAttribute("sucesso", lancamento.getTipo().getRotulo() + " lançado para "
                        + funcionario.getNome() + ".");
                YearMonth competencia = lancamento.getCompetencia();
                return outro != null
                        ? "redirect:/funcionarios/" + funcionarioId + "/lancamentos/novo?tipo=" + lancamento.getTipo()
                        + "&mes=" + competencia
                        : "redirect:/funcionarios/" + funcionarioId + "?mes=" + competencia;
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirFormulario(funcionario, form, model);
    }

    @GetMapping("/funcionarios/lancamentos/{id}")
    public String detalhar(@PathVariable Long id, Model model) {
        LancamentoFuncionario lancamento = lancamentoService.buscar(id);
        model.addAttribute("lancamento", lancamento);
        model.addAttribute("historico", lancamentoService.historico(id));
        model.addAttribute("mesPago", fechamentoService.isPago(lancamento.getFuncionario().getId(),
                lancamento.getCompetencia()));
        return "funcionarios/lancamento-detalhe";
    }

    @GetMapping("/funcionarios/lancamentos/{id}/editar")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        LancamentoFuncionario lancamento = lancamentoService.buscar(id);
        String bloqueio = bloqueioDeAlteracao(lancamento);
        if (bloqueio != null) {
            redirect.addFlashAttribute("erro", bloqueio);
            return "redirect:/funcionarios/lancamentos/" + id;
        }
        return exibirFormulario(lancamento.getFuncionario(), LancamentoFuncionarioForm.de(lancamento), model);
    }

    @PostMapping("/funcionarios/lancamentos/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") LancamentoFuncionarioForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        LancamentoFuncionario lancamento = lancamentoService.buscar(id);
        form.setId(id);
        if (!resultado.hasErrors()) {
            try {
                lancamentoService.atualizar(id, form);
                redirect.addFlashAttribute("sucesso", "Lançamento atualizado.");
                return "redirect:/funcionarios/lancamentos/" + id;
            } catch (RegraNegocioException e) {
                ErrosFormulario.rejeitar(resultado, e);
            }
        }
        return exibirFormulario(lancamento.getFuncionario(), form, model);
    }

    @GetMapping("/funcionarios/lancamentos/{id}/recibo")
    public String recibo(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        LancamentoFuncionario lancamento = lancamentoService.buscar(id);
        if (!lancamento.isAtivo() || lancamento.getTipo() != TipoLancamentoFuncionario.VALE) {
            redirect.addFlashAttribute("erro", "O recibo só existe para vales ativos.");
            return "redirect:/funcionarios/lancamentos/" + id;
        }
        model.addAttribute("lancamento", lancamento);
        model.addAttribute("extenso", ValorPorExtenso.reais(lancamento.getValor()));
        model.addAttribute("competencia", TituloPeriodo.mes(lancamento.getCompetencia()));
        model.addAttribute("dataPorExtenso", DATA_POR_EXTENSO.format(lancamento.getData()));
        model.addAttribute("escritorio", nomeEscritorio);
        return "funcionarios/recibo-vale";
    }

    @PostMapping("/funcionarios/lancamentos/{id}/cancelar")
    public String cancelar(@PathVariable Long id, @RequestParam(required = false) String motivo,
                           RedirectAttributes redirect) {
        try {
            lancamentoService.cancelar(id, motivo);
            redirect.addFlashAttribute("sucesso",
                    "Lançamento cancelado. Ele sai do acerto, mas continua registrado no histórico.");
        } catch (RegraNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/funcionarios/lancamentos/" + id;
    }

    private String bloqueioDeAlteracao(LancamentoFuncionario lancamento) {
        if (lancamento.isCancelado()) {
            return "Lançamentos cancelados não podem ser alterados.";
        }
        if (fechamentoService.isPago(lancamento.getFuncionario().getId(), lancamento.getCompetencia())) {
            return "O acerto desta competência já foi pago. Peça a quem pode reabrir o acerto para reabrir o mês "
                    + "antes de alterar.";
        }
        return null;
    }

    private String exibirFormulario(Funcionario funcionario, LancamentoFuncionarioForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("funcionario", funcionario);
        model.addAttribute("tipos", TipoLancamentoFuncionario.values());
        model.addAttribute("formas", FormaPagamento.values());
        model.addAttribute("hoje", lancamentoService.hoje());
        return VIEW_FORM;
    }
}
