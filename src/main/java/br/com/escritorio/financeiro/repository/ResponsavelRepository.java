package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.Responsavel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponsavelRepository extends JpaRepository<Responsavel, Long> {

    List<Responsavel> findAllByOrderByAtivoDescNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
