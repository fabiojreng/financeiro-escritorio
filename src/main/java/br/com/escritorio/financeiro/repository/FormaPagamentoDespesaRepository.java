package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.FormaPagamentoDespesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormaPagamentoDespesaRepository extends JpaRepository<FormaPagamentoDespesa, Long> {

    List<FormaPagamentoDespesa> findAllByOrderByAtivoDescNomeAsc();

    List<FormaPagamentoDespesa> findByAtivoTrueOrIdOrderByNomeAsc(Long id);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
