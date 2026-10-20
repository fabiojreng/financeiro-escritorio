package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.ResponsavelPagamento;
import br.com.escritorio.financeiro.domain.SituacaoRepassePagamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ResponsavelPagamentoRepository extends JpaRepository<ResponsavelPagamento, Long> {

    @EntityGraph(attributePaths = {"pagamento", "pagamento.cliente", "responsavel"})
    Optional<ResponsavelPagamento> findComDetalhesById(Long id);

    /**
     * Repasses da tela e do relatório, uma linha por responsável de cada pagamento. Com {@code somenteComValor}
     * falso, também vêm os responsáveis ainda sem repasse lançado; o filtro de situação só considera quem já tem valor.
     *
     * @param padraoCliente padrão LIKE já em minúsculas e sem acentos
     * @param padraoCpf     padrão LIKE só com dígitos, ou vazio para não filtrar por CPF
     */
    @Query("""
            select rp from ResponsavelPagamento rp
              join fetch rp.pagamento p
              join fetch p.cliente c
              join fetch p.tipoServico t
              join fetch rp.responsavel r
            where p.dataRecebimento between :inicio and :fim
              and (:somenteComValor = false or rp.valorRepasse is not null)
              and (:situacao is null or (rp.valorRepasse is not null and rp.situacaoRepasse = :situacao))
              and (lower(unaccent(c.nome)) like :padraoCliente
                   or (:padraoCpf <> '' and c.cpf like :padraoCpf))
              and (:responsavelId is null or r.id = :responsavelId)
              and (:tipoServicoId is null or t.id = :tipoServicoId)
            order by p.dataRecebimento, lower(c.nome), p.id, lower(r.nome), rp.id
            """)
    List<ResponsavelPagamento> pesquisarRepasses(@Param("inicio") LocalDate inicio,
                                                 @Param("fim") LocalDate fim,
                                                 @Param("somenteComValor") boolean somenteComValor,
                                                 @Param("situacao") SituacaoRepassePagamento situacao,
                                                 @Param("padraoCliente") String padraoCliente,
                                                 @Param("padraoCpf") String padraoCpf,
                                                 @Param("responsavelId") Long responsavelId,
                                                 @Param("tipoServicoId") Long tipoServicoId);
}
