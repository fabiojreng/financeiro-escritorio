package br.com.escritorio.financeiro.security;

import br.com.escritorio.financeiro.domain.Usuario;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class UsuarioAutenticado extends User {

    private final Long id;
    private final String nome;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getUsername(), usuario.getSenhaHash(), usuario.isAtivo(), true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
        this.id = usuario.getId();
        this.nome = usuario.getNome();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}
