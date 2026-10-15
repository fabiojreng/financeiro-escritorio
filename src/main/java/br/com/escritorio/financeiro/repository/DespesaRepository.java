package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.Despesa;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    @EntityGraph(attributePaths = {"categoria", "formaPagamento"})
    Optional<Despesa> findComDetalhesById(Long id);

    /**
     * Despesas do período, inclusive as canceladas.
     *
     * @param padraoFornecedor padrão LIKE já em minúsculas e sem acentos
     */
    @Query("""
            select d from Despesa d
              join fetch d.categoria c
              join fetch d.formaPagamento f
            where d.dataPagamento between :inicio and :fim
              and (:categoriaId is null or c.id = :categoriaId)
              and lower(unaccent(d.fornecedor)) like :padraoFornecedor
            order by d.dataPagamento, d.id
            """)
    List<Despesa> pesquisar(@Param("inicio") LocalDate inicio,
                            @Param("fim") LocalDate fim,
                            @Param("categoriaId") Long categoriaId,
                            @Param("padraoFornecedor") String padraoFornecedor);

    /** Linhas da planilha do período: só as despesas ativas, em ordem de pagamento. */
    @Query("""
            select d from Despesa d
              join fetch d.categoria c
              join fetch d.formaPagamento f
            where d.dataPagamento between :inicio and :fim
              and d.situacao = br.com.escritorio.financeiro.domain.SituacaoDespesa.ATIVA
            order by d.dataPagamento, d.id
            """)
    List<Despesa> ativasEntre(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /** Datas de pagamento distintas das despesas ativas antes da data, para achar meses ainda não enviados. */
    @Query("""
            select distinct d.dataPagamento from Despesa d
            where d.situacao = br.com.escritorio.financeiro.domain.SituacaoDespesa.ATIVA
              and d.dataPagamento < :data
            """)
    List<LocalDate> datasAtivasAntesDe(@Param("data") LocalDate data);

    @Query("select distinct d.fornecedor from Despesa d order by d.fornecedor")
    List<String> fornecedores();
}
