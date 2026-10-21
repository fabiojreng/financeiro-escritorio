package br.com.escritorio.financeiro.security;

import br.com.escritorio.financeiro.domain.Permissao;
import org.springframework.security.core.Authentication;

public final class Permissoes {

    private Permissoes() {
    }

    public static boolean possui(Authentication auth, Permissao permissao) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(authority -> permissao.name().equals(authority.getAuthority()));
    }
}
