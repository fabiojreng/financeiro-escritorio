package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.SituacaoNegociacao;
import br.com.escritorio.financeiro.domain.SituacaoParcela;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParcelaHonorarioRepository extends JpaRepository<ParcelaHonorario, Long> {

    @EntityGraph(attributePaths = {"negociacao", "negociacao.pagamento", "negociacao.pagamento.cliente"})
    Optional<ParcelaHonorario> findComDetalhesById(Long id);

    List<ParcelaHonorario> findByNegociacaoIdOrderByNumeroAsc(Long negociacaoId);

    @EntityGraph(attributePaths = "negociacao")
    List<ParcelaHonorario> findByNegociacaoIdInOrderByNumeroAsc(Collection<Long> negociacaoIds);

    @Query("""
            select p from ParcelaHonorario p
              join fetch p.negociacao n
              join fetch n.pagamento pag
              join fetch pag.cliente c
            where n.situacao = :situacaoNegociacao
              and p.situacao = :situacaoParcela
              and p.vencimento <= :limite
            order by p.vencimento, p.id
            """)
    List<ParcelaHonorario> findCobrancasAte(@Param("situacaoNegociacao") SituacaoNegociacao situacaoNegociacao,
                                            @Param("situacaoParcela") SituacaoParcela situacaoParcela,
                                            @Param("limite") LocalDate limite);
}
