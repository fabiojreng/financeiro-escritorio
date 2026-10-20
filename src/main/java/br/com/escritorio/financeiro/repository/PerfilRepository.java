package br.com.escritorio.financeiro.repository;

import br.com.escritorio.financeiro.domain.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    List<Perfil> findAllByOrderByAtivoDescNomeAsc();

    List<Perfil> findByAtivoTrueOrIdOrderByNomeAsc(Long id);

    Optional<Perfil> findFirstBySistemaTrue();

    Optional<Perfil> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
