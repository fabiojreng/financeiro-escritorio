package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.LocalPericia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocalPericiaRepository extends JpaRepository<LocalPericia, Long> {

    List<LocalPericia> findAllByOrderByAtivoDescNomeAsc();

    List<LocalPericia> findByAtivoTrueOrIdOrderByNomeAsc(Long id);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
