package br.com.escritorio.financeiro.web;

import br.com.escritorio.financeiro.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ModeloGlobalAdvice {

    @ModelAttribute("caminhoAtual")
    public String caminhoAtual(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
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
