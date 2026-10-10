package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.AcordoHonorarios;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcordoHonorariosRepository extends JpaRepository<AcordoHonorarios, Long> {

    @EntityGraph(attributePaths = {"atendimento", "atendimento.cliente", "atendimento.tipoServico",
            "atendimento.responsavel"})
    Optional<AcordoHonorarios> findComDetalhesById(Long id);

    Optional<AcordoHonorarios> findByAtendimentoId(Long atendimentoId);

    boolean existsByAtendimentoId(Long atendimentoId);

    /**
     * @param padraoCliente padrão LIKE já em minúsculas e sem acentos
     */
    @Query("""
            select a from AcordoHonorarios a
              join fetch a.atendimento atd
              join fetch atd.cliente c
              join fetch atd.tipoServico t
              join fetch atd.responsavel r
            where lower(unaccent(c.nome)) like :padraoCliente
              and (:responsavelId is null or r.id = :responsavelId)
            order by lower(c.nome), a.id
            """)
    List<AcordoHonorarios> pesquisar(@Param("padraoCliente") String padraoCliente,
                                     @Param("responsavelId") Long responsavelId);
}
