package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("select distinct a.entidade from Auditoria a order by a.entidade")
    List<String> listarEntidades();

    Page<Auditoria> findAllByOrderByDataHoraDescIdDesc(Pageable pageable);

    Page<Auditoria> findByEntidadeOrderByDataHoraDescIdDesc(String entidade, Pageable pageable);

    List<Auditoria> findByEntidadeAndEntidadeIdOrderByDataHoraDescIdDesc(String entidade, Long entidadeId);

    List<Auditoria> findByEntidadeAndEntidadeIdIn(String entidade, Collection<Long> entidadeIds);
}
