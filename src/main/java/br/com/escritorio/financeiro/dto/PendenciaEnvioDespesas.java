package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.SituacaoEnvioDespesas;

import java.time.LocalDate;
import java.time.YearMonth;

/** Mês cuja planilha de despesas precisa ser enviada à contabilidade, com o nome do mês já por extenso. */
public record PendenciaEnvioDespesas(YearMonth competencia, String nomeMes, LocalDate prazo,
                                     SituacaoEnvioDespesas situacao) {
}
