package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Cliente;
import br.com.escritorio.financeiro.dto.ClienteForm;
import br.com.escritorio.financeiro.dto.FiltroSituacao;
import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.ClienteService;
import br.com.escritorio.financeiro.service.HonorarioService;
import br.com.escritorio.financeiro.service.PagamentoService;
import br.com.escritorio.financeiro.service.PericiaService;
import br.com.escritorio.financeiro.service.CpfDuplicadoException;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private static final String VIEW_FORM = "clientes/form";

    private final ClienteService clienteService;
    private final PagamentoService pagamentoService;
    private final HonorarioService honorarioService;
    private final AuditoriaService auditoriaService;
    private final PericiaService periciaService;

    public ClienteController(ClienteService clienteService, PagamentoService pagamentoService,
                             HonorarioService honorarioService, AuditoriaService auditoriaService,
                             PericiaService periciaService) {
        this.clienteService = clienteService;
        this.pagamentoService = pagamentoService;
        this.honorarioService = honorarioService;
        this.auditoriaService = auditoriaService;
        this.periciaService = periciaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) String observacao,
                         @RequestParam(defaultValue = "ATIVOS") FiltroSituacao situacao,
                         @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("clientes", clienteService.pesquisar(busca, observacao, situacao, pagina));
        model.addAttribute("busca", busca);
        model.addAttribute("observacao", observacao);
        model.addAttribute("situacao", situacao);
        model.addAttribute("situacoes", FiltroSituacao.values());
        return "clientes/lista";
    }

    @GetMapping(value = "/sugestoes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<SugestaoCliente> sugestoes(@RequestParam(defaultValue = "") String termo,
                                           @RequestParam(defaultValue = "false") boolean todos) {
        FiltroSituacao situacao = todos ? FiltroSituacao.TODOS : FiltroSituacao.ATIVOS;
        return clienteService.sugerir(termo, situacao).stream().map(SugestaoCliente::de).toList();
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new ClienteForm());
        return VIEW_FORM;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") ClienteForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            Cliente cliente = clienteService.criar(form);
            redirect.addFlashAttribute("sucesso", "Cliente " + cliente.getNome() + " cadastrado.");
            return "redirect:/clientes/" + cliente.getId();
        } catch (RegraNegocioException e) {
            return voltarAoFormulario(resultado, model, e);
        }
    }

    @GetMapping("/{id}")
    public String detalhar(@PathVariable Long id, Model model) {
        List<PagamentoAgendado> pagamentos = pagamentoService.listarPorCliente(id);
        model.addAttribute("cliente", clienteService.buscar(id));
        model.addAttribute("pagamentos", pagamentos);
        model.addAttribute("honorarios", honorarioService.valoresPorPagamento(pagamentos));
        model.addAttribute("pericias", periciaService.doCliente(id));
        model.addAttribute("historico", auditoriaService.historico(ClienteService.ENTIDADE, id));
        return "clientes/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", ClienteForm.de(clienteService.buscar(id)));
        return VIEW_FORM;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") ClienteForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        form.setId(id);
        if (resultado.hasErrors()) {
            return VIEW_FORM;
        }
        try {
            Cliente cliente = clienteService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Cliente " + cliente.getNome() + " atualizado.");
            return "redirect:/clientes/" + id;
        } catch (RegraNegocioException e) {
            return voltarAoFormulario(resultado, model, e);
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        clienteService.alterarSituacao(id, true);
        redirect.addFlashAttribute("sucesso", "Cliente ativado.");
        return "redirect:/clientes/" + id;
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        clienteService.alterarSituacao(id, false);
        redirect.addFlashAttribute("sucesso", "Cliente desativado.");
        return "redirect:/clientes/" + id;
    }

    private String voltarAoFormulario(BindingResult resultado, Model model, RegraNegocioException erro) {
        ErrosFormulario.rejeitar(resultado, erro);
        if (erro instanceof CpfDuplicadoException duplicado) {
            model.addAttribute("clienteExistenteId", duplicado.getClienteExistenteId());
            model.addAttribute("clienteExistenteNome", duplicado.getClienteExistenteNome());
        }
        return VIEW_FORM;
    }
}
