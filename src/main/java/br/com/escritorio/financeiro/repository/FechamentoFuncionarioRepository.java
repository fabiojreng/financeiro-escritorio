package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.FechamentoFuncionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FechamentoFuncionarioRepository extends JpaRepository<FechamentoFuncionario, Long> {

    Optional<FechamentoFuncionario> findByFuncionarioIdAndCompetencia(Long funcionarioId, LocalDate competencia);

    @Query("""
            select f from FechamentoFuncionario f
            where f.competencia between :inicio and :fim
            """)
    List<FechamentoFuncionario> entreCompetencias(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("""
            select f.competencia from FechamentoFuncionario f
            where f.funcionario.id = :funcionarioId and f.pago = true
            """)
    List<LocalDate> competenciasPagas(@Param("funcionarioId") Long funcionarioId);

    @Query("""
            select f from FechamentoFuncionario f
              join fetch f.funcionario fu
            where f.pago = true
              and (:funcionarioId is null or fu.id = :funcionarioId)
              and f.dataPagamento between :inicio and :fim
            order by fu.nome, f.dataPagamento
            """)
    List<FechamentoFuncionario> pagosEntreDatas(@Param("funcionarioId") Long funcionarioId,
                                                @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("""
            select f from FechamentoFuncionario f
            where f.pago = true
            """)
    List<FechamentoFuncionario> pagos();
}
