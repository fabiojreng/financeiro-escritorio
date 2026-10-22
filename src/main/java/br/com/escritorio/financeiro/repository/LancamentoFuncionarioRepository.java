package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.LancamentoFuncionario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LancamentoFuncionarioRepository extends JpaRepository<LancamentoFuncionario, Long> {

    @EntityGraph(attributePaths = "funcionario")
    Optional<LancamentoFuncionario> findComDetalhesById(Long id);

    /** Lançamentos da competência do funcionário, inclusive os cancelados. */
    @Query("""
            select l from LancamentoFuncionario l
            where l.funcionario.id = :funcionarioId
              and l.competencia = :competencia
            order by l.data, l.id
            """)
    List<LancamentoFuncionario> daCompetencia(@Param("funcionarioId") Long funcionarioId,
                                              @Param("competencia") LocalDate competencia);

    /** Lançamentos ativos de todos os funcionários entre duas competências (primeiros dias dos meses). */
    @Query("""
            select l from LancamentoFuncionario l
              join fetch l.funcionario f
            where l.competencia between :inicio and :fim
              and l.situacao = br.com.escritorio.financeiro.domain.SituacaoLancamentoFuncionario.ATIVO
            order by l.competencia, f.nome, l.data, l.id
            """)
    List<LancamentoFuncionario> ativosEntreCompetencias(@Param("inicio") LocalDate inicio,
                                                        @Param("fim") LocalDate fim);

    /** Extrato: lançamentos ativos do funcionário pela data, em qualquer competência. */
    @Query("""
            select l from LancamentoFuncionario l
              join fetch l.funcionario f
            where (:funcionarioId is null or f.id = :funcionarioId)
              and l.data between :inicio and :fim
              and l.situacao = br.com.escritorio.financeiro.domain.SituacaoLancamentoFuncionario.ATIVO
            order by f.nome, l.data, l.id
            """)
    List<LancamentoFuncionario> ativosEntreDatas(@Param("funcionarioId") Long funcionarioId,
                                                 @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

}
