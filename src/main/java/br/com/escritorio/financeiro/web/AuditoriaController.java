package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.repository.AuditoriaRepository;
import br.com.escritorio.financeiro.service.AuditoriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaController(AuditoriaService auditoriaService, AuditoriaRepository auditoriaRepository) {
        this.auditoriaService = auditoriaService;
        this.auditoriaRepository = auditoriaRepository;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String entidade,
                         @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("registros", auditoriaService.listar(entidade, pagina));
        model.addAttribute("entidades", auditoriaRepository.listarEntidades());
        model.addAttribute("entidade", entidade);
        return "auditoria/lista";
    }
}
