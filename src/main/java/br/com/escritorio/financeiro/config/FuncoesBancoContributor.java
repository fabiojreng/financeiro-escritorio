package br.com.escritorio.financeiro.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;

/**
 * Disponibiliza a função {@code unaccent} do PostgreSQL (criada na migração V5) nas consultas JPQL.
 * Registrado em META-INF/services/org.hibernate.boot.model.FunctionContributor.
 */
public class FuncoesBancoContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        functionContributions.getFunctionRegistry().registerPattern(
                "unaccent",
                "unaccent(?1)",
                functionContributions.getTypeConfiguration().getBasicTypeRegistry()
                        .resolve(StandardBasicTypes.STRING));
    }
}
