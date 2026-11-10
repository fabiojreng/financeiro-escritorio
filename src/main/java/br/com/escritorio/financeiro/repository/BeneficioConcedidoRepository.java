package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.BeneficioConcedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BeneficioConcedidoRepository extends JpaRepository<BeneficioConcedido, Long> {

    @EntityGraph(attributePaths = {"cliente", "tipoServico", "responsavel"})
    Optional<BeneficioConcedido> findComDetalhesById(Long id);

    long countByCompetencia(LocalDate competencia);

    /**
     * Benefícios concedidos no mês, do cliente em ordem alfabética.
     *
     * @param padraoCliente padrão LIKE já em minúsculas e sem acentos
     * @param padraoCpf     padrão LIKE só com dígitos, ou vazio para não filtrar por CPF
     */
    @Query("""
            select b from BeneficioConcedido b
              join fetch b.cliente c
              join fetch b.tipoServico t
              join fetch b.responsavel r
            where b.competencia = :competencia
              and (lower(unaccent(c.nome)) like :padraoCliente
                   or (:padraoCpf <> '' and c.cpf like :padraoCpf))
              and (:tipoServicoId is null or t.id = :tipoServicoId)
              and (:responsavelId is null or r.id = :responsavelId)
            order by lower(c.nome), b.id
            """)
    List<BeneficioConcedido> pesquisar(@Param("competencia") LocalDate competencia,
                                       @Param("padraoCliente") String padraoCliente,
                                       @Param("padraoCpf") String padraoCpf,
                                       @Param("tipoServicoId") Long tipoServicoId,
                                       @Param("responsavelId") Long responsavelId);
}
