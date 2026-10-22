package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.EmprestimoFuncionario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmprestimoFuncionarioRepository extends JpaRepository<EmprestimoFuncionario, Long> {

    @EntityGraph(attributePaths = "funcionario")
    Optional<EmprestimoFuncionario> findComFuncionarioById(Long id);

    List<EmprestimoFuncionario> findByFuncionarioIdOrderByDataConcessaoDescIdDesc(Long funcionarioId);

    @Query("""
            select e from EmprestimoFuncionario e
              join fetch e.funcionario f
            where e.situacao <> br.com.escritorio.financeiro.domain.SituacaoEmprestimo.CANCELADO
            order by f.nome, e.dataConcessao, e.id
            """)
    List<EmprestimoFuncionario> naoCancelados();
}
