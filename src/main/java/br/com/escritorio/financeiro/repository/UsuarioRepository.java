package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.Perfil;
import br.com.escritorio.financeiro.domain.Permissao;
import br.com.escritorio.financeiro.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    List<Usuario> findAllByOrderByNomeAsc();

    long countByPerfilAndAtivoTrue(Perfil perfil);

    @Query("select u.perfil.id, count(u) from Usuario u group by u.perfil.id")
    List<Object[]> contarPorPerfil();

    /** Usuários ativos, fora o informado, que têm a permissão pelo perfil (o perfil do sistema tem todas). */
    @Query("""
            select count(u) from Usuario u
            where u.ativo = true and u.id <> :usuarioId
              and (u.perfil.sistema = true
                   or exists (select 1 from Perfil p join p.permissoes permissao
                              where p = u.perfil and permissao = :permissao))
            """)
    long contarAtivosComPermissaoExcetoUsuario(@Param("permissao") Permissao permissao,
                                                @Param("usuarioId") Long usuarioId);

    /** Usuários ativos de outros perfis que têm a permissão (o perfil do sistema tem todas). */
    @Query("""
            select count(u) from Usuario u
            where u.ativo = true and u.perfil.id <> :perfilId
              and (u.perfil.sistema = true
                   or exists (select 1 from Perfil p join p.permissoes permissao
                              where p = u.perfil and permissao = :permissao))
            """)
    long contarAtivosComPermissaoForaDoPerfil(@Param("permissao") Permissao permissao,
                                              @Param("perfilId") Long perfilId);
}
