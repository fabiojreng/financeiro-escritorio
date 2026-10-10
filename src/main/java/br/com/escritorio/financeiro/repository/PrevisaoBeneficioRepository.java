package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.PrevisaoBeneficio;
import br.com.escritorio.financeiro.domain.SituacaoPrevisao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrevisaoBeneficioRepository extends JpaRepository<PrevisaoBeneficio, Long> {

    @EntityGraph(attributePaths = {"atendimento", "atendimento.cliente", "atendimento.tipoServico",
            "atendimento.responsavel"})
    Optional<PrevisaoBeneficio> findComDetalhesById(Long id);

    List<PrevisaoBeneficio> findByAtendimentoIdOrderByDataPrevistaAscIdAsc(Long atendimentoId);

    /**
     * Previsões para a agenda, ordenadas pela data prevista. As datas são obrigatórias: o driver do
     * PostgreSQL não consegue tipar um parâmetro de data nulo em {@code :data is null}.
     *
     * @param padraoCliente padrão LIKE já em minúsculas e sem acentos
     */
    @Query("""
            select p from PrevisaoBeneficio p
              join fetch p.atendimento a
              join fetch a.cliente c
              join fetch a.tipoServico t
              join fetch a.responsavel r
            where p.dataPrevista between :inicio and :fim
              and p.situacao in :situacoes
              and lower(unaccent(c.nome)) like :padraoCliente
              and (:responsavelId is null or r.id = :responsavelId)
              and (:tipoServicoId is null or t.id = :tipoServicoId)
            order by p.dataPrevista, lower(c.nome), p.id
            """)
    List<PrevisaoBeneficio> pesquisar(@Param("inicio") LocalDate inicio,
                                      @Param("fim") LocalDate fim,
                                      @Param("situacoes") Collection<SituacaoPrevisao> situacoes,
                                      @Param("padraoCliente") String padraoCliente,
                                      @Param("responsavelId") Long responsavelId,
                                      @Param("tipoServicoId") Long tipoServicoId);

    long countBySituacaoInAndDataPrevistaBefore(Collection<SituacaoPrevisao> situacoes, LocalDate data);

    long countBySituacaoInAndDataPrevistaBetween(Collection<SituacaoPrevisao> situacoes, LocalDate inicio,
                                                 LocalDate fim);

    long countByDataPrevistaBetweenAndSituacaoNot(LocalDate inicio, LocalDate fim, SituacaoPrevisao situacao);

    @Query("""
            select coalesce(sum(p.valorEstimado), 0) from PrevisaoBeneficio p
            where p.dataPrevista between :inicio and :fim
              and p.situacao <> br.com.escritorio.financeiro.domain.SituacaoPrevisao.CANCELADO
            """)
    BigDecimal somarEstimadoNaoCancelado(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("""
            select coalesce(sum(p.valorConfirmado), 0) from PrevisaoBeneficio p
            where p.situacao = br.com.escritorio.financeiro.domain.SituacaoPrevisao.LIBERADO
              and p.dataEfetiva between :inicio and :fim
            """)
    BigDecimal somarConfirmadoPorDataEfetiva(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
