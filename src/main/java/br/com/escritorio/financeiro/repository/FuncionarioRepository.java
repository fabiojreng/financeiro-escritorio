package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    List<Funcionario> findAllByOrderByAtivoDescNomeAsc();

    List<Funcionario> findAllByOrderByNomeAsc();

    Optional<Funcionario> findByCpf(String cpf);
}
