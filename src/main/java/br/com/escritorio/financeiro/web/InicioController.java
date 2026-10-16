package br.com.escritorio.financeiro.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio() {
        return "redirect:/agenda-pagamentos";
    }

    @GetMapping({"/agenda", "/painel"})
    public String agendaAntiga() {
        return "redirect:/agenda-pagamentos";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}
