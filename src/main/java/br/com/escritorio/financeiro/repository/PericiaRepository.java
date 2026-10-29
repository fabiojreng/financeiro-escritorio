package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.Pericia;
import br.com.escritorio.financeiro.domain.SituacaoPericia;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PericiaRepository extends JpaRepository<Pericia, Long> {

    @EntityGraph(attributePaths = {"cliente", "tipoPericia", "responsavel", "local", "remarcadaPara"})
    Optional<Pericia> findComDetalhesById(Long id);

    /** Perícia anterior que foi remarcada para esta, se houver. */
    @EntityGraph(attributePaths = {"local"})
    Optional<Pericia> findByRemarcadaParaId(Long id);

    /**
     * Perícias com data no período, ordenadas por dia e horário (as sem horário ficam no fim do dia).
     *
     * @param padraoCliente    padrão LIKE já em minúsculas e sem acentos
     * @param padraoCpf        padrão LIKE só com dígitos, ou vazio para não filtrar por CPF
     * @param padraoObservacao padrão LIKE já em minúsculas e sem acentos, ou vazio para não filtrar
     */
    @Query("""
            select p from Pericia p
              join fetch p.cliente c
              join fetch p.tipoPericia t
              join fetch p.responsavel r
              left join fetch p.local l
            where p.data between :inicio and :fim
              and p.situacao in :situacoes
              and (lower(unaccent(c.nome)) like :padraoCliente
                   or (:padraoCpf <> '' and c.cpf like :padraoCpf))
              and (:padraoObservacao = '' or lower(unaccent(p.observacoes)) like :padraoObservacao)
              and (:responsavelId is null or r.id = :responsavelId)
              and (:tipoPericiaId is null or t.id = :tipoPericiaId)
              and (:localId is null or l.id = :localId)
            order by p.data, p.horario nulls last, lower(c.nome), p.id
            """)
    List<Pericia> pesquisar(@Param("inicio") LocalDate inicio,
                            @Param("fim") LocalDate fim,
                            @Param("situacoes") Collection<SituacaoPericia> situacoes,
                            @Param("padraoCliente") String padraoCliente,
                            @Param("padraoCpf") String padraoCpf,
                            @Param("padraoObservacao") String padraoObservacao,
                            @Param("responsavelId") Long responsavelId,
                            @Param("tipoPericiaId") Long tipoPericiaId,
                            @Param("localId") Long localId);

    /** Perícias agendadas que ainda aguardam o INSS marcar a data, das mais antigas para as mais novas. */
    @Query("""
            select p from Pericia p
              join fetch p.cliente c
              join fetch p.tipoPericia t
              join fetch p.responsavel r
              left join fetch p.local l
            where p.data is null
              and p.situacao = br.com.escritorio.financeiro.domain.SituacaoPericia.AGENDADA
              and (lower(unaccent(c.nome)) like :padraoCliente
                   or (:padraoCpf <> '' and c.cpf like :padraoCpf))
              and (:padraoObservacao = '' or lower(unaccent(p.observacoes)) like :padraoObservacao)
              and (:responsavelId is null or r.id = :responsavelId)
              and (:tipoPericiaId is null or t.id = :tipoPericiaId)
              and (:localId is null or l.id = :localId)
            order by p.criadoEm, p.id
            """)
    List<Pericia> pesquisarSemData(@Param("padraoCliente") String padraoCliente,
                                   @Param("padraoCpf") String padraoCpf,
                                   @Param("padraoObservacao") String padraoObservacao,
                                   @Param("responsavelId") Long responsavelId,
                                   @Param("tipoPericiaId") Long tipoPericiaId,
                                   @Param("localId") Long localId);

    @EntityGraph(attributePaths = {"tipoPericia", "responsavel", "local"})
    List<Pericia> findByClienteIdOrderByDataDescIdDesc(Long clienteId);
}
