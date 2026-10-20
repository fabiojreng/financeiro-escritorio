package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.PagamentoAgendado;
import br.com.escritorio.financeiro.domain.SituacaoHonorarioPagamento;
import br.com.escritorio.financeiro.domain.StatusPagamento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PagamentoAgendadoRepository extends JpaRepository<PagamentoAgendado, Long> {

    @EntityGraph(attributePaths = {"cliente", "responsaveis", "responsaveis.responsavel", "tipoServico"})
    Optional<PagamentoAgendado> findComDetalhesById(Long id);

    /**
     * Pagamentos da agenda, ordenados pelo dia de recebimento. O filtro de responsável traz o pagamento
     * quando qualquer um dos responsáveis dele corresponde.
     *
     * @param padraoCliente padrão LIKE já em minúsculas e sem acentos
     * @param padraoCpf     padrão LIKE só com dígitos, ou vazio para não filtrar por CPF
     */
    @Query("""
            select p from PagamentoAgendado p
              join fetch p.cliente c
              join fetch p.tipoServico t
              left join fetch p.responsaveis rp
              left join fetch rp.responsavel
            where p.dataRecebimento between :inicio and :fim
              and p.status in :status
              and (lower(unaccent(c.nome)) like :padraoCliente
                   or (:padraoCpf <> '' and c.cpf like :padraoCpf))
              and (:responsavelId is null or exists (
                   select 1 from ResponsavelPagamento x where x.pagamento = p and x.responsavel.id = :responsavelId))
              and (:tipoServicoId is null or t.id = :tipoServicoId)
            order by p.dataRecebimento, lower(c.nome), p.id
            """)
    List<PagamentoAgendado> pesquisar(@Param("inicio") LocalDate inicio,
                                      @Param("fim") LocalDate fim,
                                      @Param("status") Collection<StatusPagamento> status,
                                      @Param("padraoCliente") String padraoCliente,
                                      @Param("padraoCpf") String padraoCpf,
                                      @Param("responsavelId") Long responsavelId,
                                      @Param("tipoServicoId") Long tipoServicoId);

    /**
     * Pagamentos para a tela e o relatório de honorários. Com {@code somenteComValor} falso, também vêm os
     * pagamentos ainda sem honorário lançado; o filtro de situação só considera quem já tem valor.
     */
    @Query("""
            select p from PagamentoAgendado p
              join fetch p.cliente c
              join fetch p.tipoServico t
              left join fetch p.responsaveis rp
              left join fetch rp.responsavel
            where p.dataRecebimento between :inicio and :fim
              and (:somenteComValor = false or p.valorHonorario is not null)
              and (:situacao is null or (p.valorHonorario is not null and p.situacaoHonorario = :situacao))
              and (lower(unaccent(c.nome)) like :padraoCliente
                   or (:padraoCpf <> '' and c.cpf like :padraoCpf))
              and (:responsavelId is null or exists (
                   select 1 from ResponsavelPagamento x where x.pagamento = p and x.responsavel.id = :responsavelId))
              and (:tipoServicoId is null or t.id = :tipoServicoId)
            order by p.dataRecebimento, lower(c.nome), p.id
            """)
    List<PagamentoAgendado> pesquisarHonorarios(@Param("inicio") LocalDate inicio,
                                                @Param("fim") LocalDate fim,
                                                @Param("somenteComValor") boolean somenteComValor,
                                                @Param("situacao") SituacaoHonorarioPagamento situacao,
                                                @Param("padraoCliente") String padraoCliente,
                                                @Param("padraoCpf") String padraoCpf,
                                                @Param("responsavelId") Long responsavelId,
                                                @Param("tipoServicoId") Long tipoServicoId);

    @EntityGraph(attributePaths = {"cliente", "responsaveis", "responsaveis.responsavel", "tipoServico"})
    List<PagamentoAgendado> findByClienteIdOrderByDataRecebimentoDescIdDesc(Long clienteId);
}
