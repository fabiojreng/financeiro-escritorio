package br.com.escritorio.financeiro.security;

import br.com.escritorio.financeiro.domain.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Usuário logado. As permissões do perfil viram authorities e só são recarregadas no próximo login. */
public class UsuarioAutenticado extends User {

    private final Long id;
    private final String nome;
    private final String nomePerfil;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getUsername(), usuario.getSenhaHash(), usuario.isAtivo(), true, true, true,
                usuario.getPerfil().permissoesEfetivas().stream()
                        .map(permissao -> new SimpleGrantedAuthority(permissao.name()))
                        .toList());
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.nomePerfil = usuario.getPerfil().getNome();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getNomePerfil() {
        return nomePerfil;
    }
}
