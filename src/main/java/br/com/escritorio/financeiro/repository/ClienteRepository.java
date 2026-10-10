package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCpf(String cpf);

    /**
     * @param padraoNome padrão LIKE já em minúsculas e sem acentos
     * @param padraoCpf  padrão LIKE só com dígitos, ou vazio para não filtrar por CPF
     */
    @Query(value = """
            select * from cliente c
            where (lower(unaccent(c.nome)) like :padraoNome
                   or (:padraoCpf <> '' and c.cpf like :padraoCpf))
              and c.ativo in (:situacoes)
            order by lower(unaccent(c.nome)), c.id
            """,
            countQuery = """
            select count(*) from cliente c
            where (lower(unaccent(c.nome)) like :padraoNome
                   or (:padraoCpf <> '' and c.cpf like :padraoCpf))
              and c.ativo in (:situacoes)
            """,
            nativeQuery = true)
    Page<Cliente> pesquisar(@Param("padraoNome") String padraoNome,
                            @Param("padraoCpf") String padraoCpf,
                            @Param("situacoes") Collection<Boolean> situacoes,
                            Pageable pageable);
}
