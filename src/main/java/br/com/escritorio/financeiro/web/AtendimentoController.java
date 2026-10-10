package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import br.com.escritorio.financeiro.domain.TotaisRepasses;
import br.com.escritorio.financeiro.dto.AtendimentoForm;
import br.com.escritorio.financeiro.dto.FiltroSituacao;
import br.com.escritorio.financeiro.service.AtendimentoService;
import br.com.escritorio.financeiro.service.AuditoriaService;
import br.com.escritorio.financeiro.service.ClienteService;
import br.com.escritorio.financeiro.service.HonorariosService;
import br.com.escritorio.financeiro.service.PrevisaoService;
import br.com.escritorio.financeiro.service.RegraNegocioException;
import br.com.escritorio.financeiro.service.RepasseService;
import br.com.escritorio.financeiro.service.ResponsavelService;
import br.com.escritorio.financeiro.service.TipoServicoService;
import jakarta.validation.Valid;
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

import java.util.List;

@Controller
@RequestMapping("/atendimentos")
public class AtendimentoController {

    private static final String VIEW_FORM = "atendimentos/form";

    private final AtendimentoService atendimentoService;
    private final ClienteService clienteService;
    private final TipoServicoService tipoServicoService;
    private final ResponsavelService responsavelService;
    private final PrevisaoService previsaoService;
    private final HonorariosService honorariosService;
    private final RepasseService repasseService;
    private final AuditoriaService auditoriaService;

    public AtendimentoController(AtendimentoService atendimentoService, ClienteService clienteService,
                                 TipoServicoService tipoServicoService, ResponsavelService responsavelService,
                                 PrevisaoService previsaoService, HonorariosService honorariosService,
                                 RepasseService repasseService, AuditoriaService auditoriaService) {
        this.atendimentoService = atendimentoService;
        this.clienteService = clienteService;
        this.tipoServicoService = tipoServicoService;
        this.responsavelService = responsavelService;
        this.previsaoService = previsaoService;
        this.honorariosService = honorariosService;
        this.repasseService = repasseService;
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busca,
                         @RequestParam(required = false) Long responsavelId,
                         @RequestParam(required = false) Long tipoServicoId,
                         @RequestParam(required = false) SituacaoAtendimento situacao,
                         @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("atendimentos",
                atendimentoService.pesquisar(busca, responsavelId, tipoServicoId, situacao, pagina));
        model.addAttribute("busca", busca);
        model.addAttribute("responsavelId", responsavelId);
        model.addAttribute("tipoServicoId", tipoServicoId);
        model.addAttribute("situacao", situacao);
        model.addAttribute("responsaveis", responsavelService.listar());
        model.addAttribute("tipos", tipoServicoService.listar());
        model.addAttribute("situacoes", SituacaoAtendimento.values());
        return "atendimentos/lista";
    }

    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) Long clienteId,
                       @RequestParam(required = false) String busca, Model model) {
        if (clienteId == null) {
            model.addAttribute("busca", busca);
            model.addAttribute("clientes", clienteService.pesquisar(busca, FiltroSituacao.ATIVOS, 0));
            return "atendimentos/escolher-cliente";
        }
        AtendimentoForm form = new AtendimentoForm();
        form.setClienteId(clienteId);
        return exibirFormulario(form, model);
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("form") AtendimentoForm form, BindingResult resultado,
                        Model model, RedirectAttributes redirect) {
        if (form.getClienteId() == null) {
            return "redirect:/atendimentos/novo";
        }
        if (resultado.hasErrors()) {
            return exibirFormulario(form, model);
        }
        try {
            Atendimento atendimento = atendimentoService.criar(form);
            redirect.addFlashAttribute("sucesso", "Atendimento aberto.");
            return "redirect:/atendimentos/" + atendimento.getId();
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormulario(form, model);
        }
    }

    @GetMapping("/{id}")
    public String detalhar(@PathVariable Long id, Model model) {
        model.addAttribute("atendimento", atendimentoService.buscar(id));
        model.addAttribute("previsoes", previsaoService.listarPorAtendimento(id));
        model.addAttribute("hoje", previsaoService.hoje());
        model.addAttribute("honorarios", honorariosService.resumoDoAtendimento(id));
        List<Repasse> repasses = repasseService.listarPorAtendimento(id);
        model.addAttribute("repasses", repasses);
        model.addAttribute("totaisRepasses", TotaisRepasses.de(repasses));
        model.addAttribute("historico", auditoriaService.historico(AtendimentoService.ENTIDADE, id));
        return "atendimentos/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        return exibirFormulario(AtendimentoForm.de(atendimentoService.buscar(id)), model);
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("form") AtendimentoForm form,
                            BindingResult resultado, Model model, RedirectAttributes redirect) {
        form.setId(id);
        form.setClienteId(atendimentoService.buscar(id).getCliente().getId());
        if (resultado.hasErrors()) {
            return exibirFormulario(form, model);
        }
        try {
            atendimentoService.atualizar(id, form);
            redirect.addFlashAttribute("sucesso", "Atendimento atualizado.");
            return "redirect:/atendimentos/" + id;
        } catch (RegraNegocioException e) {
            ErrosFormulario.rejeitar(resultado, e);
            return exibirFormulario(form, model);
        }
    }

    private String exibirFormulario(AtendimentoForm form, Model model) {
        Long tipoAtual = form.isNovo() ? null : form.getTipoServicoId();
        Long responsavelAtual = form.isNovo() ? null : form.getResponsavelId();
        model.addAttribute("form", form);
        model.addAttribute("cliente", clienteService.buscar(form.getClienteId()));
        model.addAttribute("tipos", tipoServicoService.listarParaSelecao(tipoAtual));
        model.addAttribute("responsaveis", responsavelService.listarParaSelecao(responsavelAtual));
        model.addAttribute("situacoes", SituacaoAtendimento.values());
        return VIEW_FORM;
    }
}
