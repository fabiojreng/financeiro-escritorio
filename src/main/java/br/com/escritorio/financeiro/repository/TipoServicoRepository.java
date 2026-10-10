package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.TipoServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoServicoRepository extends JpaRepository<TipoServico, Long> {

    List<TipoServico> findAllByOrderByAtivoDescNomeAsc();

    List<TipoServico> findByAtivoTrueOrIdOrderByNomeAsc(Long id);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
