package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RecebimentoHonorarioRepository extends JpaRepository<RecebimentoHonorario, Long> {

    @EntityGraph(attributePaths = {"acordo", "parcela", "acordo.atendimento", "acordo.atendimento.cliente"})
    Optional<RecebimentoHonorario> findComDetalhesById(Long id);

    @EntityGraph(attributePaths = "parcela")
    List<RecebimentoHonorario> findByAcordoIdOrderByDataRecebimentoDescIdDesc(Long acordoId);

    @Query("""
            select coalesce(sum(r.valor), 0) from RecebimentoHonorario r
            where r.acordo.id = :acordoId and r.cancelado = false
            """)
    BigDecimal totalRecebidoNoAcordo(@Param("acordoId") Long acordoId);

    @Query("""
            select coalesce(sum(r.valor), 0) from RecebimentoHonorario r
            where r.parcela.id = :parcelaId and r.cancelado = false
            """)
    BigDecimal totalRecebidoNaParcela(@Param("parcelaId") Long parcelaId);

    @Query("""
            select r.acordo.id as acordoId, sum(r.valor) as total from RecebimentoHonorario r
            where r.cancelado = false
            group by r.acordo.id
            """)
    List<TotalPorAcordo> totaisRecebidosPorAcordo();

    interface TotalPorAcordo {
        Long getAcordoId();

        BigDecimal getTotal();
    }
}
