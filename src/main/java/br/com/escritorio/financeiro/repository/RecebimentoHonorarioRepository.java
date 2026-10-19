package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.RecebimentoHonorario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecebimentoHonorarioRepository extends JpaRepository<RecebimentoHonorario, Long> {

    @EntityGraph(attributePaths = {"parcela", "parcela.negociacao", "parcela.negociacao.pagamento",
            "parcela.negociacao.pagamento.cliente"})
    Optional<RecebimentoHonorario> findComDetalhesById(Long id);

    List<RecebimentoHonorario> findByParcelaIdOrderByDataRecebimentoDescIdDesc(Long parcelaId);

    List<RecebimentoHonorario> findByParcelaIdInOrderByDataRecebimentoAscIdAsc(Collection<Long> parcelaIds);

    @Query("""
            select coalesce(sum(r.valor), 0) from RecebimentoHonorario r
            where r.parcela.id = :parcelaId and r.cancelado = false
            """)
    BigDecimal totalRecebidoNaParcela(@Param("parcelaId") Long parcelaId);

    @Query("""
            select coalesce(sum(r.valor), 0) from RecebimentoHonorario r
            where r.parcela.negociacao.pagamento.id = :pagamentoId and r.cancelado = false
            """)
    BigDecimal totalRecebidoDoPagamento(@Param("pagamentoId") Long pagamentoId);

    @Query("""
            select r.parcela.negociacao.pagamento.id as pagamentoId, sum(r.valor) as total
            from RecebimentoHonorario r
            where r.cancelado = false and r.parcela.negociacao.pagamento.id in :pagamentoIds
            group by r.parcela.negociacao.pagamento.id
            """)
    List<TotalPorPagamento> totaisRecebidosDosPagamentos(@Param("pagamentoIds") Collection<Long> pagamentoIds);

    @Query("""
            select r.parcela.id as parcelaId, sum(r.valor) as total
            from RecebimentoHonorario r
            where r.cancelado = false and r.parcela.id in :parcelaIds
            group by r.parcela.id
            """)
    List<TotalPorParcela> totaisRecebidosDasParcelas(@Param("parcelaIds") Collection<Long> parcelaIds);

    interface TotalPorPagamento {
        Long getPagamentoId();

        BigDecimal getTotal();
    }

    interface TotalPorParcela {
        Long getParcelaId();

        BigDecimal getTotal();
    }
}
