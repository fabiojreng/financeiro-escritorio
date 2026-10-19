package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParcelaHonorarioRepository extends JpaRepository<ParcelaHonorario, Long> {

    @EntityGraph(attributePaths = {"negociacao", "negociacao.pagamento", "negociacao.pagamento.cliente"})
    Optional<ParcelaHonorario> findComDetalhesById(Long id);

    List<ParcelaHonorario> findByNegociacaoIdOrderByNumeroAsc(Long negociacaoId);

    @EntityGraph(attributePaths = "negociacao")
    List<ParcelaHonorario> findByNegociacaoIdInOrderByNumeroAsc(Collection<Long> negociacaoIds);
}
