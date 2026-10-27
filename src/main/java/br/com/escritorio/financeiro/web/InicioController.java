package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.security.Permissoes;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class InicioController {

    /** Tela inicial de cada perfil: a primeira desta lista que o usuário pode ver. */
    private static final Map<Permissao, String> TELAS_INICIAIS = new LinkedHashMap<>();

    static {
        TELAS_INICIAIS.put(Permissao.AGENDA_VISUALIZAR, "/agenda-pagamentos");
        TELAS_INICIAIS.put(Permissao.PERICIAS_VISUALIZAR, "/pericias");
        TELAS_INICIAIS.put(Permissao.HONORARIOS_VISUALIZAR, "/honorarios");
        TELAS_INICIAIS.put(Permissao.REPASSES_VISUALIZAR, "/repasses");
        TELAS_INICIAIS.put(Permissao.DESPESAS_VISUALIZAR, "/despesas");
        TELAS_INICIAIS.put(Permissao.FUNCIONARIOS_VISUALIZAR, "/funcionarios");
        TELAS_INICIAIS.put(Permissao.RELATORIOS_VISUALIZAR, "/relatorios");
        TELAS_INICIAIS.put(Permissao.CLIENTES_VISUALIZAR, "/clientes");
        TELAS_INICIAIS.put(Permissao.CADASTROS_RESPONSAVEIS, "/responsaveis");
        TELAS_INICIAIS.put(Permissao.CADASTROS_TIPOS_SERVICO, "/tipos-servico");
        TELAS_INICIAIS.put(Permissao.CADASTROS_TIPOS_PERICIA, "/tipos-pericia");
        TELAS_INICIAIS.put(Permissao.CADASTROS_LOCAIS_PERICIA, "/locais-pericia");
        TELAS_INICIAIS.put(Permissao.DESPESAS_CADASTROS, "/categorias-despesa");
        TELAS_INICIAIS.put(Permissao.ADMIN_USUARIOS, "/usuarios");
        TELAS_INICIAIS.put(Permissao.ADMIN_PERFIS, "/perfis");
        TELAS_INICIAIS.put(Permissao.ADMIN_AUDITORIA, "/auditoria");
    }

    @GetMapping("/")
    public String inicio(Authentication auth) {
        return TELAS_INICIAIS.entrySet().stream()
                .filter(tela -> Permissoes.possui(auth, tela.getKey()))
                .map(tela -> "redirect:" + tela.getValue())
                .findFirst()
                .orElse("redirect:/acesso-negado");
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
