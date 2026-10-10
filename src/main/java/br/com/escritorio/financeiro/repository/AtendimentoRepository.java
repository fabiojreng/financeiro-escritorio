package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.Atendimento;
import br.com.escritorio.financeiro.domain.SituacaoAtendimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    @EntityGraph(attributePaths = {"cliente", "tipoServico", "responsavel"})
    Optional<Atendimento> findComDetalhesById(Long id);

    @EntityGraph(attributePaths = {"tipoServico", "responsavel"})
    List<Atendimento> findByClienteIdOrderByDataAberturaDescIdDesc(Long clienteId);

    /**
     * @param padraoCliente padrão LIKE já em minúsculas e sem acentos
     */
    @Query(value = """
            select a from Atendimento a
              join fetch a.cliente c
              join fetch a.tipoServico t
              join fetch a.responsavel r
            where lower(unaccent(c.nome)) like :padraoCliente
              and (:responsavelId is null or r.id = :responsavelId)
              and (:tipoServicoId is null or t.id = :tipoServicoId)
              and (:situacao is null or a.situacao = :situacao)
            order by a.dataAbertura desc, a.id desc
            """,
            countQuery = """
            select count(a) from Atendimento a
              join a.cliente c
              join a.tipoServico t
              join a.responsavel r
            where lower(unaccent(c.nome)) like :padraoCliente
              and (:responsavelId is null or r.id = :responsavelId)
              and (:tipoServicoId is null or t.id = :tipoServicoId)
              and (:situacao is null or a.situacao = :situacao)
            """)
    Page<Atendimento> pesquisar(@Param("padraoCliente") String padraoCliente,
                                @Param("responsavelId") Long responsavelId,
                                @Param("tipoServicoId") Long tipoServicoId,
                                @Param("situacao") SituacaoAtendimento situacao,
                                Pageable pageable);
}
