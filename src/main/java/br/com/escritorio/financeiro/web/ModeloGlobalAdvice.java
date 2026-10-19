package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class ModeloGlobalAdvice {

    /** Área do sistema de cada caminho; define a cor usada no menu e no título da tela. */
    private static final Map<String, List<String>> MODULOS = Map.of(
            "rotina", List.of("/agenda-pagamentos"),
            "financeiro", List.of("/honorarios", "/repasses", "/despesas"),
            "consultas", List.of("/relatorios"),
            "cadastros", List.of("/clientes", "/responsaveis", "/tipos-servico", "/categorias-despesa",
                    "/formas-pagamento-despesa"),
            "admin", List.of("/usuarios", "/auditoria"));

    @ModelAttribute("caminhoAtual")
    public String caminhoAtual(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    @ModelAttribute("modulo")
    public String modulo(HttpServletRequest request) {
        String caminho = caminhoAtual(request);
        return MODULOS.entrySet().stream()
                .filter(modulo -> modulo.getValue().stream()
                        .anyMatch(prefixo -> caminho.equals(prefixo) || caminho.startsWith(prefixo + "/")))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /** Endereço da tela atual, com os filtros e sem o número da página, terminado em "pagina=". */
    @ModelAttribute("enderecoPaginacao")
    public String enderecoPaginacao(HttpServletRequest request) {
        String consulta = request.getQueryString() == null ? "" : Arrays.stream(request.getQueryString().split("&"))
                .filter(parametro -> !parametro.isEmpty() && !parametro.equals("pagina")
                        && !parametro.startsWith("pagina="))
                .collect(Collectors.joining("&"));
        return request.getRequestURI() + "?" + (consulta.isEmpty() ? "" : consulta + "&") + "pagina=";
    }

    @ModelAttribute("nomeUsuario")
    public String nomeUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        if (auth.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario.getNome();
        }
        return auth.getName();
    }
}
