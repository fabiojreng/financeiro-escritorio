package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.ParcelaHonorario;
import br.com.escritorio.financeiro.domain.SituacaoParcela;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParcelaHonorarioRepository extends JpaRepository<ParcelaHonorario, Long> {

    @EntityGraph(attributePaths = {"acordo", "acordo.atendimento", "acordo.atendimento.cliente",
            "acordo.atendimento.tipoServico", "acordo.atendimento.responsavel"})
    Optional<ParcelaHonorario> findComDetalhesById(Long id);

    List<ParcelaHonorario> findByAcordoIdOrderByNumeroAsc(Long acordoId);

    boolean existsByAcordoIdAndSituacaoNot(Long acordoId, SituacaoParcela situacao);

    @Query("select coalesce(max(p.numero), 0) from ParcelaHonorario p where p.acordo.id = :acordoId")
    int maiorNumero(@Param("acordoId") Long acordoId);
}
