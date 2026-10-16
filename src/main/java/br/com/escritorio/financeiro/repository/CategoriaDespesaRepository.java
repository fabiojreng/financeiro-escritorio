package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.CategoriaDespesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaDespesaRepository extends JpaRepository<CategoriaDespesa, Long> {

    List<CategoriaDespesa> findAllByOrderByAtivoDescNomeAsc();

    List<CategoriaDespesa> findByAtivoTrueOrIdOrderByNomeAsc(Long id);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
