package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.NegociacaoHonorario;
import br.com.escritorio.financeiro.domain.SituacaoNegociacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NegociacaoHonorarioRepository extends JpaRepository<NegociacaoHonorario, Long> {

    @EntityGraph(attributePaths = {"pagamento", "pagamento.cliente", "pagamento.responsaveis", "pagamento.responsaveis.responsavel", "pagamento.tipoServico"})
    Optional<NegociacaoHonorario> findComDetalhesById(Long id);

    Optional<NegociacaoHonorario> findByPagamentoIdAndSituacao(Long pagamentoId, SituacaoNegociacao situacao);

    @EntityGraph(attributePaths = "pagamento")
    List<NegociacaoHonorario> findByPagamentoIdInAndSituacao(Collection<Long> pagamentoIds, SituacaoNegociacao situacao);

    List<NegociacaoHonorario> findByPagamentoIdOrderByDataNegociacaoDescIdDesc(Long pagamentoId);
}
