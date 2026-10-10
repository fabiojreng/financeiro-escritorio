package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.Repasse;
import br.com.escritorio.financeiro.domain.SituacaoRepasse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RepasseRepository extends JpaRepository<Repasse, Long> {

    @EntityGraph(attributePaths = {"atendimento", "atendimento.cliente", "atendimento.tipoServico",
            "atendimento.responsavel", "destinatario"})
    Optional<Repasse> findComDetalhesById(Long id);

    @EntityGraph(attributePaths = {"destinatario"})
    List<Repasse> findByAtendimentoIdOrderByDataPrevistaAscIdAsc(Long atendimentoId);

    /**
     * @param padraoCliente padrão LIKE já em minúsculas e sem acentos
     */
    @Query("""
            select r from Repasse r
              join fetch r.atendimento a
              join fetch a.cliente c
              join fetch a.tipoServico t
              join fetch r.destinatario d
            where lower(unaccent(c.nome)) like :padraoCliente
              and (:destinatarioId is null or d.id = :destinatarioId)
              and (:situacao is null or r.situacao = :situacao)
            order by r.dataPrevista, lower(c.nome), r.id
            """)
    List<Repasse> pesquisar(@Param("padraoCliente") String padraoCliente,
                            @Param("destinatarioId") Long destinatarioId,
                            @Param("situacao") SituacaoRepasse situacao);

    @Query("""
            select r from Repasse r
              join fetch r.atendimento a
              join fetch a.cliente c
              join fetch a.tipoServico t
              join fetch r.destinatario d
            where r.dataPrevista between :inicio and :fim
              and (:destinatarioId is null or d.id = :destinatarioId)
              and (:tipoServicoId is null or t.id = :tipoServicoId)
              and (:situacao is null or r.situacao = :situacao)
            order by r.dataPrevista, lower(c.nome), r.id
            """)
    List<Repasse> relatorio(@Param("inicio") LocalDate inicio,
                            @Param("fim") LocalDate fim,
                            @Param("destinatarioId") Long destinatarioId,
                            @Param("tipoServicoId") Long tipoServicoId,
                            @Param("situacao") SituacaoRepasse situacao);

    @Query("""
            select coalesce(sum(r.valorPrevisto), 0) from Repasse r
            where r.situacao = br.com.escritorio.financeiro.domain.SituacaoRepasse.PENDENTE
              and r.dataPrevista <= :data
            """)
    BigDecimal somarPendentesAte(@Param("data") LocalDate data);

    long countBySituacaoAndDataPrevistaLessThanEqual(SituacaoRepasse situacao, LocalDate data);

    @Query("""
            select coalesce(sum(r.valorPago), 0) from Repasse r
            where r.situacao = br.com.escritorio.financeiro.domain.SituacaoRepasse.PAGO
              and r.dataPagamento between :inicio and :fim
            """)
    BigDecimal somarPagosEntre(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
