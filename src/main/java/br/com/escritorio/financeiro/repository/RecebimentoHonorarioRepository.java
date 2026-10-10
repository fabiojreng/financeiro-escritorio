package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
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

    @Query("""
            select coalesce(sum(r.valor), 0) from RecebimentoHonorario r
            where r.cancelado = false and r.dataRecebimento between :inicio and :fim
            """)
    BigDecimal somarRecebidoEntre(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /** Recebido até a data nos acordos com valor contratado definido e firmados até a mesma data. */
    @Query("""
            select coalesce(sum(r.valor), 0) from RecebimentoHonorario r
            where r.cancelado = false and r.dataRecebimento <= :data
              and r.acordo.valorContratado is not null and r.acordo.dataAcordo <= :data
            """)
    BigDecimal somarRecebidoEmAcordosComValorAte(@Param("data") LocalDate data);

    @Query("""
            select r from RecebimentoHonorario r
              join fetch r.acordo a
              join fetch a.atendimento atd
              join fetch atd.cliente c
              join fetch atd.tipoServico t
              join fetch atd.responsavel resp
              left join fetch r.parcela
            where r.dataRecebimento between :inicio and :fim
              and r.cancelado in :cancelados
              and (:responsavelId is null or resp.id = :responsavelId)
              and (:tipoServicoId is null or t.id = :tipoServicoId)
            order by r.dataRecebimento, lower(c.nome), r.id
            """)
    List<RecebimentoHonorario> relatorio(@Param("inicio") LocalDate inicio,
                                         @Param("fim") LocalDate fim,
                                         @Param("cancelados") Collection<Boolean> cancelados,
                                         @Param("responsavelId") Long responsavelId,
                                         @Param("tipoServicoId") Long tipoServicoId);

    interface TotalPorAcordo {
        Long getAcordoId();

        BigDecimal getTotal();
    }
}
