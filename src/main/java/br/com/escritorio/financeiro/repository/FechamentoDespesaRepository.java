package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.FechamentoDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FechamentoDespesaRepository extends JpaRepository<FechamentoDespesa, Long> {

    /** @param competencia primeiro dia do mês */
    Optional<FechamentoDespesa> findByCompetencia(LocalDate competencia);

    @Query("select f.competencia from FechamentoDespesa f where f.enviado = true")
    List<LocalDate> competenciasEnviadas();
}
