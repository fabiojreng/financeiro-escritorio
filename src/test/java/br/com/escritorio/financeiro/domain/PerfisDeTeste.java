package br.com.escritorio.financeiro.domain;

import br.com.escritorio.financeiro.security.UsuariosDeTeste;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

/** Perfis montados em memória, com id, para testes unitários sem banco. */
public final class PerfisDeTeste {

    private PerfisDeTeste() {
    }

    public static Perfil administrador() {
        Perfil perfil = perfil(1L, "Administrador", List.of());
        ReflectionTestUtils.setField(perfil, "sistema", true);
        return perfil;
    }

    public static Perfil operador() {
        return perfil(2L, "Operador", UsuariosDeTeste.PERMISSOES_OPERADOR);
    }

    public static Perfil perfil(Long id, String nome, Collection<Permissao> permissoes) {
        Perfil perfil = new Perfil(nome, null, permissoes);
        ReflectionTestUtils.setField(perfil, "id", id);
        return perfil;
    }
}
