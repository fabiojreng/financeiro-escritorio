package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.TipoPericia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoPericiaRepository extends JpaRepository<TipoPericia, Long> {

    List<TipoPericia> findAllByOrderByAtivoDescNomeAsc();

    List<TipoPericia> findByAtivoTrueOrIdOrderByNomeAsc(Long id);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
