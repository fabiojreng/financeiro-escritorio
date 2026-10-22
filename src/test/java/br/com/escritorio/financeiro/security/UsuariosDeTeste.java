package br.com.escritorio.financeiro.security;

import br.com.escritorio.financeiro.domain.ModuloPermissao;
import br.com.escritorio.financeiro.domain.Permissao;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import static br.com.escritorio.financeiro.domain.Permissao.ADMIN_AUDITORIA;
import static br.com.escritorio.financeiro.domain.Permissao.ADMIN_PERFIS;
import static br.com.escritorio.financeiro.domain.Permissao.ADMIN_USUARIOS;
import static br.com.escritorio.financeiro.domain.Permissao.DESPESAS_REABRIR_FECHAMENTO;
import static br.com.escritorio.financeiro.domain.Permissao.RELATORIOS_VISUALIZAR;
import static br.com.escritorio.financeiro.domain.Permissao.REPASSES_EDITAR;
import static br.com.escritorio.financeiro.domain.Permissao.REPASSES_VISUALIZAR;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Usuários simulados para o MockMvc, autenticados com as mesmas permissões dos perfis criados pela migration V20.
 */
public final class UsuariosDeTeste {

    public static final Set<Permissao> PERMISSOES_OPERADOR = permissoesDoOperador();

    private UsuariosDeTeste() {
    }

    public static RequestPostProcessor operador() {
        return operador("ana");
    }

    public static RequestPostProcessor operador(String username) {
        return comPermissoes(username, PERMISSOES_OPERADOR);
    }

    public static RequestPostProcessor operadorCom(Permissao... extras) {
        Set<Permissao> permissoes = EnumSet.copyOf(PERMISSOES_OPERADOR);
        permissoes.addAll(Arrays.asList(extras));
        return comPermissoes("ana", permissoes);
    }

    public static RequestPostProcessor administrador() {
        return administrador("chefe");
    }

    public static RequestPostProcessor administrador(String username) {
        return comPermissoes(username, EnumSet.allOf(Permissao.class));
    }

    public static RequestPostProcessor comPermissoes(String username, Permissao primeira, Permissao... outras) {
        return comPermissoes(username, EnumSet.of(primeira, outras));
    }

    public static RequestPostProcessor comPermissoes(String username, Collection<Permissao> permissoes) {
        return user(username).authorities(permissoes.stream()
                .map(permissao -> new SimpleGrantedAuthority(permissao.name()))
                .toList());
    }

    private static Set<Permissao> permissoesDoOperador() {
        Set<Permissao> permissoes = EnumSet.allOf(Permissao.class);
        permissoes.removeAll(EnumSet.of(REPASSES_VISUALIZAR, REPASSES_EDITAR, RELATORIOS_VISUALIZAR,
                DESPESAS_REABRIR_FECHAMENTO, ADMIN_USUARIOS, ADMIN_PERFIS, ADMIN_AUDITORIA));
        permissoes.removeIf(permissao -> permissao.getModulo() == ModuloPermissao.FUNCIONARIOS);
        return Set.copyOf(permissoes);
    }
}
