package br.com.escritorio.financeiro.dto;

import br.com.escritorio.financeiro.domain.Despesa;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Totais das despesas ativas de uma lista; as canceladas não entram em nenhuma soma. */
public record ResumoDespesas(int quantidade, BigDecimal total, List<TotalCategoria> porCategoria) {

    public record TotalCategoria(String categoria, int quantidade, BigDecimal total) {
    }

    public static ResumoDespesas de(List<Despesa> despesas) {
        Map<String, TotalCategoria> categorias = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        int quantidade = 0;
        for (Despesa despesa : despesas) {
            if (!despesa.isAtiva()) {
                continue;
            }
            quantidade++;
            total = total.add(despesa.getValor());
            categorias.merge(despesa.getCategoria().getNome(),
                    new TotalCategoria(despesa.getCategoria().getNome(), 1, despesa.getValor()),
                    (a, b) -> new TotalCategoria(a.categoria(), a.quantidade() + 1, a.total().add(b.total())));
        }
        List<TotalCategoria> ordenadas = categorias.values().stream()
                .sorted(Comparator.comparing(TotalCategoria::total).reversed()
                        .thenComparing(TotalCategoria::categoria))
                .toList();
        return new ResumoDespesas(quantidade, total, ordenadas);
    }
}
